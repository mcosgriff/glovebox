package io.cosgriff.glovebox.ui.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.ReminderGroup
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.Snoozing
import io.cosgriff.glovebox.domain.UrgentItem
import io.cosgriff.glovebox.domain.label
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.until
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class RemindersViewModel @Inject constructor(overview: GarageOverview, private val schedule: ScheduleDao) : ViewModel() {
    /** Null while loading. */
    val garage: StateFlow<Garage?> = overview.garage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null ends the snooze. */
    fun setSnooze(scheduleItemId: Long, snooze: Snooze?) {
        viewModelScope.launch { schedule.setSnooze(scheduleItemId, snooze?.untilOdometer, snooze?.untilDate) }
    }
}

private val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

/** What's due across every vehicle. Design: 10 · Reminders tab. */
@Composable
fun RemindersScreen(
    onLogIt: (vehicleId: Long, scheduleItemId: Long) -> Unit,
    onAddVehicle: () -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    vm: RemindersViewModel = hiltViewModel(),
) {
    val garage = vm.garage.collectAsStateWithLifecycle().value ?: return // first frame while loading
    // 0 = all vehicles. A vehicle that has since gone falls back to all.
    var filterId by rememberSaveable { mutableLongStateOf(0L) }
    val shown = garage.vehicles.filter { it.vehicle.id == filterId }.ifEmpty { garage.vehicles }
    val groups = ReminderPresentation.grouped(shown.map { it.vehicle to it.comingUp }, LocalDate.now())

    // The reminder whose snooze sheet is open; 0 = none. Looked up again each time so it stays current.
    var snoozingId by rememberSaveable { mutableLongStateOf(0L) }
    val snoozing = groups.values.flatten().firstOrNull { it.status.item.id == snoozingId && it.status.lastDone != null }
    if (snoozing != null) {
        val item = snoozing.status.item
        val owner = garage.vehicles.first { it.vehicle.id == snoozing.vehicle.id }
        val apply = { snooze: Snooze, verb: String ->
            val before = item.snooze
            snoozingId = 0
            vm.setSnooze(item.id, snooze)
            onMessage("${item.name} $verb until ${snooze.until()}", "Undo") { vm.setSnooze(item.id, before) }
        }
        SnoozeSheet(
            reminder = snoozing,
            subtitle = listOfNotNull("${snoozing.vehicle.year} ${snoozing.vehicle.shortName()}", snoozing.due()).joinToString(" · "),
            options = Snoozing.options(snoozing.status, owner.odometer, LocalDate.now(), owner.oil),
            skip = Snoozing.skip(snoozing.status, owner.odometer, LocalDate.now()),
            onSnooze = { apply(it, "snoozed") },
            onSkip = { apply(it, "skipped") },
            onAlreadyDone = {
                snoozingId = 0
                onLogIt(snoozing.vehicle.id, item.id)
            },
            onDismiss = { snoozingId = 0 },
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "Reminders",
                Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (garage.vehicles.size > 1) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleChip("All vehicles", selected = shown.size > 1) { filterId = 0 }
                    garage.vehicles.forEach { g ->
                        VehicleChip(g.vehicle.shortName(), selected = shown.size == 1 && shown.single() == g) { filterId = g.vehicle.id }
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item { Empty(noVehicles = garage.vehicles.isEmpty(), onAddVehicle) }
        }

        groups.forEach { (group, reminders) ->
            item(key = group.name) { GroupHeader(group) }
            if (group == ReminderGroup.NOT_LOGGED) {
                item(key = "not-logged-note") {
                    Text(
                        "Log each of these once and its countdown starts from that date and mileage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(reminders, key = { "${group.name}-${it.status.item.id}" }) { r ->
                ReminderCard(
                    group = group,
                    r = r,
                    onLogIt = { onLogIt(r.vehicle.id, r.status.item.id) },
                    onSnooze = { snoozingId = r.status.item.id },
                    onEndSnooze = {
                        val item = r.status.item
                        val before = item.snooze
                        vm.setSnooze(item.id, null)
                        onMessage("${item.name} is no longer snoozed", "Undo") { vm.setSnooze(item.id, before) }
                    },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

internal fun VehicleEntity.shortName(): String = nickname ?: model

@Composable
private fun groupColor(group: ReminderGroup): Color = when (group) {
    ReminderGroup.OVERDUE -> statusColors().overdue
    ReminderGroup.NEXT_30_DAYS -> statusColors().soon
    ReminderGroup.LATER -> statusColors().ok
    ReminderGroup.NOT_LOGGED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun GroupHeader(group: ReminderGroup) {
    Text(
        when (group) {
            ReminderGroup.OVERDUE -> "OVERDUE"
            ReminderGroup.NEXT_30_DAYS -> "NEXT 30 DAYS"
            ReminderGroup.LATER -> "LATER"
            ReminderGroup.NOT_LOGGED -> "NOT LOGGED YET"
        },
        Modifier.padding(top = 6.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = groupColor(group),
    )
}

@Composable
private fun ReminderCard(
    group: ReminderGroup,
    r: UrgentItem<VehicleEntity>,
    onLogIt: () -> Unit,
    onSnooze: () -> Unit,
    onEndSnooze: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, if (group == ReminderGroup.OVERDUE) scheme.primary else scheme.outlineVariant),
    ) {
        val title = @Composable { modifier: Modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(r.status.item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${r.vehicle.year} ${r.vehicle.shortName()} · ${r.status.item.interval.label().every()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        val buttonShape = RoundedCornerShape(10.dp)
        if (group == ReminderGroup.NOT_LOGGED) {
            // One line and a quieter button: a new vehicle has a long list of these.
            Row(
                Modifier.padding(start = 14.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                title(Modifier.weight(1f))
                FilledTonalButton(onClick = onLogIt, modifier = Modifier.heightIn(min = 48.dp), shape = buttonShape) { Text("Log it") }
            }
        } else {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    title(Modifier.weight(1f))
                    r.due()?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = groupColor(group))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onLogIt, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = buttonShape) {
                        Text("Log it")
                    }
                    // Snoozing only does something to a reminder that is due; one that is snoozed can be woken.
                    val second = when {
                        r.status.snooze != null -> "End snooze" to onEndSnooze
                        r.status.status.state != DueState.OK -> "Snooze…" to onSnooze
                        else -> null
                    }
                    second?.let { (label, action) ->
                        Button(
                            onClick = action,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.surfaceVariant, contentColor = scheme.onSurface),
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

/** "every 5,000 mi" for a single limit; "10,000 mi / 12 mo" already reads as a pair. */
private fun String.every(): String = if (" / " in this) this else "every $this"

/** "3,312 mi over", "in 640 mi", "in 12 days", "Mar 2027" */
internal fun UrgentItem<VehicleEntity>.due(): String? = when (val h = progress.headline) {
    DueHeadline.NotLogged -> null
    is DueHeadline.Miles -> when {
        h.remaining < 0 -> "${abs(h.remaining).asMiles()} over"
        h.remaining == 0 -> "due now"
        else -> "in ${h.remaining.asMiles()}"
    }
    is DueHeadline.Days -> when {
        h.remaining < 0 -> "${days(abs(h.remaining))} over"
        h.remaining == 0L -> "due today"
        h.remaining <= 60 -> "in ${days(h.remaining)}"
        else -> status.status.dueByDate?.format(monthYear) ?: "in ${days(h.remaining)}"
    }
    is DueHeadline.Snoozed -> "Snoozed until ${h.snooze.until()}"
}

private fun days(n: Long): String = if (n == 1L) "1 day" else "$n days"

@Composable
private fun VehicleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = scheme.surface,
            labelColor = scheme.onSurface,
            selectedContainerColor = scheme.onSurface,
            selectedLabelColor = scheme.surface,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun Empty(noVehicles: Boolean, onAddVehicle: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 96.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Nothing to remind you about yet", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            if (noVehicles) {
                "Add a vehicle and pick what to track. What's due will show up here."
            } else {
                "No reminders are being tracked. Add items to a vehicle's maintenance schedule and they'll show up here."
            },
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (noVehicles) {
            Button(onClick = onAddVehicle, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Add a vehicle") }
        }
    }
}

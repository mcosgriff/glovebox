package io.cosgriff.glovebox.ui.reminders

import io.cosgriff.glovebox.domain.Interval
import androidx.compose.ui.platform.LocalResources
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.monthYear
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.domain.actual
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import io.cosgriff.glovebox.notify.Notifier
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.until
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class RemindersViewModel @Inject constructor(overview: GarageOverview, private val schedule: ScheduleDao, val notifier: Notifier) : ViewModel() {
    /** Null while loading. */
    val garage: StateFlow<Garage?> = overview.garage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null ends the snooze. */
    fun setSnooze(scheduleItemId: Long, snooze: Snooze?) {
        viewModelScope.launch { schedule.setSnooze(scheduleItemId, snooze?.untilOdometer, snooze?.untilDate) }
    }
}


/** What's due across every vehicle. Design: 10 · Reminders tab. */
@Composable
fun RemindersScreen(
    onLogIt: (vehicleId: Long, scheduleItemId: Long) -> Unit,
    onAddVehicle: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenNotifications: () -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    vm: RemindersViewModel = hiltViewModel(),
) {
    val garage = vm.garage.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val today = LocalDate.now()
    val res = LocalResources.current
    // The reminder whose snooze sheet is open; 0 = none. Looked up again each time so it stays current.
    // Item ids are unique across vehicles, so the vehicle filter doesn't matter here.
    var snoozingId by rememberSaveable { mutableLongStateOf(0L) }
    val snoozing = ReminderPresentation.grouped(garage.vehicles.map { it.vehicle to it.comingUp }, today).values.flatten()
        .firstOrNull { it.status.item.id == snoozingId && it.status.lastDone != null }
    if (snoozing != null) {
        val item = snoozing.status.item
        val owner = garage.vehicles.first { it.vehicle.id == snoozing.vehicle.id }
        val apply = { snooze: Snooze, skipped: Boolean ->
            val before = item.snooze
            snoozingId = 0
            vm.setSnooze(item.id, snooze)
            onMessage(res.getString(if (skipped) R.string.reminders_skipped_until else R.string.reminders_snoozed_until, item.name, snooze.until(snoozing.vehicle.unit)), res.getString(R.string.undo)) { vm.setSnooze(item.id, before) }
        }
        SnoozeSheet(
            reminder = snoozing,
            subtitle = listOfNotNull("${snoozing.vehicle.year} ${snoozing.vehicle.shortName()}", snoozing.due()).joinToString(" · "),
            options = Snoozing.options(snoozing.status, owner.odometer, today, owner.oil, snoozing.vehicle.unit),
            skip = Snoozing.skip(snoozing.status, owner.odometer, today),
            onSnooze = { apply(it, false) },
            onSkip = { apply(it, true) },
            onAlreadyDone = {
                snoozingId = 0
                onLogIt(snoozing.vehicle.id, item.id)
            },
            onDismiss = { snoozingId = 0 },
        )
    }

    RemindersContent(
        garage, today, notificationsAllowed = vm.notifier.allowed,
        onLogIt = onLogIt, onAddVehicle = onAddVehicle, onOpenRules = onOpenRules, onOpenNotifications = onOpenNotifications,
        onSnooze = { snoozingId = it.status.item.id },
        onEndSnooze = { r ->
            val item = r.status.item
            val before = item.snooze
            vm.setSnooze(item.id, null)
            onMessage(res.getString(R.string.reminders_unsnoozed, item.name), res.getString(R.string.undo)) { vm.setSnooze(item.id, before) }
        },
    )
}

/** The Reminders tab as given, with no ViewModel: what [RemindersScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun RemindersContent(
    garage: Garage,
    today: LocalDate,
    notificationsAllowed: Boolean,
    onLogIt: (vehicleId: Long, scheduleItemId: Long) -> Unit,
    onAddVehicle: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSnooze: (UrgentItem<VehicleEntity>) -> Unit,
    onEndSnooze: (UrgentItem<VehicleEntity>) -> Unit,
) {
    // 0 = all vehicles. A vehicle that has since gone falls back to all.
    var filterId by rememberSaveable { mutableLongStateOf(0L) }
    val shown = garage.vehicles.filter { it.vehicle.id == filterId }.ifEmpty { garage.vehicles }
    val groups = ReminderPresentation.grouped(shown.map { it.vehicle to it.comingUp }, today)
    val rulesLabel = stringResource(R.string.reminder_rules_title)

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reminders_title),
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(
                    onClick = onOpenRules,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = rulesLabel },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text(stringResource(R.string.reminders_rules))
                }
            }
        }

        if (garage.vehicles.size > 1) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleChip(stringResource(R.string.home_all_vehicles), selected = shown.size > 1) { filterId = 0 }
                    garage.vehicles.forEach { g ->
                        VehicleChip(g.vehicle.shortName(), selected = shown.size == 1 && shown.single() == g) { filterId = g.vehicle.id }
                    }
                }
            }
        }

        // The moment reminders exist is the moment being told about them is worth something.
        if (groups.isNotEmpty() && !notificationsAllowed) {
            item {
                Surface(
                    onClick = onOpenNotifications,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.reminders_notifications_off), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.reminders_turn_on),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        stringResource(R.string.item_log_once),
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
                    onSnooze = { onSnooze(r) },
                    onEndSnooze = { onEndSnooze(r) },
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
            ReminderGroup.OVERDUE -> stringResource(R.string.reminders_group_overdue)
            ReminderGroup.NEXT_30_DAYS -> stringResource(R.string.reminders_group_next_30)
            ReminderGroup.LATER -> stringResource(R.string.reminders_group_later)
            ReminderGroup.NOT_LOGGED -> stringResource(R.string.reminders_group_not_logged)
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
                // As on vehicle home: amber, not the overdue red, for a job the schedule has passed with nothing logged.
                r.status.lastScheduled?.let { point ->
                    val unit = r.vehicle.unit
                    val at = r.vehicle.clusterSwap().actual(point.at).asDistance(unit)
                    Text(
                        if (point.ago == 0) stringResource(R.string.scheduled_now, at) else stringResource(R.string.last_scheduled, at, point.ago.asDistance(unit)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColors().soon,
                    )
                }
                Text(
                    "${r.vehicle.year} ${r.vehicle.shortName()} · ${r.status.item.interval.everyShort(r.vehicle.unit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        val buttonShape = RoundedCornerShape(10.dp)
        // Every card has the same buttons, so TalkBack is told whose they are (#234): "Log it: Brake fluid, 2019 Blue truck".
        val job = r.status.item.name
        val vehicle = "${r.vehicle.year} ${r.vehicle.shortName()}"
        val logItFor = stringResource(R.string.reminders_log_it_for, job, vehicle)
        // "Log it" in ink, as on vehicle home: the accent on a row says it is late, so it isn't on every row's button.
        val logIt = @Composable { modifier: Modifier ->
            OutlinedButton(
                onClick = onLogIt,
                modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = logItFor },
                shape = buttonShape,
                border = BorderStroke(1.5.dp, scheme.onSurface),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text(stringResource(R.string.log_it))
            }
        }
        if (group == ReminderGroup.NOT_LOGGED) {
            // One line and a quieter button: a new vehicle has a long list of these.
            Row(
                Modifier.padding(start = 14.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                title(Modifier.weight(1f))
                logIt(Modifier)
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
                    logIt(Modifier.weight(1f))
                    // Snoozing only does something to a reminder that is due; one that is snoozed can be woken.
                    val second = when {
                        r.status.snooze != null -> Triple(stringResource(R.string.reminders_end_snooze), stringResource(R.string.reminders_end_snooze_for, job, vehicle), onEndSnooze)
                        r.status.status.state != DueState.OK -> Triple(stringResource(R.string.reminders_snooze), stringResource(R.string.reminders_snooze_for, job, vehicle), onSnooze)
                        else -> null
                    }
                    second?.let { (label, described, action) ->
                        Button(
                            onClick = action,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = described },
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
@Composable
private fun Interval.everyShort(unit: DistanceUnit): String =
    if (miles != null && months != null) text(unit) else stringResource(R.string.reminders_every, text(unit))

/** "3,312 mi over", "in 640 mi", "in 12 days", "Mar 2027" */
@Composable
internal fun UrgentItem<VehicleEntity>.due(): String? = when (val h = progress.headline) {
    DueHeadline.NotLogged -> null
    is DueHeadline.Miles -> when {
        h.remaining < 0 -> stringResource(R.string.reminders_over, abs(h.remaining).asDistance(vehicle.unit))
        h.remaining == 0 -> stringResource(R.string.reminders_due_now)
        else -> stringResource(R.string.reminders_in, h.remaining.asDistance(vehicle.unit))
    }
    is DueHeadline.Days -> when {
        h.remaining < 0 -> stringResource(R.string.reminders_over, days(abs(h.remaining)))
        h.remaining == 0L -> stringResource(R.string.reminders_due_today)
        h.remaining <= 60 -> stringResource(R.string.reminders_in, days(h.remaining))
        else -> status.status.dueByDate?.format(monthYear) ?: stringResource(R.string.reminders_in, days(h.remaining))
    }
    is DueHeadline.Snoozed -> stringResource(R.string.due_snoozed_until, h.snooze.until(vehicle.unit))
}

// Days however many, unlike a headline's, which turns long ones into months.
@Composable
private fun days(n: Long): String = pluralStringResource(R.plurals.days, n.toInt(), n.toInt().asNumber())

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
        Text(stringResource(R.string.reminders_empty_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            if (noVehicles) {
                stringResource(R.string.reminders_empty_no_vehicle)
            } else {
                stringResource(R.string.reminders_empty_none_tracked)
            },
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (noVehicles) {
            Button(onClick = onAddVehicle, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.garage_empty_add)) }
        }
    }
}

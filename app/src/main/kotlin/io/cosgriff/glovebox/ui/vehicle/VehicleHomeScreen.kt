package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DecidedBy
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.ReminderProgress
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.label
import io.cosgriff.glovebox.ui.VehicleHomeRoute
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

data class VehicleHome(
    val vehicle: VehicleEntity,
    val odometer: Int?,
    val defaultRule: DueRule,
    val comingUp: ComingUp,
)

@HiltViewModel
class VehicleHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<VehicleHomeRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. Recomputed whenever the log changes. */
    val home: StateFlow<VehicleHome?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeTracked(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
        settings.settings,
    ) { vehicle, items, records, readings, s ->
        if (vehicle == null) return@combine null
        val entries = records.map { it.toEntry() }
        val updates = readings.map { OdometerReading(it.date, it.odometer) }
        val tracked = items.mapNotNull { it.toTracked(s.defaultRule) }
        VehicleHome(
            vehicle = vehicle,
            odometer = VehicleReminders.currentOdometer(entries, updates),
            defaultRule = s.defaultRule,
            comingUp = ReminderPresentation.comingUp(VehicleReminders.statuses(tracked, entries, updates, LocalDate.now())),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun VehicleHomeScreen(
    onBack: () -> Unit,
    onLogService: (vehicleId: Long) -> Unit,
    vm: VehicleHomeViewModel = hiltViewModel(),
) {
    val home = vm.home.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val v = home.vehicle
    val up = home.comingUp

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                modifier = Modifier.padding(start = 8.dp, top = 12.dp).heightIn(min = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null)
                Text("All vehicles", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall)
            }

            Column(
                // Bottom padding leaves room for the Log service button.
                Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderCard(v, home.odometer, up)

                if (up.counting.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Coming up", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            home.defaultRule.label(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WhiteCard {
                        up.counting.forEachIndexed { i, (status, progress) ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ReminderRow(status, progress)
                        }
                    }
                }

                if (up.notLogged.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Not logged yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Log each of these once and its countdown starts from that date and mileage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WhiteCard {
                        up.notLogged.forEachIndexed { i, status ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(status.item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Every ${status.item.interval.label().replace(" / ", " or ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (up.counting.isEmpty() && up.notLogged.isEmpty()) {
                    Text(
                        "No reminders are being tracked for this vehicle yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onLogService(v.id) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Log service") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

@Composable
private fun HeaderCard(v: VehicleEntity, odometer: Int?, up: ComingUp) {
    WhiteCard(padding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${v.year} ${v.nickname ?: v.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        // vPIC drive types read like "4WD/4-Wheel Drive/4x4"; the first part is enough here.
                        listOfNotNull(v.trim, v.drivetrain?.substringBefore('/'), v.engine).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(up)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Odometer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    odometer?.asMiles() ?: "Not recorded",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/** Counts only reminders that have been logged; a new vehicle shows no pill rather than "9 overdue". */
@Composable
private fun StatusPill(up: ComingUp) {
    val scheme = MaterialTheme.colorScheme
    val (text, container, content) = when {
        up.overdue > 0 -> Triple("${up.overdue} OVERDUE", scheme.primaryContainer, scheme.onPrimaryContainer)
        up.dueSoon > 0 -> Triple("${up.dueSoon} DUE SOON", scheme.surfaceVariant, scheme.onSurface)
        up.counting.isNotEmpty() -> Triple("UP TO DATE", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        else -> return
    }
    Surface(color = container, shape = CircleShape) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

@Composable
private fun ReminderRow(status: ItemStatus, progress: ReminderProgress) {
    val colors = statusColors()
    val color = when (status.status.state) {
        DueState.OVERDUE -> colors.overdue
        DueState.DUE_SOON -> colors.soon
        DueState.OK -> colors.ok
    }
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(status.item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(progress.headline.text(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
        }
        ProgressBar(progress.fraction ?: 0.0, color)
        Text(status.detail(progress), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Decorative: the headline text beside it already says the same thing. */
@Composable
private fun ProgressBar(fraction: Double, color: Color) {
    Box(
        Modifier.fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
            .clearAndSetSemantics { },
    ) {
        Box(Modifier.fillMaxWidth(fraction.toFloat()).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Composable
private fun WhiteCard(padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp), content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

private fun DueHeadline.text(): String = when (this) {
    DueHeadline.NotLogged -> "Not logged yet"
    is DueHeadline.Miles -> when {
        remaining < 0 -> "Overdue ${abs(remaining).asMiles()}"
        remaining == 0 -> "Due now"
        else -> "In ${remaining.asMiles()}"
    }
    is DueHeadline.Days -> when {
        remaining < 0 -> "Overdue ${days(abs(remaining))}"
        remaining == 0L -> "Due today"
        else -> "In ${days(remaining)}"
    }
}

/** Days up to two months, then whole months — "In 151 days" is harder to picture than "In 5 months". */
private fun days(n: Long): String = when {
    n == 1L -> "1 day"
    n <= 60 -> "$n days"
    else -> "${n / 30} months"
}

/** "Every 5,000 mi or 6 mo · last at 60,100 mi · est. Nov 5, 2026" */
private fun ItemStatus.detail(progress: ReminderProgress): String {
    val last = lastDone ?: return ""
    val every = "Every ${item.interval.label().replace(" / ", " or ")}"
    val lastText = if (progress.headline is DueHeadline.Days) "last on ${last.date.format(longDate)}" else "last at ${last.odometer.asMiles()}"
    // Only worth an estimate when mileage decides it and the date comes from the driving pace.
    val estimate = status.estimatedDueDate
        ?.takeIf { progress.headline is DueHeadline.Miles && status.decidedBy == DecidedBy.MILEAGE && status.state != DueState.OVERDUE }
        ?.let { "est. ${it.format(longDate)}" }
    return listOfNotNull(every, lastText, estimate).joinToString(" · ")
}

private fun DueRule.label(): String = when (this) {
    DueRule.WHICHEVER_FIRST -> "Whichever comes first"
    DueRule.WHICHEVER_LAST -> "Whichever comes last"
    DueRule.MILEAGE_ONLY -> "Mileage only"
    DueRule.TIME_ONLY -> "Time only"
}

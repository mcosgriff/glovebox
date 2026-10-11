package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DecidedBy
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.ReminderProgress
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.label
import io.cosgriff.glovebox.ui.VehicleHomeRoute
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class VehicleHome(
    val vehicle: VehicleEntity,
    val odometer: Int?,
    val defaultRule: DueRule,
    val comingUp: ComingUp,
    /** Kept so the mileage sheet can show what a new reading would change. */
    val inputs: ReminderInputs,
)

@HiltViewModel
class VehicleHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    private val odometer: OdometerDao,
    settings: SettingsRepository,
    recalls: RecallDao,
    recallRepository: RecallRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<VehicleHomeRoute>()
    private val vehicleId = route.vehicleId

    /** Came from the mileage check-in notification: the sheet opens without a tap. */
    val startWithMileage: Boolean = route.updateMileage

    init {
        // Quietly re-checks a recall list that is a week old or was never fetched.
        viewModelScope.launch { recallRepository.refreshIfStale(vehicleId) }
    }

    val openRecalls: StateFlow<OpenRecalls> = recalls.observe(vehicleId)
        .map { list -> Recalls.open(list.map { it.toStored() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OpenRecalls(0, null))

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
            comingUp = ReminderPresentation.comingUp(VehicleReminders.statuses(tracked, entries, updates, LocalDate.now(), s.lead)),
            inputs = ReminderInputs(tracked, entries, updates, s.lead),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Records today's odometer; [onSaved] gets the new row's id so the caller can offer Undo. */
    fun saveMileage(miles: Int, onSaved: (readingId: Long) -> Unit) {
        viewModelScope.launch {
            onSaved(odometer.insert(OdometerReadingEntity(vehicleId = vehicleId, date = LocalDate.now(), odometer = miles)))
        }
    }

    fun undoMileage(readingId: Long) {
        viewModelScope.launch { odometer.delete(readingId) }
    }
}

@Composable
fun VehicleHomeScreen(
    onBack: () -> Unit,
    onLogService: (vehicleId: Long) -> Unit,
    onOpenRules: () -> Unit,
    onOpenRecalls: (vehicleId: Long) -> Unit,
    onOpenDetails: (vehicleId: Long) -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    vm: VehicleHomeViewModel = hiltViewModel(),
) {
    val home = vm.home.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val v = home.vehicle
    val up = home.comingUp
    val openRecalls by vm.openRecalls.collectAsStateWithLifecycle()
    var updatingMileage by rememberSaveable { mutableStateOf(vm.startWithMileage) }

    if (updatingMileage) {
        UpdateMileageSheet(
            vehicleName = "${v.year} ${v.nickname ?: v.model}",
            preview = { miles -> MileageUpdate.preview(home.inputs, miles, LocalDate.now()) },
            onSave = { miles ->
                updatingMileage = false
                vm.saveMileage(miles) { id -> onMessage("Mileage updated · ${miles.asMiles()}", "Undo") { vm.undoMileage(id) } }
            },
            onDismiss = { updatingMileage = false },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null)
                    Text("All vehicles", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.weight(1f))
                OutlinedIconButton(
                    onClick = { onOpenDetails(v.id) },
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Vehicle details")
                }
            }

            Column(
                // Bottom padding leaves room for the Log service button.
                Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderCard(v, home.odometer, up, onUpdateMileage = { updatingMileage = true })

                if (openRecalls.count > 0) RecallBanner(openRecalls, onClick = { onOpenRecalls(v.id) })

                if (up.counting.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Coming up", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onOpenRules, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(home.defaultRule.ruleLabel(), style = MaterialTheme.typography.bodySmall)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
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
private fun HeaderCard(v: VehicleEntity, odometer: Int?, up: ComingUp, onUpdateMileage: () -> Unit) {
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
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Odometer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        odometer?.asMiles() ?: "Not recorded",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                OutlinedButton(
                    onClick = onUpdateMileage,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text("Update mileage")
                }
            }
        }
    }
}

/** Design: the tinted strip under the header on 4 · Garage / vehicle home. */
@Composable
private fun RecallBanner(open: OpenRecalls, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = scheme.primaryContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, scheme.primary),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(22.dp), tint = statusColors().overdue)
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(if (open.count == 1) "1 open recall" else "${open.count} open recalls", open.only).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                // Not "at a $makeName dealer": NHTSA also lists recalled aftermarket parts for the model.
                Text("Recall repairs are free of charge", style = MaterialTheme.typography.bodySmall, color = statusColors().soon)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = statusColors().overdue)
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


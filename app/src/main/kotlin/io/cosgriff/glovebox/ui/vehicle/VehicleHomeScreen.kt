package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.ui.screenState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.Pill
import io.cosgriff.glovebox.ui.components.FilledCard
import androidx.compose.ui.platform.LocalResources
import io.cosgriff.glovebox.ui.service.asNumber
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.every
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.ui.graphics.luminance
import androidx.activity.compose.ReportDrawn
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import io.cosgriff.glovebox.data.db.ShopRecommendationDao
import io.cosgriff.glovebox.data.db.toModel
import io.cosgriff.glovebox.domain.FindingColour
import io.cosgriff.glovebox.domain.OpenRecommendations
import io.cosgriff.glovebox.domain.ShopRecommendations
import io.cosgriff.glovebox.ui.components.FindingTag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.actual
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.ReminderProgress
import io.cosgriff.glovebox.ui.VehicleHomeRoute
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.data.log.clusterSwap
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    logs: VehicleLogs,
    private val odometer: OdometerDao,
    recalls: RecallDao,
    recallRepository: RecallRepository,
    recommendations: ShopRecommendationDao,
) : ViewModel() {
    private val route = savedStateHandle.route<VehicleHomeRoute>()
    private val vehicleId = route.vehicleId

    /** Came from the mileage check-in notification: the sheet opens without a tap. */
    val startWithMileage: Boolean = route.updateMileage

    init {
        // Quietly re-checks a recall list that is a week old or was never fetched.
        viewModelScope.launch { recallRepository.refreshIfStale(vehicleId) }
    }

    val openRecalls: StateFlow<OpenRecalls> = recalls.observe(vehicleId)
        .map { list -> Recalls.open(list.map { it.toStored() }) }
        .screenState(viewModelScope, OpenRecalls(0, null))

    /** The shop's recommendations kept for later and still open (#572), for their row. */
    val openRecommendations: StateFlow<OpenRecommendations> = recommendations.observe(vehicleId)
        .map { rows -> ShopRecommendations.summary(rows.map { it.toModel() }) }
        .screenState(viewModelScope, OpenRecommendations())

    /** Null while loading, and if the vehicle no longer exists. Recomputed whenever the log changes. */
    val home: StateFlow<VehicleHome?> = logs.observe(vehicleId).map { log ->
        log ?: return@map null
        VehicleHome(
            vehicle = log.vehicle,
            odometer = log.odometer,
            defaultRule = log.defaultRule,
            comingUp = ReminderPresentation.comingUp(log.statuses(LocalDate.now())),
            inputs = log.inputs,
        )
    }.screenState(viewModelScope, null)

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
    /** Log service with one reminder's work already ticked. */
    onLogItem: (vehicleId: Long, scheduleItemId: Long) -> Unit,
    onOpenRules: () -> Unit,
    onOpenRecalls: (vehicleId: Long) -> Unit,
    onOpenDetails: (vehicleId: Long) -> Unit,
    /** The "When were these last done?" list. */
    onEnterLastDone: (vehicleId: Long) -> Unit,
    /** Opens the camera to read the odometer; [last] is the latest known reading. */
    onReadDash: (last: Int?, unit: DistanceUnit) -> Unit,
    /** What the camera read, until [onDashReadingUsed] says the sheet has taken it. */
    dashReading: Int?,
    onDashReadingUsed: () -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    /** The shop's recommendations kept for later (#572). */
    onOpenRecommendations: (vehicleId: Long) -> Unit = {},
    vm: VehicleHomeViewModel = hiltViewModel(),
) {
    val home = vm.home.collectAsStateWithLifecycle().value ?: return // first frame while loading
    // Tells Android the start is done once the reminders are drawn, which is what the startup benchmark
    // measures (#222). Only the first time counts; later visits change nothing.
    ReportDrawn()
    val v = home.vehicle
    val unit = v.unit
    val openRecalls by vm.openRecalls.collectAsStateWithLifecycle()
    val openRecommendations by vm.openRecommendations.collectAsStateWithLifecycle()
    var updatingMileage by rememberSaveable { mutableStateOf(vm.startWithMileage) }
    val res = LocalResources.current

    if (updatingMileage) {
        UpdateMileageSheet(
            vehicleName = v.shortName(),
            unit = unit,
            preview = { miles -> MileageUpdate.preview(home.inputs, miles, LocalDate.now(), unit) },
            onSave = { miles ->
                updatingMileage = false
                vm.saveMileage(miles) { id -> onMessage(res.getString(R.string.home_mileage_updated, miles.asDistance(unit)), res.getString(R.string.undo)) { vm.undoMileage(id) } }
            },
            onDismiss = { updatingMileage = false },
            onReadDash = { onReadDash(home.odometer, unit) },
            dashReading = dashReading,
            onDashReadingUsed = onDashReadingUsed,
        )
    }

    VehicleHomeContent(
        home, openRecalls, onBack, onLogService, onLogItem, onOpenRules, onOpenRecalls, onOpenDetails, onEnterLastDone,
        onUpdateMileage = { updatingMileage = true },
        recommendations = openRecommendations,
        onOpenRecommendations = onOpenRecommendations,
    )
}

/** Vehicle home as given, with no ViewModel: what [VehicleHomeScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun VehicleHomeContent(
    home: VehicleHome,
    openRecalls: OpenRecalls,
    onBack: () -> Unit,
    onLogService: (vehicleId: Long) -> Unit,
    onLogItem: (vehicleId: Long, scheduleItemId: Long) -> Unit,
    onOpenRules: () -> Unit,
    onOpenRecalls: (vehicleId: Long) -> Unit,
    onOpenDetails: (vehicleId: Long) -> Unit,
    onEnterLastDone: (vehicleId: Long) -> Unit,
    onUpdateMileage: () -> Unit,
    recommendations: OpenRecommendations = OpenRecommendations(),
    onOpenRecommendations: (vehicleId: Long) -> Unit = {},
) {
    val v = home.vehicle
    val unit = v.unit
    val up = home.comingUp
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null)
                    Text(stringResource(R.string.home_all_vehicles), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.weight(1f))
                OutlinedIconButton(
                    onClick = { onOpenDetails(v.id) },
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.home_vehicle_details))
                }
            }

            Column(
                // Bottom padding leaves room for the Log service button.
                Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderCard(v, home.odometer, up, onUpdateMileage = onUpdateMileage)

                if (openRecalls.count > 0) RecallBanner(openRecalls, onClick = { onOpenRecalls(v.id) })

                // Only while something is open; apart from the schedule, it never makes anything overdue (#572).
                if (recommendations.count > 0) RecommendationsRow(recommendations, onClick = { onOpenRecommendations(v.id) })

                // Three groups, each row one tap from Log service with that job ticked. Not logged yet comes
                // first: these have nothing to count from, so they are what needs doing first.
                val tapHint = stringResource(R.string.home_tap_hint)
                if (up.notLogged.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.due_not_logged), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.home_not_logged_intro, tapHint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        up.notLogged.forEachIndexed { i, status ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.log_it)) { onLogItem(v.id, status.item.id) }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(status.item.name, style = MaterialTheme.typography.titleSmall)
                                    // Amber, not red: the schedule's last point has passed with nothing logged,
                                    // which isn't the same as the work being overdue.
                                    status.lastScheduled?.let { point ->
                                        val at = v.clusterSwap().actual(point.at).asDistance(unit)
                                        Text(
                                            if (point.ago == 0) stringResource(R.string.scheduled_now, at) else stringResource(R.string.last_scheduled, at, point.ago.asDistance(unit)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColors().soon,
                                        )
                                    }
                                    Text(
                                        status.item.every(unit),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // The whole row opens Log service with this job ticked; this says so.
                                LogItLabel()
                            }
                        }
                    }
                    InkOutlinedButton(onClick = { onEnterLastDone(v.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), quiet = true) {
                        Text(stringResource(R.string.home_enter_last_done))
                    }
                }

                // The rule link sits by the first of these two headings that is shown.
                val groups = listOf(stringResource(R.string.overdue) to up.overdueNow, stringResource(R.string.home_coming_up) to up.upcoming).filter { it.second.isNotEmpty() }
                groups.forEachIndexed { g, (title, rows) ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (g == 0) {
                                TextButton(onClick = onOpenRules, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(home.defaultRule.ruleLabel(), style = MaterialTheme.typography.bodySmall)
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (g == 0 && up.notLogged.isEmpty()) {
                            Text(tapHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FilledCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        rows.forEachIndexed { i, (status, progress) ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ReminderRow(status, progress, v.clusterSwap(), unit, onClick = { onLogItem(v.id, status.item.id) })
                        }
                    }
                }

                if (up.counting.isEmpty() && up.notLogged.isEmpty()) {
                    Text(
                        stringResource(R.string.home_none_tracked),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // The content-slot form, not `text =` and `icon =`: that one drew the label but left it out of the
        // accessibility tree, so TalkBack said only "Button" (material3 1.4.0).
        ExtendedFloatingActionButton(
            onClick = { onLogService(v.id) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.log_service))
        }
    }
}

@Composable
private fun HeaderCard(v: VehicleEntity, odometer: Int?, up: ComingUp, onUpdateMileage: () -> Unit) {
    FilledCard(padding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(v.shortName(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.odometer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        odometer?.asDistance(v.unit) ?: stringResource(R.string.home_not_recorded),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = PlexMono,
                    )
                    // The dash figure is the one to compare with; the vehicle's real mileage goes under it.
                    val swap = v.clusterSwap()
                    if (swap != null && odometer != null) {
                        Text(
                            stringResource(R.string.home_actual, swap.actual(odometer).asDistance(v.unit)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                InkOutlinedButton(onClick = onUpdateMileage, modifier = Modifier.heightIn(min = 48.dp), quiet = true) {
                    Text(stringResource(R.string.update_mileage_title))
                }
            }
        }
    }
}

/** Board RecommendedHome (#572): the shop's open recommendations, counted, with how many are red and yellow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationsRow(open: OpenRecommendations, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, tint = scheme.onSurface)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(pluralStringResource(R.plurals.home_recommendations, open.count, open.count.asNumber()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (open.red > 0 || open.amber > 0) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (open.red > 0) FindingTag(FindingColour.RED, pluralStringResource(R.plurals.home_recommendations_red, open.red, open.red.asNumber()))
                        if (open.amber > 0) FindingTag(FindingColour.AMBER, pluralStringResource(R.plurals.home_recommendations_amber, open.amber, open.amber.asNumber()))
                    }
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** Design: the tinted strip under the header on 4 · Garage / vehicle home. */
@Composable
private fun RecallBanner(open: OpenRecalls, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // Red when a recall says not to drive or to park outside (#461), the most urgent things a recall can say.
    val urgent = open.doNotDrive || open.parkOutside
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (urgent) scheme.errorContainer else scheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.5.dp, if (urgent) scheme.error else scheme.primary),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(22.dp), tint = if (urgent) scheme.error else statusColors().overdue)
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(pluralStringResource(R.plurals.open_recalls, open.count, open.count.asNumber()), open.only).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                if (open.doNotDrive) Text(stringResource(R.string.recalls_do_not_drive), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                if (open.parkOutside) Text(stringResource(R.string.recalls_park_outside), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                // Not "at a $makeName dealer": NHTSA also lists recalled aftermarket parts for the model.
                Text(stringResource(R.string.home_recalls_free), style = MaterialTheme.typography.bodySmall, color = statusColors().soon)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = if (urgent) scheme.error else statusColors().overdue)
        }
    }
}

/**
 * Counts only reminders that have been logged; a new vehicle shows no pill rather than "9 overdue". Which
 * state it shows is the garage chip's ([ComingUp.attention]), so the two can't disagree; the words are its own.
 */
@Composable
private fun StatusPill(up: ComingUp) {
    val scheme = MaterialTheme.colorScheme
    val attention = up.attention ?: return
    val (text, container, content) = when {
        // Canvas: a soft tint with dark orange text in light; in dark the tint would vanish into the card, so it is solid.
        attention.state == DueState.OVERDUE && scheme.background.luminance() < 0.5f ->
            Triple(pluralStringResource(R.plurals.home_pill_overdue, attention.count, attention.count.asNumber()), scheme.onPrimaryContainer, scheme.onPrimary)
        attention.state == DueState.OVERDUE ->
            Triple(pluralStringResource(R.plurals.home_pill_overdue, attention.count, attention.count.asNumber()), scheme.primaryContainer, scheme.onPrimaryContainer)
        attention.state == DueState.DUE_SOON ->
            Triple(pluralStringResource(R.plurals.home_pill_due_soon, attention.count, attention.count.asNumber()), scheme.surfaceVariant, scheme.onSurface)
        // Nothing to be up to date on while there is work the log doesn't have.
        attention.toLog > 0 ->
            Triple(pluralStringResource(R.plurals.home_pill_to_log, attention.toLog, attention.toLog.asNumber()), scheme.surfaceVariant, statusColors().soon)
        else -> Triple(stringResource(R.string.home_pill_up_to_date), scheme.tertiaryContainer, scheme.onTertiaryContainer)
    }
    Pill(text, container, content)
}

@Composable
private fun ReminderRow(status: ItemStatus, progress: ReminderProgress, swap: ClusterSwap?, unit: DistanceUnit, onClick: () -> Unit) {
    val colors = statusColors()
    val color = when (status.status.state) {
        DueState.OVERDUE -> colors.overdue
        DueState.DUE_SOON -> colors.soon
        DueState.OK -> colors.ok
    }
    Column(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = stringResource(R.string.log_it), onClick = onClick).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(status.item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(progress.headline.text(unit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
        }
        ProgressBar(progress.fraction ?: 0.0, color)
        // "Log it" as on Not logged yet, on the detail line so the name and countdown keep the top line.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                status.detail(progress, swap, unit),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LogItLabel()
        }
    }
}

/**
 * "Log it" drawn as an outlined button on a reminder row. It has no click of its own: the whole row
 * is the button (with "Log it" as its click label), so a tap anywhere on the row does the same.
 * In ink, not the accent: the accent on a reminder row says it is late, and a button on every row
 * in that colour would say it of every row.
 */
@Composable
private fun LogItLabel() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
        color = Color.Transparent,
    ) {
        Text(
            stringResource(R.string.log_it),
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

/**
 * "Every 5,000 mi or 6 mo · last at 60,100 mi · est. Nov 5, 2026". A job counting down to its first
 * time has no last, so it gets the interval alone (and an estimate, when there is one).
 */
@Composable
private fun ItemStatus.detail(progress: ReminderProgress, swap: ClusterSwap?, unit: DistanceUnit): String {
    val every = item.every(unit)
    // As it was read at the time, which is what the record says.
    val last = lastDone?.let { swap.asReadOn(it) }
    val lastText = when {
        last == null -> null
        progress.headline is DueHeadline.Days -> stringResource(R.string.home_last_on, last.date.format(longDate))
        else -> stringResource(R.string.home_last_at, last.odometer.asDistance(unit))
    }
    // Beside a headline in miles only: one in days would read against a date worked out from the pace.
    val estimate = status.paceEstimate
        ?.takeIf { progress.headline is DueHeadline.Miles }
        ?.let { stringResource(R.string.home_estimate, it.format(longDate)) }
    return listOfNotNull(every, lastText, estimate).joinToString(" · ")
}


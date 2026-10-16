package io.cosgriff.glovebox.ui

import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavBackStackEntry
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContentScope
import io.cosgriff.glovebox.domain.LogEdits
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collectLatest
import io.cosgriff.glovebox.ui.about.LicenceTextScreen
import io.cosgriff.glovebox.ui.about.LicencesScreen
import io.cosgriff.glovebox.ui.about.PrivacyScreen
import io.cosgriff.glovebox.ui.about.AboutScreen
import io.cosgriff.glovebox.ui.settings.DeleteAllScreen
import io.cosgriff.glovebox.ui.settings.RestoreViewModel
import io.cosgriff.glovebox.ui.settings.RestoreSheet
import io.cosgriff.glovebox.domain.BackupRules
import io.cosgriff.glovebox.ui.settings.BackupScreen
import io.cosgriff.glovebox.ui.settings.ImportScreen
import io.cosgriff.glovebox.ui.settings.NotificationsScreen
import io.cosgriff.glovebox.notify.DestinationViewModel
import io.cosgriff.glovebox.notify.Destination
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.cosgriff.glovebox.ui.garage.AddVehicleScreen
import io.cosgriff.glovebox.ui.export.ExportResultsViewModel
import io.cosgriff.glovebox.ui.garage.GarageScreen
import io.cosgriff.glovebox.ui.garage.ManualAddScreen
import io.cosgriff.glovebox.ui.history.HistoryScreen
import io.cosgriff.glovebox.ui.history.RecordDetailScreen
import io.cosgriff.glovebox.ui.photo.PhotoViewerScreen
import io.cosgriff.glovebox.ui.recalls.RecallsScreen
import io.cosgriff.glovebox.ui.reminders.ReminderRulesScreen
import io.cosgriff.glovebox.ui.reminders.RemindersScreen
import androidx.navigation.toRoute
import io.cosgriff.glovebox.ui.scan.ReadDashScreen
import io.cosgriff.glovebox.ui.vehicle.MileageReadingsScreen
import io.cosgriff.glovebox.ui.scan.ScanVinScreen
import io.cosgriff.glovebox.ui.schedule.ScheduleScreen
import io.cosgriff.glovebox.ui.schedule.LastDoneScreen
import io.cosgriff.glovebox.ui.schedule.MilestonesScreen
import io.cosgriff.glovebox.ui.schedule.ScheduleSetupScreen
import io.cosgriff.glovebox.ui.vehicle.ArchivedVehiclesScreen
import io.cosgriff.glovebox.ui.vehicle.ClearHistoryScreen
import io.cosgriff.glovebox.ui.vehicle.DeleteVehicleScreen
import io.cosgriff.glovebox.ui.vehicle.FluidsPartsScreen
import io.cosgriff.glovebox.ui.vehicle.DocumentsScreen
import io.cosgriff.glovebox.ui.service.LogServiceScreen
import io.cosgriff.glovebox.ui.service.RecordEvent
import io.cosgriff.glovebox.ui.service.RecordEventsViewModel
import io.cosgriff.glovebox.ui.vehicle.VehicleDetailsScreen
import io.cosgriff.glovebox.ui.vehicle.VehicleHomeScreen
import io.cosgriff.glovebox.ui.settings.SettingsScreen
import io.cosgriff.glovebox.ui.torque.TorqueSpecsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

// Type-safe routes. Matches the navigation map on the design canvas.
@Serializable object GarageRoute
@Serializable object HistoryRoute
@Serializable object RemindersRoute
@Serializable object SettingsRoute
@Serializable object AddVehicleRoute
@Serializable object ManualAddRoute
@Serializable object ScanVinRoute
@Serializable data class MileageReadingsRoute(val vehicleId: Long)

/** [last] is the vehicle's latest known reading in dash terms, to tell the odometer from the other numbers in view. */
@Serializable data class ReadDashRoute(val last: Int? = null, val unit: String = "MILES")
@Serializable object ReminderRulesRoute
@Serializable object NotificationsRoute
@Serializable object BackupRoute
/** Adds data from a JSON file the user picks to one vehicle. */
/** [vehicleId] starts the page with that vehicle chosen, as Vehicle details opens it. */
@Serializable data class ImportRoute(val vehicleId: Long? = null)
@Serializable object DeleteAllRoute
@Serializable object AboutRoute
@Serializable object PrivacyRoute
@Serializable object LicencesRoute
@Serializable data class LicenceTextRoute(val id: String)
@Serializable data class ScheduleSetupRoute(val vehicleId: Long)
/** A vehicle's schedule once it has been set up. */
@Serializable data class ScheduleRoute(val vehicleId: Long)
/** "When were these last done?": a date and mileage for reminders that have never been logged. */
@Serializable data class LastDoneRoute(val vehicleId: Long)
/** The schedule laid out by mileage, with what the log says was done at each point. */
@Serializable data class MilestonesRoute(val vehicleId: Long)
/** [updateMileage] opens the Update mileage sheet straight away, for the mileage check-in notification. */
@Serializable data class VehicleHomeRoute(val vehicleId: Long, val updateMileage: Boolean = false)
/**
 * [recordId] 0 logs a new record; otherwise that record is edited. [scheduleItemId] ticks that
 * reminder's work to start with (new records only).
 */
@Serializable data class LogServiceRoute(val vehicleId: Long, val recordId: Long = 0, val scheduleItemId: Long = 0)
@Serializable data class RecordDetailRoute(val recordId: Long)
/** One of a record's photos, full screen; [start] is its place in the grid. */
@Serializable data class PhotoViewerRoute(val recordId: Long, val start: Int)
@Serializable data class RecallsRoute(val vehicleId: Long)
@Serializable data class VehicleDetailsRoute(val vehicleId: Long)
@Serializable object ArchivedVehiclesRoute
@Serializable data class DeleteVehicleRoute(val vehicleId: Long)
/** Deletes a vehicle's service records (imported, or all) and nothing else. */
@Serializable data class ClearHistoryRoute(val vehicleId: Long)
@Serializable data class TorqueSpecsRoute(val vehicleId: Long)
@Serializable data class FluidsPartsRoute(val vehicleId: Long)
@Serializable data class DocumentsRoute(val vehicleId: Long)
/** One archived vehicle's history, read-only. */
@Serializable data class ArchivedHistoryRoute(val vehicleId: Long)

/**
 * A destination with the page colour behind it. Screens draw no background of their own, and without
 * one the screen underneath shows through while a back swipe is held or a transition runs.
 */
private inline fun <reified T : Any> NavGraphBuilder.screen(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T> { entry ->
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content(entry) }
}

private const val SCANNED_VIN = "scannedVin"
private const val DASH_READING = "dashReading"
private const val CONFIRM_DELETE = "confirmDelete"

private data class Tab(val route: Any, val type: KClass<*>, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(GarageRoute, GarageRoute::class, "Garage", Icons.Outlined.Home),
    Tab(HistoryRoute, HistoryRoute::class, "History", Icons.Outlined.History),
    Tab(RemindersRoute, RemindersRoute::class, "Reminders", Icons.Outlined.Notifications),
    Tab(SettingsRoute, SettingsRoute::class, "Settings", Icons.Outlined.Settings),
)

@Composable
fun GloveboxNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    // Vehicle home sits under the Garage tab and keeps the bar, as on the design.
    val onVehicleHome = destination?.hasRoute(VehicleHomeRoute::class) == true
    val showBar = onVehicleHome || tabs.any { t -> destination?.hasRoute(t.type) == true }

    // Service log changes are announced here so Undo works whichever screen the user lands on.
    val recordEvents: RecordEventsViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // For plain confirmations with nothing to undo.
    val showNote: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    LaunchedEffect(Unit) {
        // Latest only: a newer change replaces the bar for an older one, whose Undo is then gone.
        recordEvents.events.collectLatest { event ->
            val (message, action) = when (event) {
                is RecordEvent.Saved -> "Service saved · reminders updated" to "View"
                is RecordEvent.Edited -> "Changes saved · reminders updated" to "Undo"
                is RecordEvent.Deleted -> "Record deleted · reminders updated" to "Undo"
                is RecordEvent.RecallDone ->
                    if (event.recordId != null) "Recall marked done · added to Service history" to "View" else "Recall marked done" to "Undo"
                is RecordEvent.RecallReopened ->
                    if (event.record != null) "Recall marked open · service record removed" to "Undo" else "Recall marked open" to "Undo"
            }
            val recordId = when (event) {
                is RecordEvent.Saved -> event.recordId
                is RecordEvent.RecallDone -> event.recordId
                else -> null
            }
            var undone = false
            try {
                // An Undo stays up longer than a snackbar normally does; the delete sheet says for how long.
                val result = if (recordId != null) {
                    snackbar.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Long)
                } else {
                    withTimeoutOrNull((LogEdits.UNDO_SECONDS * 1000L).milliseconds) {
                        snackbar.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Indefinite)
                    }
                }
                if (result == SnackbarResult.ActionPerformed) {
                    if (recordId != null) {
                        nav.navigate(RecordDetailRoute(recordId))
                    } else {
                        undone = true
                        recordEvents.undo(event) {
                            when (event) {
                                is RecordEvent.Deleted -> showNote("Record restored \u00b7 reminders updated")
                                is RecordEvent.Edited -> showNote("Changes undone \u00b7 reminders updated")
                                else -> Unit
                            }
                        }
                    }
                }
            } finally {
                // Undo wasn't taken, or can no longer be: the photo files the event was holding on to can go.
                if (!undone) recordEvents.discard(event)
            }
        }
    }

    // A tapped notification's destination. This host only exists once the app is unlocked, so nothing
    // is shown before then; the destination waits in PendingDestination until it is.
    val destinations: DestinationViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        // Collected, not keyed on: taking the destination clears it, and an effect keyed on it would
        // cancel itself mid-lookup. On a cold start that lookup waits for the database to open.
        destinations.pending.collect { waiting ->
            if (waiting == null) return@collect
            when (val to = destinations.take()) {
                null -> Unit
                Destination.Reminders -> nav.navigate(RemindersRoute) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                // Opened over the Garage tab, so Back from either lands there.
                is Destination.Recalls -> nav.navigate(RecallsRoute(to.vehicleId))
                is Destination.UpdateMileage -> nav.navigate(VehicleHomeRoute(to.vehicleId, updateMileage = true))
                Destination.Backup -> nav.navigate(BackupRoute) { launchSingleTop = true }
                Destination.AddVehicle -> nav.navigate(AddVehicleRoute) { launchSingleTop = true }
            }
        }
    }

    val restore: RestoreViewModel = hiltViewModel()
    val exportResults: ExportResultsViewModel = hiltViewModel()
    LaunchedEffect(Unit) { exportResults.results.collect { snackbar.showSnackbar(it) } }

    // For one-off confirmations with a single action, usually Undo.
    val showMessage: (String, String, () -> Unit) -> Unit = { message, actionLabel, onAction ->
        scope.launch {
            val result = snackbar.showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }


    // A saved or deleted record ends on the History list, which RecordActions.focus points at the right vehicle.
    // The tab the user was on keeps its screens for when they go back to it.
    val showHistory: () -> Unit = {
        nav.navigate(HistoryRoute) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
    }

    // A vehicle that left the garage takes its home and details screens with it.
    val onArchived: (String, () -> Unit) -> Unit = { name, undo ->
        nav.popBackStack<GarageRoute>(inclusive = false)
        showMessage("$name moved to Archived vehicles", "Undo", undo)
    }

    // Shown whenever a backup file has been picked, from whichever screen started it.
    RestoreSheet(
        onRestored = { summary ->
            nav.popBackStack<GarageRoute>(inclusive = false)
            showNote("Restored ${BackupRules.restoreSummary(summary.vehicles, summary.records, summary.photos)}")
        },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(tab.type) == true ||
                                (onVehicleHome && tab.route == GarageRoute),
                            onClick = {
                                // Re-tapping Garage from a vehicle goes back to the list.
                                if (onVehicleHome && tab.route == GarageRoute) {
                                    nav.popBackStack<GarageRoute>(inclusive = false)
                                    return@NavigationBarItem
                                }
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = GarageRoute, modifier = Modifier.padding(padding)) {
            screen<GarageRoute> {
                GarageScreen(
                    onAddVehicle = { nav.navigate(AddVehicleRoute) },
                    onOpenVehicle = { vehicleId -> nav.navigate(VehicleHomeRoute(vehicleId)) },
                    onOpenArchive = { nav.navigate(ArchivedVehiclesRoute) },
                    onRestore = { restore.pick() },
                    onSeeReminders = {
                        nav.navigate(RemindersRoute) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            screen<ReadDashRoute> { entry ->
                ReadDashScreen(
                    last = entry.toRoute<ReadDashRoute>().last,
                    unit = DistanceUnit.of(entry.toRoute<ReadDashRoute>().unit),
                    onClose = { nav.popBackStack() },
                    onUse = { miles ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(DASH_READING, miles)
                        nav.popBackStack()
                    },
                )
            }
            screen<VehicleHomeRoute> { entry ->
                // What the camera read comes back through this entry's saved state, as a scanned VIN does.
                val dashReading by entry.savedStateHandle.getStateFlow<Int?>(DASH_READING, null).collectAsStateWithLifecycle()
                VehicleHomeScreen(
                    onReadDash = { last, unit -> nav.navigate(ReadDashRoute(last, unit.name)) },
                    dashReading = dashReading,
                    onDashReadingUsed = { entry.savedStateHandle[DASH_READING] = null },
                    onBack = { nav.popBackStack() },
                    onLogService = { vehicleId -> nav.navigate(LogServiceRoute(vehicleId)) },
                    onLogItem = { vehicleId, scheduleItemId -> nav.navigate(LogServiceRoute(vehicleId, scheduleItemId = scheduleItemId)) },
                    onOpenRules = { nav.navigate(ReminderRulesRoute) },
                    onOpenRecalls = { vehicleId -> nav.navigate(RecallsRoute(vehicleId)) },
                    onOpenDetails = { vehicleId -> nav.navigate(VehicleDetailsRoute(vehicleId)) },
                    onEnterLastDone = { vehicleId -> nav.navigate(LastDoneRoute(vehicleId)) },
                    onMessage = showMessage,
                )
            }
            screen<VehicleDetailsRoute> {
                VehicleDetailsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenRecalls = { vehicleId -> nav.navigate(RecallsRoute(vehicleId)) },
                    onOpenSchedule = { vehicleId -> nav.navigate(ScheduleRoute(vehicleId)) },
                    onArchived = onArchived,
                    onDelete = { vehicleId -> nav.navigate(DeleteVehicleRoute(vehicleId)) },
                    onOpenTorqueSpecs = { vehicleId -> nav.navigate(TorqueSpecsRoute(vehicleId)) },
                    onOpenMoreTakes = { vehicleId -> nav.navigate(FluidsPartsRoute(vehicleId)) },
                    onOpenReadings = { vehicleId -> nav.navigate(MileageReadingsRoute(vehicleId)) },
                    onOpenImport = { vehicleId -> nav.navigate(ImportRoute(vehicleId)) },
                    onOpenDocuments = { vehicleId -> nav.navigate(DocumentsRoute(vehicleId)) },
                    onClearHistory = { vehicleId -> nav.navigate(ClearHistoryRoute(vehicleId)) },
                )
            }
            screen<ClearHistoryRoute> {
                ClearHistoryScreen(
                    onCancel = { nav.popBackStack() },
                    onCleared = { n ->
                        nav.popBackStack()
                        showNote(if (n == 1) "1 service record deleted" else "$n service records deleted")
                    },
                )
            }
            screen<MileageReadingsRoute> {
                MileageReadingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) },
                    onMessage = showMessage,
                )
            }
            screen<FluidsPartsRoute> { FluidsPartsScreen(onBack = { nav.popBackStack() }) }
            screen<DocumentsRoute> { DocumentsScreen(onBack = { nav.popBackStack() }) }
            screen<TorqueSpecsRoute> { TorqueSpecsScreen(onBack = { nav.popBackStack() }, onMessage = showMessage) }
            screen<DeleteVehicleRoute> {
                DeleteVehicleScreen(
                    onCancel = { nav.popBackStack() },
                    onDeleted = { name ->
                        nav.popBackStack<GarageRoute>(inclusive = false)
                        showNote("$name and all its data deleted")
                    },
                    onArchived = onArchived,
                )
            }
            screen<ArchivedVehiclesRoute> {
                ArchivedVehiclesScreen(
                    onBack = { nav.popBackStack() },
                    onViewHistory = { vehicleId -> nav.navigate(ArchivedHistoryRoute(vehicleId)) },
                    onDelete = { vehicleId -> nav.navigate(DeleteVehicleRoute(vehicleId)) },
                    onMessage = showMessage,
                )
            }
            screen<ArchivedHistoryRoute> {
                HistoryScreen(
                    onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) },
                    onBack = { nav.popBackStack() },
                )
            }
            screen<RecallsRoute> { RecallsScreen(onBack = { nav.popBackStack() }) }
            screen<AddVehicleRoute> { entry ->
                // The scanner hands its VIN back through this entry's saved state.
                val scannedVin by entry.savedStateHandle.getStateFlow<String?>(SCANNED_VIN, null).collectAsStateWithLifecycle()
                AddVehicleScreen(
                    onBack = { nav.popBackStack() },
                    onNoVin = { nav.navigate(ManualAddRoute) },
                    onScan = { nav.navigate(ScanVinRoute) },
                    scannedVin = scannedVin,
                    onScannedVinUsed = { entry.savedStateHandle[SCANNED_VIN] = null },
                    onSaved = { vehicleId ->
                        nav.navigate(ScheduleSetupRoute(vehicleId)) {
                            popUpTo<AddVehicleRoute> { inclusive = true }
                        }
                    },
                )
            }
            screen<ScanVinRoute> {
                ScanVinScreen(
                    onClose = { nav.popBackStack() },
                    onUseVin = { vin ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(SCANNED_VIN, vin)
                        nav.popBackStack()
                    },
                )
            }
            screen<ManualAddRoute> {
                ManualAddScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { vehicleId ->
                        // Both add screens go, so Back from the schedule lands on the garage.
                        nav.navigate(ScheduleSetupRoute(vehicleId)) {
                            popUpTo<AddVehicleRoute> { inclusive = true }
                        }
                    },
                )
            }
            screen<LogServiceRoute> {
                LogServiceScreen(
                    onDone = { nav.popBackStack() },
                    onAdded = {
                        nav.popBackStack()
                        showHistory()
                    },
                    onDeleteInstead = {
                        // Editing is only reached from the record's own screen, which has the confirmation.
                        nav.previousBackStackEntry?.savedStateHandle?.set(CONFIRM_DELETE, true)
                        nav.popBackStack()
                    },
                )
            }
            screen<ScheduleSetupRoute> {
                ScheduleSetupScreen(
                    onDone = { nav.popBackStack() },
                    // Takes setup's place, so Back from the list doesn't offer the schedule a second time.
                    onEnterLastDone = { vehicleId -> nav.navigate(LastDoneRoute(vehicleId)) { popUpTo<ScheduleSetupRoute> { inclusive = true } } },
                )
            }
            screen<MilestonesRoute> {
                MilestonesScreen(onBack = { nav.popBackStack() }, onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) })
            }
            screen<LastDoneRoute> {
                LastDoneScreen(onDone = { nav.popBackStack() }, onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) })
            }
            screen<ScheduleRoute> {
                ScheduleScreen(
                    onBack = { nav.popBackStack() },
                    onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) },
                    onSetUp = { vehicleId -> nav.navigate(ScheduleSetupRoute(vehicleId)) },
                    onEnterLastDone = { vehicleId -> nav.navigate(LastDoneRoute(vehicleId)) },
                    onOpenMilestones = { vehicleId -> nav.navigate(MilestonesRoute(vehicleId)) },
                )
            }
            screen<HistoryRoute> {
                HistoryScreen(onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) })
            }
            screen<RecordDetailRoute> { entry ->
                val confirmDelete by entry.savedStateHandle.getStateFlow(CONFIRM_DELETE, false).collectAsStateWithLifecycle()
                RecordDetailScreen(
                    onBack = { nav.popBackStack() },
                    onDeleted = {
                        nav.popBackStack()
                        showHistory()
                    },
                    confirmDelete = confirmDelete,
                    onConfirmDeleteShown = { entry.savedStateHandle[CONFIRM_DELETE] = false },
                    onEdit = { vehicleId, recordId -> nav.navigate(LogServiceRoute(vehicleId, recordId)) },
                    onOpenPhoto = { recordId, index -> nav.navigate(PhotoViewerRoute(recordId, index)) },
                )
            }
            screen<PhotoViewerRoute> { PhotoViewerScreen(onClose = { nav.popBackStack() }) }
            screen<RemindersRoute> {
                RemindersScreen(
                    onLogIt = { vehicleId, scheduleItemId ->
                        nav.navigate(LogServiceRoute(vehicleId, scheduleItemId = scheduleItemId))
                    },
                    onAddVehicle = { nav.navigate(AddVehicleRoute) },
                    onOpenRules = { nav.navigate(ReminderRulesRoute) },
                    onOpenNotifications = { nav.navigate(NotificationsRoute) },
                    onMessage = showMessage,
                )
            }
            screen<ReminderRulesRoute> { ReminderRulesScreen(onBack = { nav.popBackStack() }) }
            screen<SettingsRoute> {
                SettingsScreen(
                    onOpenRules = { nav.navigate(ReminderRulesRoute) },
                    onOpenNotifications = { nav.navigate(NotificationsRoute) },
                    onOpenBackup = { nav.navigate(BackupRoute) },
                    onOpenImport = { nav.navigate(ImportRoute()) },
                    onDeleteAll = { nav.navigate(DeleteAllRoute) },
                    onOpenAbout = { nav.navigate(AboutRoute) },
                    onOpenSchedule = { vehicleId -> nav.navigate(ScheduleRoute(vehicleId)) },
                )
            }
            screen<AboutRoute> {
                AboutScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPrivacy = { nav.navigate(PrivacyRoute) },
                    onOpenLicences = { nav.navigate(LicencesRoute) },
                    onMessage = showNote,
                )
            }
            screen<PrivacyRoute> { PrivacyScreen(onBack = { nav.popBackStack() }) }
            screen<LicencesRoute> {
                LicencesScreen(onBack = { nav.popBackStack() }, onOpenLicence = { id -> nav.navigate(LicenceTextRoute(id)) })
            }
            screen<LicenceTextRoute> { LicenceTextScreen(onBack = { nav.popBackStack() }, onMessage = showNote) }
            screen<DeleteAllRoute> {
                DeleteAllScreen(onCancel = { nav.popBackStack() }, onBackupFirst = { nav.navigate(BackupRoute) })
            }
            screen<ImportRoute> {
                ImportScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTorqueSpecs = { vehicleId -> nav.navigate(TorqueSpecsRoute(vehicleId)) },
                    onOpenSchedule = { vehicleId -> nav.navigate(ScheduleRoute(vehicleId)) },
                    onEnterLastDone = { vehicleId -> nav.navigate(LastDoneRoute(vehicleId)) },
                    onOpenMilestones = { vehicleId -> nav.navigate(MilestonesRoute(vehicleId)) },
                    onOpenHistory = { vehicleId, archived -> if (archived) nav.navigate(ArchivedHistoryRoute(vehicleId)) else showHistory() },
                )
            }
            screen<BackupRoute> { BackupScreen(onBack = { nav.popBackStack() }, onRestore = { restore.pick() }) }
            screen<NotificationsRoute> {
                NotificationsScreen(onBack = { nav.popBackStack() }, onOpenRules = { nav.navigate(ReminderRulesRoute) })
            }
        }
    }
}

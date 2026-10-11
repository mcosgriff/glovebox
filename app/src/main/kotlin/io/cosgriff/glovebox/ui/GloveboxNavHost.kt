package io.cosgriff.glovebox.ui

import io.cosgriff.glovebox.ui.settings.DeleteAllScreen
import io.cosgriff.glovebox.ui.settings.RestoreViewModel
import io.cosgriff.glovebox.ui.settings.RestoreSheet
import io.cosgriff.glovebox.domain.BackupRules
import io.cosgriff.glovebox.ui.settings.BackupScreen
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
import io.cosgriff.glovebox.ui.scan.ScanVinScreen
import io.cosgriff.glovebox.ui.schedule.ScheduleScreen
import io.cosgriff.glovebox.ui.schedule.ScheduleSetupScreen
import io.cosgriff.glovebox.ui.vehicle.ArchivedVehiclesScreen
import io.cosgriff.glovebox.ui.vehicle.DeleteVehicleScreen
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

// Type-safe routes. Matches the navigation map on the design canvas.
@Serializable object GarageRoute
@Serializable object HistoryRoute
@Serializable object RemindersRoute
@Serializable object SettingsRoute
@Serializable object AddVehicleRoute
@Serializable object ManualAddRoute
@Serializable object ScanVinRoute
@Serializable object ReminderRulesRoute
@Serializable object NotificationsRoute
@Serializable object BackupRoute
@Serializable object DeleteAllRoute
@Serializable data class ScheduleSetupRoute(val vehicleId: Long)
/** A vehicle's schedule once it has been set up. */
@Serializable data class ScheduleRoute(val vehicleId: Long)
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
@Serializable data class TorqueSpecsRoute(val vehicleId: Long)
/** One archived vehicle's history, read-only. */
@Serializable data class ArchivedHistoryRoute(val vehicleId: Long)

private const val SCANNED_VIN = "scannedVin"

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
    LaunchedEffect(Unit) {
        recordEvents.events.collect { event ->
            val (message, action) = when (event) {
                is RecordEvent.Saved -> "Service saved · reminders updated" to "View"
                is RecordEvent.Edited -> "Changes saved · reminders updated" to "Undo"
                is RecordEvent.Deleted -> "Record deleted · reminders updated" to "Undo"
                is RecordEvent.RecallDone ->
                    if (event.recordId != null) "Recall marked done · added to Service history" to "View" else "Recall marked done" to "Undo"
                is RecordEvent.RecallReopened ->
                    if (event.record != null) "Recall marked open · service record removed" to "Undo" else "Recall marked open" to "Undo"
            }
            val result = snackbar.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                val recordId = when (event) {
                    is RecordEvent.Saved -> event.recordId
                    is RecordEvent.RecallDone -> event.recordId
                    else -> null
                }
                if (recordId != null) nav.navigate(RecordDetailRoute(recordId)) else recordEvents.undo(event)
            } else {
                // Undo wasn't taken: the photo files the event was holding on to can go.
                recordEvents.discard(event)
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

    // For plain confirmations with nothing to undo.
    val showNote: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }

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
            composable<GarageRoute> {
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
            composable<VehicleHomeRoute> {
                VehicleHomeScreen(
                    onBack = { nav.popBackStack() },
                    onLogService = { vehicleId -> nav.navigate(LogServiceRoute(vehicleId)) },
                    onOpenRules = { nav.navigate(ReminderRulesRoute) },
                    onOpenRecalls = { vehicleId -> nav.navigate(RecallsRoute(vehicleId)) },
                    onOpenDetails = { vehicleId -> nav.navigate(VehicleDetailsRoute(vehicleId)) },
                    onMessage = showMessage,
                )
            }
            composable<VehicleDetailsRoute> {
                VehicleDetailsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenRecalls = { vehicleId -> nav.navigate(RecallsRoute(vehicleId)) },
                    onOpenSchedule = { vehicleId -> nav.navigate(ScheduleRoute(vehicleId)) },
                    onArchived = onArchived,
                    onDelete = { vehicleId -> nav.navigate(DeleteVehicleRoute(vehicleId)) },
                    onOpenTorqueSpecs = { vehicleId -> nav.navigate(TorqueSpecsRoute(vehicleId)) },
                )
            }
            composable<TorqueSpecsRoute> { TorqueSpecsScreen(onBack = { nav.popBackStack() }, onMessage = showMessage) }
            composable<DeleteVehicleRoute> {
                DeleteVehicleScreen(
                    onCancel = { nav.popBackStack() },
                    onDeleted = { name ->
                        nav.popBackStack<GarageRoute>(inclusive = false)
                        showNote("$name and all its data deleted")
                    },
                    onArchived = onArchived,
                )
            }
            composable<ArchivedVehiclesRoute> {
                ArchivedVehiclesScreen(
                    onBack = { nav.popBackStack() },
                    onViewHistory = { vehicleId -> nav.navigate(ArchivedHistoryRoute(vehicleId)) },
                    onDelete = { vehicleId -> nav.navigate(DeleteVehicleRoute(vehicleId)) },
                    onMessage = showMessage,
                )
            }
            composable<ArchivedHistoryRoute> {
                HistoryScreen(
                    onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable<RecallsRoute> { RecallsScreen(onBack = { nav.popBackStack() }) }
            composable<AddVehicleRoute> { entry ->
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
            composable<ScanVinRoute> {
                ScanVinScreen(
                    onClose = { nav.popBackStack() },
                    onUseVin = { vin ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(SCANNED_VIN, vin)
                        nav.popBackStack()
                    },
                )
            }
            composable<ManualAddRoute> {
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
            composable<LogServiceRoute> { LogServiceScreen(onDone = { nav.popBackStack() }) }
            composable<ScheduleSetupRoute> { ScheduleSetupScreen(onDone = { nav.popBackStack() }) }
            composable<ScheduleRoute> {
                ScheduleScreen(
                    onBack = { nav.popBackStack() },
                    onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) },
                    onSetUp = { vehicleId -> nav.navigate(ScheduleSetupRoute(vehicleId)) },
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) })
            }
            composable<RecordDetailRoute> {
                RecordDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { vehicleId, recordId -> nav.navigate(LogServiceRoute(vehicleId, recordId)) },
                    onOpenPhoto = { recordId, index -> nav.navigate(PhotoViewerRoute(recordId, index)) },
                )
            }
            composable<PhotoViewerRoute> { PhotoViewerScreen(onClose = { nav.popBackStack() }) }
            composable<RemindersRoute> {
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
            composable<ReminderRulesRoute> { ReminderRulesScreen(onBack = { nav.popBackStack() }) }
            composable<SettingsRoute> {
                SettingsScreen(
                    onOpenRules = { nav.navigate(ReminderRulesRoute) },
                    onOpenNotifications = { nav.navigate(NotificationsRoute) },
                    onOpenBackup = { nav.navigate(BackupRoute) },
                    onDeleteAll = { nav.navigate(DeleteAllRoute) },
                )
            }
            composable<DeleteAllRoute> {
                DeleteAllScreen(onCancel = { nav.popBackStack() }, onBackupFirst = { nav.navigate(BackupRoute) })
            }
            composable<BackupRoute> { BackupScreen(onBack = { nav.popBackStack() }, onRestore = { restore.pick() }) }
            composable<NotificationsRoute> {
                NotificationsScreen(onBack = { nav.popBackStack() }, onOpenRules = { nav.navigate(ReminderRulesRoute) })
            }
        }
    }
}

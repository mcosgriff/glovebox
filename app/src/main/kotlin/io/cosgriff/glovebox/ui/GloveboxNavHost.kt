package io.cosgriff.glovebox.ui

import io.cosgriff.glovebox.ui.components.asChoice
import io.cosgriff.glovebox.ui.components.ChooseVehicleDialog
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.settings.restoreSummary
import io.cosgriff.glovebox.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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
import io.cosgriff.glovebox.ui.settings.BackupScreen
import io.cosgriff.glovebox.ui.settings.ImportScreen
import io.cosgriff.glovebox.ui.settings.NotificationsScreen
import io.cosgriff.glovebox.notify.DestinationViewModel
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.notify.ShortcutTask
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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.cosgriff.glovebox.ui.garage.AddVehicleScreen
import io.cosgriff.glovebox.ui.export.ExportResultsViewModel
import io.cosgriff.glovebox.ui.garage.GarageScreen
import io.cosgriff.glovebox.ui.garage.ManualAddScreen
import io.cosgriff.glovebox.ui.history.HistoryScreen
import io.cosgriff.glovebox.ui.history.SortCustomScreen
import io.cosgriff.glovebox.ui.settings.CategoryRulesScreen
import io.cosgriff.glovebox.ui.history.RecordDetailScreen
import io.cosgriff.glovebox.ui.photo.PhotoViewerScreen
import io.cosgriff.glovebox.ui.recalls.RecallsScreen
import io.cosgriff.glovebox.ui.reminders.ReminderRulesScreen
import io.cosgriff.glovebox.ui.reminders.RemindersScreen
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
import io.cosgriff.glovebox.ui.vehicle.DocumentViewerScreen
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
import kotlin.time.Duration.Companion.milliseconds

// Type-safe routes. Matches the navigation map on the design canvas.
@Serializable data object GarageRoute : NavKey
@Serializable data object HistoryRoute : NavKey
@Serializable data object RemindersRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object AddVehicleRoute : NavKey
@Serializable data object ManualAddRoute : NavKey
@Serializable data object ScanVinRoute : NavKey
@Serializable data class MileageReadingsRoute(val vehicleId: Long) : NavKey

/** [last] is the vehicle's latest known reading in dash terms, to tell the odometer from the other numbers in view. */
@Serializable data class ReadDashRoute(val last: Int? = null, val unit: String = "MILES") : NavKey
@Serializable data object ReminderRulesRoute : NavKey
@Serializable data object NotificationsRoute : NavKey
@Serializable data object BackupRoute : NavKey
/** Adds data from a JSON file the user picks to one vehicle. */
/** [vehicleId] starts the page with that vehicle chosen, as Vehicle details opens it. */
@Serializable data class ImportRoute(val vehicleId: Long? = null) : NavKey
@Serializable data object DeleteAllRoute : NavKey
@Serializable data object AboutRoute : NavKey
@Serializable data object PrivacyRoute : NavKey
@Serializable data object LicencesRoute : NavKey
@Serializable data class LicenceTextRoute(val id: String) : NavKey
@Serializable data class ScheduleSetupRoute(val vehicleId: Long) : NavKey
/** A vehicle's schedule once it has been set up. */
@Serializable data class ScheduleRoute(val vehicleId: Long) : NavKey
/** "When were these last done?": a date and mileage for reminders that have never been logged. */
@Serializable data class LastDoneRoute(val vehicleId: Long) : NavKey
/** The schedule laid out by mileage, with what the log says was done at each point. */
@Serializable data class MilestonesRoute(val vehicleId: Long) : NavKey
/** [updateMileage] opens the Update mileage sheet straight away, for the mileage check-in notification. */
@Serializable data class VehicleHomeRoute(val vehicleId: Long, val updateMileage: Boolean = false) : NavKey
/**
 * [recordId] 0 logs a new record; otherwise that record is edited. [scheduleItemId] ticks that
 * reminder's work to start with (new records only).
 */
@Serializable data class LogServiceRoute(val vehicleId: Long, val recordId: Long = 0, val scheduleItemId: Long = 0) : NavKey
@Serializable data class RecordDetailRoute(val recordId: Long) : NavKey
/** One of a record's photos, full screen; [start] is its place in the grid. */
@Serializable data class PhotoViewerRoute(val recordId: Long, val start: Int) : NavKey
@Serializable data class RecallsRoute(val vehicleId: Long) : NavKey
@Serializable data class VehicleDetailsRoute(val vehicleId: Long) : NavKey
@Serializable data object ArchivedVehiclesRoute : NavKey
@Serializable data class DeleteVehicleRoute(val vehicleId: Long) : NavKey
/** Deletes a vehicle's service records (imported, or all) and nothing else. */
@Serializable data class ClearHistoryRoute(val vehicleId: Long) : NavKey
/** The user's category rules, from Settings (#310). */
@Serializable data object CategoryRulesRoute : NavKey
/** Sorts a vehicle's Custom lines under catalog lines, in bulk (#310). */
@Serializable data class SortCustomRoute(val vehicleId: Long) : NavKey
@Serializable data class TorqueSpecsRoute(val vehicleId: Long) : NavKey
@Serializable data class FluidsPartsRoute(val vehicleId: Long) : NavKey
@Serializable data class DocumentsRoute(val vehicleId: Long) : NavKey
@Serializable data class DocumentViewerRoute(val documentId: Long) : NavKey
/** One archived vehicle's history, read-only. */
@Serializable data class ArchivedHistoryRoute(val vehicleId: Long) : NavKey

/**
 * A destination with the page colour behind it. Screens draw no background of their own, and without
 * one the screen underneath shows through while a back swipe is held or a transition runs. Its
 * ViewModels are told the route ([ProvideRoute]).
 */
private inline fun <reified T : NavKey> EntryProviderScope<NavKey>.screen(noinline content: @Composable (T) -> Unit) =
    entry<T> { key ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ProvideRoute(key) { content(key) }
        }
    }

private const val SCANNED_VIN = "scannedVin"
private const val DASH_READING = "dashReading"
private const val CONFIRM_DELETE = "confirmDelete"

private data class Tab(val route: NavKey, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(GarageRoute, R.string.garage_title, Icons.Outlined.Home),
    Tab(HistoryRoute, R.string.history_title, Icons.Outlined.History),
    Tab(RemindersRoute, R.string.reminders_title, Icons.Outlined.Notifications),
    Tab(SettingsRoute, R.string.settings_title, Icons.Outlined.Settings),
)
private const val GARAGE_TAB = 0
private const val HISTORY_TAB = 1
private const val REMINDERS_TAB = 2

/** How many back stacks there are, one per tab; `MainActivity` keeps a ViewModel store for each. */
const val TAB_COUNT = 4

/**
 * Each tab's own back stack, and which tab is showing. Every stack starts at its tab's screen and
 * is never emptied. What is shown is the current tab's stack over the Garage list, so Back from a
 * tab's first screen goes to Garage, and from there out of the app.
 */
@Stable
private class Navigator(val stacks: List<NavBackStack<NavKey>>, tab: MutableIntState) {
    var tab by tab
    private val stack get() = stacks[tab]
    val top: NavKey get() = stack.last()

    /** [singleTop] doesn't stack a screen on itself. */
    fun push(key: NavKey, singleTop: Boolean = false) {
        if (singleTop && top == key) return
        stack.add(key)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else if (tab != GARAGE_TAB) toGarageList()
    }

    /** Puts [key] in place of the last [T] and everything above it, so Back doesn't return to them. */
    inline fun <reified T : NavKey> replace(key: NavKey) {
        val at = stacks[tab].indexOfLast { it is T }
        if (at > 0) stacks[tab].trimTo(at)
        push(key)
    }

    /** A tab comes back as it was left. */
    fun switchTo(index: Int) {
        tab = index
    }

    /** The Garage list, with nothing above it: what is left when a vehicle goes, or everything is restored. */
    fun toGarageList() {
        stack.trimTo(1)
        stacks[GARAGE_TAB].trimTo(1)
        tab = GARAGE_TAB
    }

    /** History's list, which [RecordActions.focus] points at a vehicle and record. The tab left keeps its screens. */
    fun showHistory() {
        stacks[HISTORY_TAB].trimTo(1)
        tab = HISTORY_TAB
    }

    /** The entries to show, given each tab's: Garage's first screen under the current tab's. */
    fun <T : Any> shown(entries: List<List<NavEntry<T>>>): List<NavEntry<T>> =
        if (tab == GARAGE_TAB) entries[GARAGE_TAB] else listOf(entries[GARAGE_TAB].first()) + entries[tab]
}

@Composable
private fun rememberNavigator(): Navigator {
    val stacks = tabs.map { rememberNavBackStack(it.route) }
    val tab = rememberSaveable { mutableIntStateOf(GARAGE_TAB) }
    return remember(stacks, tab) { Navigator(stacks, tab) }
}

/**
 * [screenModels] holds each tab's screens' ViewModels. It lives above the lock in `MainActivity`:
 * the lock takes this host out of composition, and a store made in here would clear every screen's
 * ViewModels when it did.
 */
@Composable
fun GloveboxNavHost(screenModels: List<ViewModelStoreProvider>) {
    val nav = rememberNavigator()
    val top = nav.top
    // Vehicle home sits under the Garage tab and keeps the bar, as on the design.
    val onVehicleHome = top is VehicleHomeRoute
    val showBar = onVehicleHome || tabs.any { it.route == top }
    val results = rememberSaveable(saver = ResultsSaver) { mutableStateMapOf() }

    // Service log changes are announced here so Undo works whichever screen the user lands on.
    val recordEvents: RecordEventsViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // For plain confirmations with nothing to undo.
    val showNote: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    val res = LocalResources.current
    LaunchedEffect(Unit) {
        // Latest only: a newer change replaces the bar for an older one, whose Undo is then gone.
        recordEvents.events.collectLatest { event ->
            val (message, action) = when (event) {
                is RecordEvent.Saved -> res.getString(R.string.event_saved) to res.getString(R.string.view)
                is RecordEvent.Edited -> res.getString(R.string.event_edited) to res.getString(R.string.undo)
                is RecordEvent.Deleted -> res.getString(R.string.event_deleted) to res.getString(R.string.undo)
                is RecordEvent.RecallDone -> if (event.recordId != null) {
                    res.getString(R.string.event_recall_done_logged) to res.getString(R.string.view)
                } else {
                    res.getString(R.string.event_recall_done) to res.getString(R.string.undo)
                }
                is RecordEvent.RecallReopened ->
                    res.getString(if (event.record != null) R.string.event_recall_open_removed else R.string.event_recall_open) to res.getString(R.string.undo)
                is RecordEvent.Filed ->
                    res.getQuantityString(R.plurals.event_lines_filed, event.lineIds.size, event.lineIds.size.asNumber()) to res.getString(R.string.undo)
            }
            val recordId = when (event) {
                is RecordEvent.Saved -> event.recordId
                is RecordEvent.RecallDone -> event.recordId
                else -> null
            }
            var undone = false
            try {
                // An Undo stays up for a set time, which the delete sheet states; the ✕ dismisses it sooner.
                val result = if (recordId != null) {
                    snackbar.showSnackbar(message, actionLabel = action, withDismissAction = true, duration = SnackbarDuration.Long)
                } else {
                    withTimeoutOrNull((LogEdits.UNDO_SECONDS * 1000L).milliseconds) {
                        snackbar.showSnackbar(message, actionLabel = action, withDismissAction = true, duration = SnackbarDuration.Indefinite)
                    }
                }
                if (result == SnackbarResult.ActionPerformed) {
                    if (recordId != null) {
                        nav.push(RecordDetailRoute(recordId))
                    } else {
                        undone = true
                        recordEvents.undo(event) {
                            when (event) {
                                is RecordEvent.Deleted -> showNote(res.getString(R.string.event_restored))
                                is RecordEvent.Edited, is RecordEvent.Filed -> showNote(res.getString(R.string.event_undone))
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

    // A tapped notification's or launcher shortcut's destination. This host only exists once the app is unlocked, so nothing
    // is shown before then; the destination waits in PendingDestination until it is.
    val destinations: DestinationViewModel = hiltViewModel()
    // A launcher shortcut's task while the user picks which vehicle it is for. Saved, so it comes back after the lock.
    var choosingFor by rememberSaveable { mutableStateOf<ShortcutTask?>(null) }
    val forVehicle: (ShortcutTask, Long) -> Unit = { task, vehicleId ->
        when (task) {
            ShortcutTask.LOG_SERVICE -> nav.push(LogServiceRoute(vehicleId))
            ShortcutTask.UPDATE_MILEAGE -> nav.push(VehicleHomeRoute(vehicleId, updateMileage = true))
        }
    }
    LaunchedEffect(Unit) {
        // Collected, not keyed on: taking the destination clears it, and an effect keyed on it would
        // cancel itself mid-lookup. On a cold start that lookup waits for the database to open. Nothing
        // here suspends after take(), which clears it only once it has been looked up.
        destinations.pending.collect { waiting ->
            if (waiting == null) return@collect
            val arrival = destinations.take() ?: return@collect
            when (val to = arrival.to) {
                Destination.Reminders -> nav.switchTo(REMINDERS_TAB)
                // Opened over the Garage tab, so Back from either lands there.
                is Destination.Recalls -> nav.push(RecallsRoute(to.vehicleId))
                is Destination.UpdateMileage -> nav.push(VehicleHomeRoute(to.vehicleId, updateMileage = true))
                // Not stacked on itself when the app was already showing that vehicle.
                is Destination.VehicleHome -> nav.push(VehicleHomeRoute(to.vehicleId), singleTop = true)
                Destination.Backup -> nav.push(BackupRoute, singleTop = true)
                Destination.AddVehicle -> nav.push(AddVehicleRoute, singleTop = true)
                // No vehicle to do it for: the garage, where one is added (or the sold ones are).
                is Destination.Shortcut -> when (arrival.activeVehicleIds.size) {
                    0 -> nav.toGarageList()
                    1 -> forVehicle(to.task, arrival.activeVehicleIds.single())
                    else -> choosingFor = to.task
                }
            }
        }
    }
    choosingFor?.let { task ->
        val active by destinations.activeVehicles.collectAsStateWithLifecycle(initialValue = null)
        active?.let { vehicles ->
            ChooseVehicleDialog(
                task = task,
                vehicles = vehicles.map { it.asChoice() },
                onChoose = { vehicleId ->
                    choosingFor = null
                    forVehicle(task, vehicleId)
                },
                onDismiss = { choosingFor = null },
            )
        }
    }

    val restore: RestoreViewModel = hiltViewModel()
    val exportResults: ExportResultsViewModel = hiltViewModel()
    LaunchedEffect(Unit) { exportResults.results.collect { snackbar.showSnackbar(it) } }

    // For one-off confirmations with a single action, usually Undo.
    val showMessage: (String, String, () -> Unit) -> Unit = { message, actionLabel, onAction ->
        scope.launch {
            val result = snackbar.showSnackbar(message, actionLabel = actionLabel, withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }


    // A saved or deleted record ends on the History list, which RecordActions.focus points at the right vehicle.
    // The tab the user was on keeps its screens for when they go back to it.
    val showHistory: () -> Unit = { nav.showHistory() }

    // A vehicle that left the garage takes its home and details screens with it.
    val onArchived: (String, () -> Unit) -> Unit = { name, undo ->
        nav.toGarageList()
        showMessage(res.getString(R.string.event_archived, name), res.getString(R.string.undo), undo)
    }

    // Shown whenever a backup file has been picked, from whichever screen started it.
    RestoreSheet(
        onRestored = { summary ->
            nav.toGarageList()
            showNote(res.getString(R.string.event_restored_backup, restoreSummary(summary.vehicles, summary.records, summary.photos, summary.documents)))
        },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = nav.tab == index,
                            onClick = {
                                // Re-tapping Garage from a vehicle goes back to the list.
                                if (onVehicleHome && index == GARAGE_TAB) nav.toGarageList() else nav.switchTo(index)
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val destinations = entryProvider<NavKey> {
            screen<GarageRoute> {
                GarageScreen(
                    showing = nav.tab == GARAGE_TAB && top == GarageRoute,
                    onAddVehicle = { nav.push(AddVehicleRoute) },
                    onOpenVehicle = { vehicleId -> nav.push(VehicleHomeRoute(vehicleId)) },
                    onOpenArchive = { nav.push(ArchivedVehiclesRoute) },
                    onRestore = { restore.pick() },
                    onSeeReminders = {
                        nav.switchTo(REMINDERS_TAB)
                    },
                )
            }
            screen<ReadDashRoute> { route ->
                ReadDashScreen(
                    last = route.last,
                    unit = DistanceUnit.of(route.unit),
                    onClose = { nav.pop() },
                    onUse = { miles ->
                        results[DASH_READING] = miles
                        nav.pop()
                    },
                )
            }
            screen<VehicleHomeRoute> {
                // What the camera read comes back through the results, as a scanned VIN does.
                val dashReading = results[DASH_READING] as Int?
                VehicleHomeScreen(
                    onReadDash = { last, unit -> nav.push(ReadDashRoute(last, unit.name)) },
                    dashReading = dashReading,
                    onDashReadingUsed = { results.remove(DASH_READING) },
                    onBack = { nav.pop() },
                    onLogService = { vehicleId -> nav.push(LogServiceRoute(vehicleId)) },
                    onLogItem = { vehicleId, scheduleItemId -> nav.push(LogServiceRoute(vehicleId, scheduleItemId = scheduleItemId)) },
                    onOpenRules = { nav.push(ReminderRulesRoute) },
                    onOpenRecalls = { vehicleId -> nav.push(RecallsRoute(vehicleId)) },
                    onOpenDetails = { vehicleId -> nav.push(VehicleDetailsRoute(vehicleId)) },
                    onEnterLastDone = { vehicleId -> nav.push(LastDoneRoute(vehicleId)) },
                    onMessage = showMessage,
                )
            }
            screen<VehicleDetailsRoute> {
                VehicleDetailsScreen(
                    onBack = { nav.pop() },
                    onOpenRecalls = { vehicleId -> nav.push(RecallsRoute(vehicleId)) },
                    onOpenSchedule = { vehicleId -> nav.push(ScheduleRoute(vehicleId)) },
                    onArchived = onArchived,
                    onDelete = { vehicleId -> nav.push(DeleteVehicleRoute(vehicleId)) },
                    onOpenTorqueSpecs = { vehicleId -> nav.push(TorqueSpecsRoute(vehicleId)) },
                    onOpenMoreTakes = { vehicleId -> nav.push(FluidsPartsRoute(vehicleId)) },
                    onOpenReadings = { vehicleId -> nav.push(MileageReadingsRoute(vehicleId)) },
                    onOpenImport = { vehicleId -> nav.push(ImportRoute(vehicleId)) },
                    onOpenDocuments = { vehicleId -> nav.push(DocumentsRoute(vehicleId)) },
                    onClearHistory = { vehicleId -> nav.push(ClearHistoryRoute(vehicleId)) },
                )
            }
            screen<ClearHistoryRoute> {
                ClearHistoryScreen(
                    onCancel = { nav.pop() },
                    onCleared = { n ->
                        nav.pop()
                        showNote(res.getQuantityString(R.plurals.event_records_deleted, n, n.asNumber()))
                    },
                )
            }
            screen<MileageReadingsRoute> {
                MileageReadingsScreen(
                    onBack = { nav.pop() },
                    onOpenRecord = { recordId -> nav.push(RecordDetailRoute(recordId)) },
                    onMessage = showMessage,
                )
            }
            screen<FluidsPartsRoute> { FluidsPartsScreen(onBack = { nav.pop() }) }
            screen<DocumentsRoute> {
                DocumentsScreen(onBack = { nav.pop() }, onView = { documentId -> nav.push(DocumentViewerRoute(documentId)) })
            }
            screen<DocumentViewerRoute> { DocumentViewerScreen(onClose = { nav.pop() }) }
            screen<TorqueSpecsRoute> { TorqueSpecsScreen(onBack = { nav.pop() }, onMessage = showMessage) }
            screen<DeleteVehicleRoute> {
                DeleteVehicleScreen(
                    onCancel = { nav.pop() },
                    onDeleted = { name ->
                        nav.toGarageList()
                        showNote(res.getString(R.string.event_vehicle_deleted, name))
                    },
                    onArchived = onArchived,
                )
            }
            screen<ArchivedVehiclesRoute> {
                ArchivedVehiclesScreen(
                    onBack = { nav.pop() },
                    onViewHistory = { vehicleId -> nav.push(ArchivedHistoryRoute(vehicleId)) },
                    onDelete = { vehicleId -> nav.push(DeleteVehicleRoute(vehicleId)) },
                    onMessage = showMessage,
                )
            }
            screen<ArchivedHistoryRoute> {
                HistoryScreen(
                    onOpenRecord = { recordId -> nav.push(RecordDetailRoute(recordId)) },
                    onBack = { nav.pop() },
                )
            }
            screen<RecallsRoute> { RecallsScreen(onBack = { nav.pop() }) }
            screen<AddVehicleRoute> {
                // The scanner hands its VIN back through the results.
                val scannedVin = results[SCANNED_VIN] as String?
                AddVehicleScreen(
                    onBack = { nav.pop() },
                    onNoVin = { nav.push(ManualAddRoute) },
                    onScan = { nav.push(ScanVinRoute) },
                    scannedVin = scannedVin,
                    onScannedVinUsed = { results.remove(SCANNED_VIN) },
                    onSaved = { vehicleId ->
                        nav.replace<AddVehicleRoute>(ScheduleSetupRoute(vehicleId))
                    },
                )
            }
            screen<ScanVinRoute> {
                ScanVinScreen(
                    onClose = { nav.pop() },
                    onUseVin = { vin ->
                        results[SCANNED_VIN] = vin
                        nav.pop()
                    },
                )
            }
            screen<ManualAddRoute> {
                ManualAddScreen(
                    onBack = { nav.pop() },
                    onSaved = { vehicleId ->
                        // Both add screens go, so Back from the schedule lands on the garage.
                        nav.replace<AddVehicleRoute>(ScheduleSetupRoute(vehicleId))
                    },
                )
            }
            screen<LogServiceRoute> {
                LogServiceScreen(
                    onDone = { nav.pop() },
                    onAdded = {
                        nav.pop()
                        showHistory()
                    },
                    onDeleteInstead = {
                        // Editing is only reached from the record's own screen, which has the confirmation.
                        results[CONFIRM_DELETE] = true
                        nav.pop()
                    },
                )
            }
            screen<ScheduleSetupRoute> {
                ScheduleSetupScreen(
                    onDone = { nav.pop() },
                    // Takes setup's place, so Back from the list doesn't offer the schedule a second time.
                    onEnterLastDone = { vehicleId -> nav.replace<ScheduleSetupRoute>(LastDoneRoute(vehicleId)) },
                )
            }
            screen<MilestonesRoute> {
                MilestonesScreen(onBack = { nav.pop() }, onOpenRecord = { recordId -> nav.push(RecordDetailRoute(recordId)) })
            }
            screen<LastDoneRoute> {
                LastDoneScreen(onDone = { nav.pop() }, onOpenRecord = { recordId -> nav.push(RecordDetailRoute(recordId)) })
            }
            screen<ScheduleRoute> {
                ScheduleScreen(
                    onBack = { nav.pop() },
                    onOpenRecord = { recordId -> nav.push(RecordDetailRoute(recordId)) },
                    onSetUp = { vehicleId -> nav.push(ScheduleSetupRoute(vehicleId)) },
                    onEnterLastDone = { vehicleId -> nav.push(LastDoneRoute(vehicleId)) },
                    onOpenMilestones = { vehicleId -> nav.push(MilestonesRoute(vehicleId)) },
                )
            }
            screen<HistoryRoute> {
                HistoryScreen(
                    onOpenRecord = { recordId -> nav.push(RecordDetailRoute(recordId)) },
                    onSortCustom = { vehicleId -> nav.push(SortCustomRoute(vehicleId)) },
                )
            }
            screen<SortCustomRoute> { SortCustomScreen(onBack = { nav.pop() }) }
            screen<RecordDetailRoute> {
                val confirmDelete = results[CONFIRM_DELETE] == true
                RecordDetailScreen(
                    onBack = { nav.pop() },
                    onDeleted = {
                        nav.pop()
                        showHistory()
                    },
                    confirmDelete = confirmDelete,
                    onConfirmDeleteShown = { results.remove(CONFIRM_DELETE) },
                    onEdit = { vehicleId, recordId -> nav.push(LogServiceRoute(vehicleId, recordId)) },
                    onOpenPhoto = { recordId, index -> nav.push(PhotoViewerRoute(recordId, index)) },
                    onOpenRecord = { nav.push(RecordDetailRoute(it)) },
                )
            }
            screen<PhotoViewerRoute> { PhotoViewerScreen(onClose = { nav.pop() }) }
            screen<RemindersRoute> {
                RemindersScreen(
                    onLogIt = { vehicleId, scheduleItemId ->
                        nav.push(LogServiceRoute(vehicleId, scheduleItemId = scheduleItemId))
                    },
                    onAddVehicle = { nav.push(AddVehicleRoute) },
                    onOpenRules = { nav.push(ReminderRulesRoute) },
                    onOpenNotifications = { nav.push(NotificationsRoute) },
                    onMessage = showMessage,
                )
            }
            screen<ReminderRulesRoute> { ReminderRulesScreen(onBack = { nav.pop() }) }
            screen<SettingsRoute> {
                SettingsScreen(
                    onOpenRules = { nav.push(ReminderRulesRoute) },
                    onOpenNotifications = { nav.push(NotificationsRoute) },
                    onOpenBackup = { nav.push(BackupRoute) },
                    onOpenImport = { nav.push(ImportRoute()) },
                    onDeleteAll = { nav.push(DeleteAllRoute) },
                    onOpenAbout = { nav.push(AboutRoute) },
                    onOpenSchedule = { vehicleId -> nav.push(ScheduleRoute(vehicleId)) },
                    onOpenCategoryRules = { nav.push(CategoryRulesRoute) },
                )
            }
            screen<CategoryRulesRoute> { CategoryRulesScreen(onBack = { nav.pop() }) }
            screen<AboutRoute> {
                AboutScreen(
                    onBack = { nav.pop() },
                    onOpenPrivacy = { nav.push(PrivacyRoute) },
                    onOpenLicences = { nav.push(LicencesRoute) },
                    onMessage = showNote,
                )
            }
            screen<PrivacyRoute> { PrivacyScreen(onBack = { nav.pop() }) }
            screen<LicencesRoute> {
                LicencesScreen(onBack = { nav.pop() }, onOpenLicence = { id -> nav.push(LicenceTextRoute(id)) })
            }
            screen<LicenceTextRoute> { LicenceTextScreen(onBack = { nav.pop() }, onMessage = showNote) }
            screen<DeleteAllRoute> {
                DeleteAllScreen(onCancel = { nav.pop() }, onBackupFirst = { nav.push(BackupRoute) })
            }
            screen<ImportRoute> {
                ImportScreen(
                    onBack = { nav.pop() },
                    onOpenTorqueSpecs = { vehicleId -> nav.push(TorqueSpecsRoute(vehicleId)) },
                    onOpenSchedule = { vehicleId -> nav.push(ScheduleRoute(vehicleId)) },
                    onEnterLastDone = { vehicleId -> nav.push(LastDoneRoute(vehicleId)) },
                    onOpenMilestones = { vehicleId -> nav.push(MilestonesRoute(vehicleId)) },
                    onOpenHistory = { vehicleId, archived -> if (archived) nav.push(ArchivedHistoryRoute(vehicleId)) else showHistory() },
                )
            }
            screen<BackupRoute> { BackupScreen(onBack = { nav.pop() }, onRestore = { restore.pick() }) }
            screen<NotificationsRoute> {
                NotificationsScreen(onBack = { nav.pop() }, onOpenRules = { nav.push(ReminderRulesRoute) })
            }
        }
        // Every tab's entries are kept, not only the shown ones, so a tab's screens and their state
        // are still there when the user comes back to it.
        val entries = tabs.indices.map { index ->
            rememberDecoratedNavEntries(
                backStack = nav.stacks[index],
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(screenModels[index]),
                ),
                entryProvider = destinations,
            )
        }
        NavDisplay(entries = nav.shown(entries), modifier = Modifier.padding(padding), onBack = nav::pop)
    }
}

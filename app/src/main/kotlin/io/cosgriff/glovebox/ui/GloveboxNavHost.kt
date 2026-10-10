package io.cosgriff.glovebox.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.cosgriff.glovebox.ui.garage.AddVehicleScreen
import io.cosgriff.glovebox.ui.garage.GarageScreen
import io.cosgriff.glovebox.ui.history.HistoryScreen
import io.cosgriff.glovebox.ui.history.RecordDetailScreen
import io.cosgriff.glovebox.ui.schedule.ScheduleSetupScreen
import io.cosgriff.glovebox.ui.service.LogServiceScreen
import io.cosgriff.glovebox.ui.service.RecordEvent
import io.cosgriff.glovebox.ui.service.RecordEventsViewModel
import io.cosgriff.glovebox.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Type-safe routes. Matches the navigation map on the design canvas.
@Serializable object GarageRoute
@Serializable object HistoryRoute
@Serializable object RemindersRoute
@Serializable object SettingsRoute
@Serializable object AddVehicleRoute
@Serializable data class ScheduleSetupRoute(val vehicleId: Long)
/** [recordId] 0 logs a new record; otherwise that record is edited. */
@Serializable data class LogServiceRoute(val vehicleId: Long, val recordId: Long = 0)
@Serializable data class RecordDetailRoute(val recordId: Long)

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
    val showBar = tabs.any { t -> destination?.hasRoute(t.type) == true }

    // Service log changes are announced here so Undo works whichever screen the user lands on.
    val recordEvents: RecordEventsViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        recordEvents.events.collect { event ->
            val (message, action) = when (event) {
                is RecordEvent.Saved -> "Service saved · reminders updated" to "View"
                is RecordEvent.Edited -> "Changes saved · reminders updated" to "Undo"
                is RecordEvent.Deleted -> "Record deleted · reminders updated" to "Undo"
            }
            val result = snackbar.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                when (event) {
                    is RecordEvent.Saved -> nav.navigate(RecordDetailRoute(event.recordId))
                    else -> recordEvents.undo(event)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(tab.type) == true,
                            onClick = {
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
                    onLogService = { vehicleId -> nav.navigate(LogServiceRoute(vehicleId)) },
                )
            }
            composable<AddVehicleRoute> {
                AddVehicleScreen(
                    onSaved = { vehicleId ->
                        nav.navigate(ScheduleSetupRoute(vehicleId)) {
                            popUpTo<AddVehicleRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<LogServiceRoute> { LogServiceScreen(onDone = { nav.popBackStack() }) }
            composable<ScheduleSetupRoute> { ScheduleSetupScreen(onDone = { nav.popBackStack() }) }
            composable<HistoryRoute> {
                HistoryScreen(onOpenRecord = { recordId -> nav.navigate(RecordDetailRoute(recordId)) })
            }
            composable<RecordDetailRoute> {
                RecordDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { vehicleId, recordId -> nav.navigate(LogServiceRoute(vehicleId, recordId)) },
                )
            }
            composable<RemindersRoute> { PlaceholderScreen("Reminders", "What's due across all vehicles.") }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

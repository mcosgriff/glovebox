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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.cosgriff.glovebox.ui.garage.AddVehicleScreen
import io.cosgriff.glovebox.ui.garage.GarageScreen
import io.cosgriff.glovebox.ui.schedule.ScheduleSetupScreen
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

    Scaffold(
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
            composable<GarageRoute> { GarageScreen(onAddVehicle = { nav.navigate(AddVehicleRoute) }) }
            composable<AddVehicleRoute> {
                AddVehicleScreen(
                    onSaved = { vehicleId ->
                        nav.navigate(ScheduleSetupRoute(vehicleId)) {
                            popUpTo<AddVehicleRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<ScheduleSetupRoute> { ScheduleSetupScreen(onDone = { nav.popBackStack() }) }
            composable<HistoryRoute> { PlaceholderScreen("History", "Service records will list here.") }
            composable<RemindersRoute> { PlaceholderScreen("Reminders", "What's due across all vehicles.") }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

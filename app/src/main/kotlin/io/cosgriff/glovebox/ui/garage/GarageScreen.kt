package io.cosgriff.glovebox.ui.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.Attention
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.UrgentItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class GarageVehicle(val vehicle: VehicleEntity, val odometer: Int?, val comingUp: ComingUp)

data class Garage(val vehicles: List<GarageVehicle>) {
    val overdue: Int get() = vehicles.sumOf { it.comingUp.overdue }
    val dueSoon: Int get() = vehicles.sumOf { it.comingUp.dueSoon }

    /** Only worth showing when there is more than one vehicle to look across. */
    val urgent: List<UrgentItem<VehicleEntity>>
        get() = if (vehicles.size < 2) emptyList() else ReminderPresentation.mostUrgent(vehicles.map { it.vehicle to it.comingUp })
}

@HiltViewModel
class GarageViewModel @Inject constructor(
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
) : ViewModel() {
    /** Null while loading. Recomputed whenever a log, schedule or mileage changes. */
    val garage: StateFlow<Garage?> = combine(
        vehicles.observeActive(),
        schedule.observeAllTracked(),
        service.observeAllRecords(),
        odometer.observeAll(),
        settings.settings,
    ) { active, items, records, readings, s ->
        val itemsBy = items.groupBy { it.vehicleId }
        val recordsBy = records.groupBy { it.record.vehicleId }
        val readingsBy = readings.groupBy { it.vehicleId }
        val today = LocalDate.now()
        Garage(
            active.map { v ->
                val entries = recordsBy[v.id].orEmpty().map { it.toEntry() }
                val updates = readingsBy[v.id].orEmpty().map { OdometerReading(it.date, it.odometer) }
                val tracked = itemsBy[v.id].orEmpty().mapNotNull { it.toTracked(s.defaultRule) }
                GarageVehicle(
                    vehicle = v,
                    odometer = VehicleReminders.currentOdometer(entries, updates),
                    comingUp = ReminderPresentation.comingUp(VehicleReminders.statuses(tracked, entries, updates, today)),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun GarageScreen(
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long) -> Unit,
    onSeeReminders: () -> Unit,
    vm: GarageViewModel = hiltViewModel(),
) {
    val garage = vm.garage.collectAsStateWithLifecycle().value ?: return // first frame while loading

    // With a single vehicle the list has nothing to choose from, so the app opens on that vehicle.
    // Once only: "All vehicles" still comes back here, which is where a second one gets added.
    var checkedSingle by rememberSaveable { mutableStateOf(false) }
    // Not saved: stays set while this screen animates out, and is fresh again on the way back.
    var leaving by remember { mutableStateOf(false) }
    val skipList = !checkedSingle && garage.vehicles.size == 1
    LaunchedEffect(Unit) {
        if (skipList) {
            leaving = true
            onOpenVehicle(garage.vehicles.single().vehicle.id)
        }
        checkedSingle = true
    }
    if (skipList || leaving) return

    if (garage.vehicles.isEmpty()) {
        EmptyGarage(onAddVehicle)
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(garage, onAddVehicle) }
        items(garage.vehicles, key = { it.vehicle.id }) { v -> VehicleCard(v, onClick = { onOpenVehicle(v.vehicle.id) }) }
        val urgent = garage.urgent
        if (urgent.isNotEmpty()) {
            item { AcrossAll(garage.vehicles.size, urgent, onSeeReminders) }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun Header(garage: Garage, onAddVehicle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Garage",
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(garage.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedIconButton(
            onClick = onAddVehicle,
            modifier = Modifier.size(48.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add vehicle")
        }
    }
}

/** "2 vehicles · 1 overdue" — never-logged reminders aren't counted. */
private fun Garage.subtitle(): String = listOfNotNull(
    if (vehicles.size == 1) "1 vehicle" else "${vehicles.size} vehicles",
    when {
        overdue > 0 -> "$overdue overdue"
        dueSoon > 0 -> "$dueSoon due soon"
        else -> null
    },
).joinToString(" · ")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleCard(g: GarageVehicle, onClick: () -> Unit) {
    val v = g.vehicle
    val scheme = MaterialTheme.colorScheme
    val urgent = g.comingUp.overdue > 0
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = if (urgent) BorderStroke(1.5.dp, scheme.primary) else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${v.year} ${v.nickname ?: v.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        // vPIC drive types read like "4WD/4-Wheel Drive/4x4"; the first part is enough here.
                        listOfNotNull(v.trim, v.drivetrain?.substringBefore('/'), v.engine).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(
                    g.odometer?.asMiles() ?: "No mileage",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (g.odometer == null) scheme.onSurfaceVariant else scheme.onSurface,
                )
            }
            g.comingUp.attention?.let { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AttentionChip(it) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.comingUp.nextLine(), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AttentionChip(a: Attention) {
    val scheme = MaterialTheme.colorScheme
    val (text, container, content) = when (a.state) {
        DueState.OVERDUE -> Triple(a.only?.let { "$it overdue" } ?: "${a.count} overdue", scheme.primary, scheme.onPrimary)
        DueState.DUE_SOON -> Triple(a.only?.let { "$it due soon" } ?: "${a.count} due soon", scheme.surfaceVariant, statusColors().soon)
        DueState.OK -> Triple("Up to date", scheme.tertiaryContainer, scheme.onTertiaryContainer)
    }
    Surface(color = container, shape = CircleShape) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (a.state == DueState.OVERDUE) FontWeight.Bold else FontWeight.SemiBold,
            color = content,
        )
    }
}

/** "Next: Oil & filter in 1,688 mi" */
private fun ComingUp.nextLine(): String {
    next?.let { (status, progress) ->
        return "Next: ${status.item.name} ${progress.headline.text().replaceFirstChar(Char::lowercase)}"
    }
    return when {
        counting.isNotEmpty() -> "Nothing else coming up"
        notLogged.size == 1 -> "1 reminder not logged yet"
        notLogged.isNotEmpty() -> "${notLogged.size} reminders not logged yet"
        else -> "No reminders tracked"
    }
}

@Composable
private fun AcrossAll(vehicleCount: Int, urgent: List<UrgentItem<VehicleEntity>>, onSeeReminders: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (vehicleCount == 2) "ACROSS BOTH" else "ACROSS ALL",
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurfaceVariant,
        )
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                urgent.forEach { item ->
                    val overdue = item.status.status.state == DueState.OVERDUE
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${item.status.item.name} · ${item.vehicle.nickname ?: item.vehicle.model}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            if (overdue) "Overdue" else item.progress.headline.remaining(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (overdue) statusColors().overdue else statusColors().soon,
                        )
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onSeeReminders),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("See all reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.primary)
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = scheme.primary,
                    )
                }
            }
        }
    }
}

/** "640 mi", "12 days" — "In 640 mi" without the "In", for a right-hand column. */
private fun DueHeadline.remaining(): String = text().removePrefix("In ")

@Composable
private fun EmptyGarage(onAddVehicle: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text("No vehicles yet", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Add one with its VIN and we'll fill in the details, the factory maintenance schedule and any recalls.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAddVehicle, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Add a vehicle") }
    }
}

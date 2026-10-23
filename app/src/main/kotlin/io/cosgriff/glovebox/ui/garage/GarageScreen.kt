package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.service.nicknameOrModel
import io.cosgriff.glovebox.ui.components.PrimaryButton
import io.cosgriff.glovebox.ui.components.Pill
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.ahead
import io.cosgriff.glovebox.ui.service.amount
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.domain.Attention
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.UrgentItem
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(overview: GarageOverview, recalls: RecallRepository, vehicles: VehicleDao) : ViewModel() {
    /** How many vehicles are in the archive. */
    val archived: StateFlow<Int> = vehicles.observeArchived().map { it.size }
        .screenState(viewModelScope, 0)

    init {
        // Once per launch: re-check any vehicle whose recall list is a week old or was never fetched.
        viewModelScope.launch { recalls.refreshStale() }
    }

    /** Null while loading. Recomputed whenever a log, schedule or mileage changes. */
    val garage: StateFlow<Garage?> = overview.garage.screenState(viewModelScope, null)
}

@Composable
fun GarageScreen(
    showing: Boolean,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long) -> Unit,
    onSeeReminders: () -> Unit,
    onOpenArchive: () -> Unit,
    onRestore: () -> Unit,
    vm: GarageViewModel = hiltViewModel(),
) {
    val garage = vm.garage.collectAsStateWithLifecycle().value ?: return // first frame while loading

    // With a single vehicle the list has nothing to choose from, so the app opens on that vehicle.
    // Once only: "All vehicles" still comes back here, which is where a second one gets added. Only while
    // the list is what shows: a notification or shortcut that opened something over it on a cold start
    // used the launch up, and Back from there comes to the list.
    var checkedSingle by rememberSaveable { mutableStateOf(false) }
    // Not saved: stays set while this screen animates out, and is fresh again on the way back.
    var leaving by remember { mutableStateOf(false) }
    val skipList = !checkedSingle && showing && garage.vehicles.size == 1
    // Keyed on the decision, not run once: coming back, the list can show its last count for a frame
    // before the new one arrives. An empty garage doesn't use the skip up, so the first vehicle, once
    // added and set up, is opened too.
    LaunchedEffect(skipList) {
        if (skipList) {
            leaving = true
            onOpenVehicle(garage.vehicles.single().vehicle.id)
        }
        if (garage.vehicles.isNotEmpty()) checkedSingle = true
    }
    if (skipList || leaving) return

    val archived by vm.archived.collectAsStateWithLifecycle()
    GarageContent(garage, archived, onAddVehicle, onOpenVehicle, onSeeReminders, onOpenArchive, onRestore)
}

/** The garage as given, with no ViewModel: what [GarageScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun GarageContent(
    garage: Garage,
    archived: Int,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long) -> Unit,
    onSeeReminders: () -> Unit,
    onOpenArchive: () -> Unit,
    onRestore: () -> Unit,
) {
    if (garage.vehicles.isEmpty()) {
        EmptyGarage(onAddVehicle, archived, onOpenArchive, onRestore)
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
        if (archived > 0) {
            item { ArchivedRow(archived, onOpenArchive) }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun Header(garage: Garage, onAddVehicle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.garage_title),
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
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.garage_add_vehicle))
        }
    }
}

/** "2 vehicles · 1 overdue · 1 open recall" — never-logged reminders aren't counted. */
@Composable
private fun Garage.subtitle(): String = listOfNotNull(
    pluralStringResource(R.plurals.garage_vehicles, vehicles.size, vehicles.size.asNumber()),
    when {
        overdue > 0 -> pluralStringResource(R.plurals.count_overdue, overdue, overdue.asNumber())
        dueSoon > 0 -> pluralStringResource(R.plurals.count_due_soon, dueSoon, dueSoon.asNumber())
        else -> null
    },
    openRecalls.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.open_recalls, it, it.asNumber()) },
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = if (urgent) BorderStroke(1.5.dp, scheme.primary) else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(v.shortName(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        // vPIC drive types read like "4WD/4-Wheel Drive/4x4"; the first part is enough here.
                        listOfNotNull(v.trim, v.drivetrain?.substringBefore('/'), v.engine).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(
                    g.odometer?.asDistance(v.unit) ?: stringResource(R.string.garage_no_mileage),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    color = if (g.odometer == null) scheme.onSurfaceVariant else scheme.onSurface,
                )
            }
            if (g.comingUp.attention != null || g.recalls != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    g.comingUp.attention?.let { AttentionChip(it) }
                    g.recalls?.let { RecallChip(it) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.comingUp.nextLine(LocalResources.current, v.unit), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
    val text = a.label(LocalResources.current)
    val (container, content) = when {
        a.toLog > 0 -> scheme.surfaceVariant to statusColors().soon
        a.state == DueState.OVERDUE -> scheme.primary to scheme.onPrimary
        a.state == DueState.DUE_SOON -> scheme.surfaceVariant to statusColors().soon
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    Pill(text, container, content, bold = a.state == DueState.OVERDUE)
}

@Composable
private fun RecallChip(open: OpenRecalls) {
    val scheme = MaterialTheme.colorScheme
    val (text, container, content) = when (open.count) {
        0 -> Triple(stringResource(R.string.no_open_recalls), scheme.tertiaryContainer, scheme.onTertiaryContainer)
        else -> Triple(pluralStringResource(R.plurals.open_recalls, open.count, open.count.asNumber()), scheme.primaryContainer, scheme.onPrimaryContainer)
    }
    Pill(text, container, content, bold = false)
}

@Composable
private fun AcrossAll(vehicleCount: Int, urgent: List<UrgentItem<VehicleEntity>>, onSeeReminders: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(if (vehicleCount == 2) R.string.garage_across_both else R.string.garage_across_all), bold = true)
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
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
                            "${item.status.item.name} · ${item.vehicle.nicknameOrModel()}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            if (overdue) stringResource(R.string.overdue) else item.progress.headline.remaining(item.vehicle.unit),
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
                    Text(stringResource(R.string.garage_see_all_reminders), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.primary)
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
private fun DueHeadline.remaining(unit: DistanceUnit): String = amount(unit)?.takeIf { ahead == true } ?: text(unit)

@Composable
private fun ArchivedRow(count: Int, onClick: () -> Unit) {
    val color = statusColors().soon
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.garage_archived, count.asNumber()), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
    }
}

@Composable
private fun EmptyGarage(onAddVehicle: () -> Unit, archived: Int, onOpenArchive: () -> Unit, onRestore: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.garage_empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.garage_empty_body),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(onClick = onAddVehicle) { Text(stringResource(R.string.garage_empty_add)) }
        // A new phone starts here: this is how the data from the old one comes back.
        TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.welcome_restore)) }
        // With every vehicle sold, this is the only way back to them.
        if (archived > 0) ArchivedRow(archived, onOpenArchive)
    }
}

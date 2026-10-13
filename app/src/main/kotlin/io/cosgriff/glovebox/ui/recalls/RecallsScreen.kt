package io.cosgriff.glovebox.ui.recalls

import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.PlexMono
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.RecallRefresh
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.RecallsRoute
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import io.cosgriff.glovebox.ui.service.NoLinks
import javax.inject.Inject

data class RecallsState(
    val vehicle: VehicleEntity,
    /** Open first, then the most recently reported. */
    val recalls: List<RecallEntity>,
    /** Starting point for the odometer when a recall is marked done. */
    val odometer: Int?,
) {
    val open: Int get() = recalls.count { it.completedOn == null }
    val canCheck: Boolean get() = Recalls.canCheck(vehicle.year, vehicle.make, vehicle.model)
    val makeName: String get() = Recalls.makeName(vehicle.make)
}

/** How a recall is being marked done: on its own, or with a record of the work. */
data class RecallCompletion(val date: LocalDate, val odometer: Int?, val shopName: String?)

@HiltViewModel
class RecallsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    recalls: RecallDao,
    service: ServiceDao,
    odometer: OdometerDao,
    private val repository: RecallRepository,
    private val actions: RecordActions,
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<RecallsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<RecallsState?> = combine(
        vehicles.observe(vehicleId),
        recalls.observe(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
    ) { vehicle, list, records, readings ->
        if (vehicle == null) return@combine null
        val byId = list.associateBy { it.id }
        RecallsState(
            vehicle = vehicle,
            recalls = Recalls.sorted(list.map { it.toStored() }).map { byId.getValue(it.id) },
            odometer = vehicle.clusterSwap().let { swap -> VehicleReminders.currentOdometer(records.map { it.toEntry(swap, NoLinks) }, readings.toReadings(swap)) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var refreshing by mutableStateOf(false)
        private set

    /** The last check didn't get through; the list on screen is the stored one. */
    var failed by mutableStateOf(false)
        private set

    init {
        // A list that is a week old (or was never fetched) is re-checked on the way in.
        check { repository.refreshIfStale(vehicleId) }
    }

    fun refresh() = check { repository.refresh(vehicleId) }

    private fun check(ask: suspend () -> RecallRefresh?) {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            failed = ask() == RecallRefresh.Failed
            refreshing = false
        }
    }

    fun markDone(recall: RecallEntity, completion: RecallCompletion) = viewModelScope.launch {
        val record = completion.odometer?.let {
            ServiceRecordEntity(
                vehicleId = vehicleId,
                date = completion.date,
                odometer = it,
                diy = false,
                shopName = completion.shopName,
            )
        }
        actions.completeRecall(recall.id, completion.date, record)
    }

    fun markOpen(recall: RecallEntity) = viewModelScope.launch { actions.reopenRecall(recall.id) }
}

private enum class RecallTab(val label: String) { ALL("All"), OPEN("Open"), DONE("Completed") }

/** Design: 8 · Recalls. */
@Composable
fun RecallsScreen(onBack: () -> Unit, vm: RecallsViewModel = hiltViewModel()) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val v = state.vehicle
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(RecallTab.ALL) }
    var marking by rememberSaveable { mutableStateOf<Long?>(null) }
    val site = Recalls.manufacturerSite(v.make)

    state.recalls.firstOrNull { it.id == marking }?.let { recall ->
        MarkDoneSheet(
            recall = recall,
            odometer = state.odometer,
            unit = v.unit,
            makeName = state.makeName,
            onConfirm = {
                marking = null
                vm.markDone(recall, it)
            },
            onDismiss = { marking = null },
        )
    }

    val shown = state.recalls.filter {
        when (tab) {
            RecallTab.ALL -> true
            RecallTab.OPEN -> it.completedOn == null
            RecallTab.DONE -> it.completedOn != null
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Recalls", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull("${v.year} ${v.nickname ?: v.model}", v.vin?.let { "VIN …${it.takeLast(6)}" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SummaryCard(
                state = state,
                refreshing = vm.refreshing,
                failed = vm.failed,
                lookupLabel = site?.let { "Verify my VIN with ${it.name}" } ?: "Check my VIN at NHTSA",
                onRefresh = vm::refresh,
                onLookup = { context.openInBrowser(site?.recallUrl ?: Recalls.NHTSA_LOOKUP_URL) },
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
            )
        }

        if (state.recalls.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecallTab.entries.forEach { t ->
                        FilterChip(
                            selected = t == tab,
                            onClick = { tab = t },
                            label = { Text(t.label) },
                            // Canvas: the chosen filter is solid ink.
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (tab == RecallTab.OPEN) "No open recalls." else "Nothing marked done yet.",
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(shown, key = { it.id }) { recall ->
                RecallCard(
                    recall = recall,
                    onToggle = { if (recall.completedOn == null) marking = recall.id else vm.markOpen(recall) },
                    onFindDealer = { context.findDealer(state.makeName, site?.recallUrl ?: Recalls.NHTSA_LOOKUP_URL) },
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                )
            }
        }

        item {
            Row(
                Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 1.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Recall list from NHTSA by year, make and model. NHTSA can't say whether the repair was already done on " +
                        "this VIN: confirm that with ${state.makeName}, then mark it here. Marking one done can add it to Service history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    state: RecallsState,
    refreshing: Boolean,
    failed: Boolean,
    lookupLabel: String,
    onRefresh: () -> Unit,
    onLookup: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val v = state.vehicle
    val checked = v.recallsCheckedOn
    val open = state.open
    // Tinted only once there is an answer: orange needs attention, teal is all clear.
    val (container, accent) = when {
        checked == null -> scheme.surface to scheme.outlineVariant
        open > 0 -> scheme.primaryContainer to scheme.primary
        else -> scheme.tertiaryContainer to scheme.tertiary
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        contentColor = scheme.onSurface, // canvas: ink on the tint, not the tint's own text colour
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (checked == null) accent else accent.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (checked == null) {
                    Text(
                        when {
                            !state.canCheck -> "Recalls can't be checked"
                            refreshing -> "Checking NHTSA…"
                            else -> "Not checked yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("$open", fontFamily = PlexMono, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = accent)
                        Text(
                            when (open) {
                                0 -> "open recalls — all clear"
                                1 -> "open recall needs attention"
                                else -> "open recalls need attention"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    state.summaryLine(refreshing, failed, LocalDate.now()),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors().soon,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.canCheck) {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !refreshing,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, scheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = scheme.surface, contentColor = scheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(if (refreshing) "Checking…" else "Refresh", maxLines = 1)
                    }
                }
                Button(
                    onClick = onLookup,
                    modifier = Modifier.weight(2f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(lookupLabel, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** "2 recalls on file for this vehicle. Checked today." */
private fun RecallsState.summaryLine(refreshing: Boolean, failed: Boolean, today: LocalDate): String {
    val name = "${vehicle.year} $makeName ${vehicle.model}"
    val checked = vehicle.recallsCheckedOn
    if (!canCheck) {
        return "NHTSA lists recalls by year, make and model, and this vehicle is missing one of them. You can still look up its VIN."
    }
    if (checked == null) {
        return when {
            refreshing -> "Asking NHTSA about the $name. Only the year, make and model are sent."
            failed -> "Couldn't reach NHTSA. Check your connection and try again."
            else -> "Refresh to ask NHTSA about the $name. Only the year, make and model are sent."
        }
    }
    val onFile = when (recalls.size) {
        0 -> "NHTSA lists no recalls for the $name."
        1 -> "1 recall on file for this vehicle."
        else -> "${recalls.size} recalls on file for this vehicle."
    }
    val checkedText = when (ChronoUnit.DAYS.between(checked, today)) {
        0L -> "Checked today."
        1L -> "Checked yesterday."
        else -> "Checked ${checked.format(longDate)}."
    }
    return if (failed) "$onFile Couldn't reach NHTSA just now, so this is the list from ${checked.format(longDate)}." else "$onFile $checkedText"
}

@Composable
private fun RecallCard(recall: RecallEntity, onToggle: () -> Unit, onFindDealer: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val done = recall.completedOn != null
    var expanded by rememberSaveable { mutableStateOf(false) }
    // The collapsed card clips long NHTSA text; only offer to expand when something is hidden.
    var clipped by rememberSaveable { mutableStateOf(false) }
    val body = statusColors().soon

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, if (done) scheme.outlineVariant else scheme.primary),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "NHTSA ${Recalls.campaignLabel(recall.campaign)}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    color = body,
                )
                Pill(
                    if (done) "COMPLETED" else "OPEN",
                    container = if (done) scheme.tertiaryContainer else scheme.primaryContainer,
                    content = if (done) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
                )
            }
            Text(Recalls.componentLabel(recall.component), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                recall.summary.ifEmpty { "NHTSA gave no summary for this recall." },
                style = MaterialTheme.typography.bodyMedium,
                color = body,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) clipped = clipped || it.hasVisualOverflow },
            )
            if (expanded) recall.consequence?.let { Labelled("Risk", it) }
            recall.remedy?.let {
                Labelled("Remedy", it, maxLines = if (expanded) Int.MAX_VALUE else 3, onClipped = { if (!expanded) clipped = true })
            }
            Text(
                listOfNotNull(
                    recall.reportedOn?.let { "Reported ${it.format(longDate)}" },
                    recall.completedOn?.let { "marked done ${it.format(longDate)}" },
                ).joinToString(" · ").replaceFirstChar(Char::uppercase),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (clipped || recall.consequence != null) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(if (expanded) "Show less" else "Show the full notice")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onToggle,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, scheme.onSurface),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(if (done) "Mark as open" else "Mark as done", maxLines = 1)
                }
                Button(
                    onClick = onFindDealer,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.surfaceVariant, contentColor = scheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("Find a dealer", maxLines = 1)
                }
            }
        }
    }
}

/** "Remedy: Dealers will replace the fuel pump." */
@Composable
private fun Labelled(label: String, text: String, maxLines: Int = Int.MAX_VALUE, onClipped: () -> Unit = {}) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = statusColors().soon)) { append("$label: ") }
            append(text)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow) onClipped() },
    )
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(color = container, shape = CircleShape) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

/** External pages open in the user's browser; the app itself still talks only to NHTSA. */
private fun Context.openInBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // No browser installed; nothing useful to do.
    }
}

/** Hands "<make> dealer" to the phone's maps app, or the manufacturer's site if there isn't one. */
private fun Context.findDealer(makeName: String, fallbackUrl: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, ("geo:0,0?q=" + Uri.encode("$makeName dealer")).toUri()))
    } catch (_: ActivityNotFoundException) {
        openInBrowser(fallbackUrl)
    }
}

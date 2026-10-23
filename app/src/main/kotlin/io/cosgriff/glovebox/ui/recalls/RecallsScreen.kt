package io.cosgriff.glovebox.ui.recalls

import io.cosgriff.glovebox.ui.screenState
import androidx.compose.runtime.setValue
import io.cosgriff.glovebox.ui.service.highestReading
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.domain.RecallView
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.Pill
import io.cosgriff.glovebox.ui.components.InkChip
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.styledStringResource
import androidx.annotation.StringRes
import io.cosgriff.glovebox.ui.service.recallComponent
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.PlexMono
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
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
import io.cosgriff.glovebox.ui.RecallsRoute
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.Visits
import io.cosgriff.glovebox.ui.history.VisitRow
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.toVisit

data class RecallsState(
    val vehicle: VehicleEntity,
    /** Open first, then the most recently reported. */
    val recalls: List<RecallEntity>,
    /** Starting point for the odometer when a recall is marked done. */
    val odometer: Int?,
    /** The vehicle's service records, for "Add to the visit on …" when a recall is marked done. */
    val records: List<ServiceRecordWithItems> = emptyList(),
) {
    /** The visits, or records on their own, a recall done on [date] can join (#307); none is chosen for the user. */
    fun visitsOn(date: LocalDate): List<VisitRow> {
        val byId = records.associateBy { it.record.id }
        return Visits.onDay(records.map { it.record.toVisit() }, date).map { group ->
            val parts = group.mapNotNull { byId[it.id] }
            val first = parts.first().record
            VisitRow(
                id = first.id,
                work = parts.flatMap { it.items }.joinToString(", ") { it.label },
                about = listOf(
                    first.odometer.asDistance(vehicle.unit),
                    LogEdits.who(first.diy, first.shopName, null, first.enteredAsLastDone, first.imported).text,
                ).joinToString(" · "),
            )
        }
    }

    val open: Int get() = recalls.count { it.toStored().open }
    val canCheck: Boolean get() = Recalls.canCheck(vehicle.year, vehicle.make, vehicle.model)
    val makeName: String get() = Recalls.makeName(vehicle.make)
}

/**
 * How a recall is being marked done: on its own, or with a record of the work. [visitWith] is a record
 * that record joins as one visit, when the user chose "Add to the visit on …".
 */
data class RecallCompletion(val date: LocalDate, val odometer: Int?, val shopName: String?, val visitWith: Long? = null)

@HiltViewModel
class RecallsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    private val recalls: RecallDao,
    service: ServiceDao,
    odometer: OdometerDao,
    private val repository: RecallRepository,
    private val actions: RecordActions,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<RecallsRoute>().vehicleId

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
            odometer = vehicle.highestReading(records, readings),
            records = records,
        )
    }.screenState(viewModelScope, null)

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
        actions.completeRecall(recall.id, completion.date, record, completion.visitWith)
    }

    fun markOpen(recall: RecallEntity) = viewModelScope.launch { actions.reopenRecall(recall.id) }

    /** The user found it doesn't apply to this vehicle. Nothing is deleted; [unarchive] puts it back. */
    fun archive(recall: RecallEntity) = viewModelScope.launch { recalls.setArchived(recall.id, LocalDate.now()) }

    fun unarchive(recall: RecallEntity) = viewModelScope.launch { recalls.setArchived(recall.id, null) }
}

private enum class RecallTab(@StringRes val label: Int, val view: RecallView) {
    ALL(R.string.history_all, RecallView.ALL),
    OPEN(R.string.recalls_tab_open, RecallView.OPEN),
    DONE(R.string.recalls_tab_done, RecallView.DONE),
    ARCHIVED(R.string.recalls_tab_archived, RecallView.ARCHIVED),
}

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
            visitsOn = state::visitsOn,
            onConfirm = {
                marking = null
                vm.markDone(recall, it)
            },
            onDismiss = { marking = null },
        )
    }

    val shown = state.recalls.filter { it.toStored().shownIn(tab.view) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader(stringResource(R.string.details_recalls), onBack, subtitle = listOfNotNull(v.shortName(), v.vin?.let { stringResource(R.string.recalls_vin_end, it.takeLast(6)) }).joinToString(" · "))
        }

        item {
            SummaryCard(
                state = state,
                refreshing = vm.refreshing,
                failed = vm.failed,
                lookupLabel = site?.let { stringResource(R.string.recalls_verify_with, it.name) } ?: stringResource(R.string.recalls_check_nhtsa),
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
                    // Archived only appears once something is.
                    RecallTab.entries.filter { it != RecallTab.ARCHIVED || state.recalls.any { r -> r.archivedOn != null } || tab == it }.forEach { t ->
                        InkChip(stringResource(t.label), selected = t == tab) { tab = t }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        when (tab) {
                            RecallTab.OPEN -> stringResource(R.string.recalls_none_open)
                            RecallTab.ARCHIVED -> stringResource(R.string.recalls_none_archived)
                            else -> stringResource(R.string.recalls_none_done)
                        },
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(shown, key = { it.id }) { recall ->
                RecallCard(
                    recall = recall,
                    onToggle = { if (recall.completedOn == null) marking = recall.id else vm.markOpen(recall) },
                    onArchive = { vm.archive(recall) },
                    onUnarchive = { vm.unarchive(recall) },
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
                    stringResource(R.string.recalls_note, state.makeName),
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
        shape = MaterialTheme.shapes.large,
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
                            !state.canCheck -> stringResource(R.string.recalls_cant_check)
                            refreshing -> stringResource(R.string.recalls_checking_nhtsa)
                            else -> stringResource(R.string.details_recalls_not_checked)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(open.asNumber(), fontFamily = PlexMono, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = accent)
                        Text(
                            if (open == 0) stringResource(R.string.recalls_all_clear) else pluralStringResource(R.plurals.recalls_need_attention, open),
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
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.5.dp, scheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = scheme.surface, contentColor = scheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(stringResource(if (refreshing) R.string.add_vehicle_recalls_checking else R.string.refresh), maxLines = 1)
                    }
                }
                Button(
                    onClick = onLookup,
                    modifier = Modifier.weight(2f).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(lookupLabel, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** "2 recalls on file for this vehicle. Checked today." */
@Composable
private fun RecallsState.summaryLine(refreshing: Boolean, failed: Boolean, today: LocalDate): String {
    val name = "${vehicle.year} $makeName ${vehicle.model}"
    val checked = vehicle.recallsCheckedOn
    if (!canCheck) return stringResource(R.string.recalls_missing_ymm)
    if (checked == null) {
        return when {
            refreshing -> stringResource(R.string.recalls_asking, name)
            failed -> stringResource(R.string.add_vehicle_nhtsa_unreachable)
            else -> stringResource(R.string.recalls_refresh_to_ask, name)
        }
    }
    val onFile = if (recalls.isEmpty()) stringResource(R.string.recalls_none_listed, name) else pluralStringResource(R.plurals.recalls_on_file, recalls.size, recalls.size.asNumber())
    if (failed) return stringResource(R.string.recalls_failed_list_from, onFile, checked.format(longDate))
    val checkedText = when (ChronoUnit.DAYS.between(checked, today)) {
        0L -> stringResource(R.string.recalls_checked_today)
        1L -> stringResource(R.string.recalls_checked_yesterday)
        else -> stringResource(R.string.recalls_checked_on, checked.format(longDate))
    }
    return "$onFile $checkedText"
}

@Composable
private fun RecallCard(
    recall: RecallEntity,
    onToggle: () -> Unit,
    onFindDealer: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val done = recall.completedOn != null
    val archived = !done && recall.archivedOn != null
    var expanded by rememberSaveable { mutableStateOf(false) }
    // The collapsed card clips long NHTSA text; only offer to expand when something is hidden.
    var clipped by rememberSaveable { mutableStateOf(false) }
    val body = statusColors().soon

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, if (done || archived) scheme.outlineVariant else scheme.primary),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.recalls_campaign, Recalls.campaignLabel(recall.campaign)),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    color = body,
                )
                when {
                    done -> Pill(stringResource(R.string.recalls_pill_completed), container = scheme.tertiaryContainer, content = scheme.onTertiaryContainer)
                    archived -> Pill(stringResource(R.string.recalls_pill_archived), container = scheme.surfaceVariant, content = scheme.onSurfaceVariant)
                    else -> Pill(stringResource(R.string.recalls_pill_open), container = scheme.primaryContainer, content = scheme.onPrimaryContainer)
                }
            }
            Text(recallComponent(recall.component), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                recall.summary.ifEmpty { stringResource(R.string.recalls_no_summary) },
                style = MaterialTheme.typography.bodyMedium,
                color = body,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) clipped = clipped || it.hasVisualOverflow },
            )
            if (expanded) recall.consequence?.let { Labelled(stringResource(R.string.recalls_risk), it) }
            recall.remedy?.let {
                Labelled(stringResource(R.string.recalls_remedy), it, maxLines = if (expanded) Int.MAX_VALUE else 3, onClipped = { if (!expanded) clipped = true })
            }
            Text(
                listOfNotNull(
                    recall.reportedOn?.let { stringResource(R.string.recalls_reported, it.format(longDate)) },
                    recall.completedOn?.let { stringResource(R.string.recalls_marked_done, it.format(longDate)) },
                    recall.archivedOn?.takeIf { archived }?.let { stringResource(R.string.recalls_archived_on, it.format(longDate)) },
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
                    Text(stringResource(if (expanded) R.string.recalls_show_less else R.string.recalls_show_full))
                }
            }
            if (archived) {
                InkOutlinedButton(onClick = onUnarchive, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.recalls_move_back), maxLines = 1)
                }
                return@Column
            }
            // What the owner says about the recall, as one connected group: done, or not this vehicle's.
            // Archiving is only for an open recall, so a completed one has the single button.
            // One outlined shape split by a line, so the two read as a group: what the owner says about the recall.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.weight(if (done) 1f else 2f).height(48.dp).clip(MaterialTheme.shapes.small)
                        .border(1.5.dp, scheme.onSurface, MaterialTheme.shapes.small),
                ) {
                    StatusButton(stringResource(if (done) R.string.recalls_mark_open else R.string.recalls_mark_done), onClick = onToggle, modifier = Modifier.weight(1f))
                    if (!done) {
                        Box(Modifier.width(1.5.dp).fillMaxHeight().background(scheme.onSurface))
                        StatusButton(stringResource(R.string.recalls_archive), onClick = onArchive, modifier = Modifier.weight(1f))
                    }
                }
                Button(
                    onClick = onFindDealer,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.surfaceVariant, contentColor = scheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.recalls_find_dealer), maxLines = 1, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!done) {
                Text(
                    stringResource(R.string.recalls_archive_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One half of the recall card's Mark as done / Archive group; the group draws the outline. */
@Composable
private fun StatusButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, maxLines = 1, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** "Remedy: Dealers will replace the fuel pump." */
@Composable
private fun Labelled(label: String, text: String, maxLines: Int = Int.MAX_VALUE, onClipped: () -> Unit = {}) {
    Text(
        styledStringResource(R.string.recalls_labelled, label, SpanStyle(fontWeight = FontWeight.SemiBold, color = statusColors().soon), text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow) onClipped() },
    )
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
        startActivity(Intent(Intent.ACTION_VIEW, ("geo:0,0?q=" + Uri.encode(getString(R.string.recalls_dealer_search, makeName))).toUri()))
    } catch (_: ActivityNotFoundException) {
        openInBrowser(fallbackUrl)
    }
}

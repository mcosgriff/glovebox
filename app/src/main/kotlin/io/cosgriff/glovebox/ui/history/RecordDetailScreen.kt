package io.cosgriff.glovebox.ui.history

import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.photoCount
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.runtime.LaunchedEffect
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.photo.RecordPhotoPicker
import io.cosgriff.glovebox.ui.photo.PhotoThumb
import io.cosgriff.glovebox.ui.photo.PhotoTarget
import io.cosgriff.glovebox.ui.photo.AttachMenu
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.domain.ServiceRecord
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.charges
import io.cosgriff.glovebox.data.db.jobPrices
import io.cosgriff.glovebox.data.db.toModel
import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.data.log.toDraft
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.CostBreakdown
import io.cosgriff.glovebox.domain.JobPrice
import io.cosgriff.glovebox.domain.JobPrices
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.categoryOf
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.nextDueAfter
import io.cosgriff.glovebox.ui.RecordDetailRoute
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.data.log.toEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import io.cosgriff.glovebox.domain.VisitRecord
import io.cosgriff.glovebox.domain.Visits
import io.cosgriff.glovebox.ui.components.Chevron
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.data.db.toVisit
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class WorkLine(
    val label: String,
    val category: String?,
    /** What this entry means for the reminder it's linked to, if any. */
    val reminderNote: String?,
    /** True when [reminderNote] is a live "next due"; false when a later record has superseded it. */
    val current: Boolean,
)

data class RecordDetail(
    val record: ServiceRecordWithItems,
    val vehicleName: String,
    /** What the vehicle's odometer counts in; the record's reading is in it. */
    val unit: DistanceUnit,
    /** The vehicle is archived: its records can be read but not edited or deleted. */
    val readOnly: Boolean,
    val lines: List<WorkLine>,
    /** Reminders that roll back if this record is deleted, already worded for display. */
    val deleteImpact: List<String>,
    /** The other records in this one's visit (#307); empty when it is in none. */
    val visit: List<VisitRow> = emptyList(),
    /** Records that look like the same visit, offered for linking while it is in none. */
    val offers: List<VisitRow> = emptyList(),
    /** What "Same visit as…" lists: the vehicle's other records not in its visit, the same day first. */
    val choices: List<VisitRow> = emptyList(),
) {
    /** [lines] under the jobs they were priced in (#481), in the record's order; a line with no job stands alone. */
    val work: List<WorkJob> get() = workOf(record.toModel(), lines)

    /** Null while nothing is priced: then the record shows no breakdown at all. */
    val breakdown: CostBreakdown? get() = JobPrices.breakdown(record.jobPrices, record.record.costCents, record.charges)

    /** The lines in no priced job, for the breakdown's caption. */
    val unpriced: List<String> get() = work.filter { !it.priced }.flatMap { j -> j.lines.map { it.label } }
}

/**
 * A job as the record shows it: [heading] is the shop's wording, null for a line on its own (or a job whose
 * wording is just its line's name). [priced] when it has labor or parts.
 */
data class WorkJob(
    val heading: String?,
    val laborCents: Long?,
    val partsCents: Long?,
    val lines: List<WorkLine>,
    /** Its parts as the invoice printed them (#502); [partsCents] is then their totals added up. */
    val rows: List<WorkPartRow> = emptyList(),
) {
    val priced: Boolean get() = JobPrice(laborCents, partsCents).priced
    val totalCents: Long? get() = JobPrice(laborCents, partsCents).totalCents
}

/** [lines] are [record]'s lines worded, one for one. */
internal fun workOf(record: ServiceRecord, lines: List<WorkLine>): List<WorkJob> =
    record.work.map { entry ->
        val own = entry.lines.map(lines::get)
        val job = entry.job?.let(record.jobs::get) ?: return@map WorkJob(null, null, null, own)
        val heading = job.wording?.takeIf { own.size > 1 || it != own.single().label }
        val rows = job.rows.map { WorkPartRow(it.wording, it.quantityMilli, it.eachCents, it.totalCents, it.line?.let(lines::getOrNull)?.label) }
        WorkJob(heading, job.laborCents, job.price.partsCents, own, rows)
    }

/** One part row as the record shows it (#502): as typed, with the name of the line it is for, if any. */
data class WorkPartRow(val wording: String?, val quantityMilli: Long?, val eachCents: Long?, val totalCents: Long?, val line: String?)

/** Another record, as the visit section and "Same visit as…" show it. [work] and [about] are worded. */
data class VisitRow(val id: Long, val work: String, val about: String)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val service: ServiceDao,
    logs: VehicleLogs,
    private val actions: RecordActions,
    private val photoPicker: RecordPhotoPicker,
) : ViewModel() {
    val photoProblem: StateFlow<String?> = photoPicker.problem

    private val photoRoom: Int get() = RecordPhotos.room(detail.value?.record?.photos?.size ?: 0)

    /** Added to the record straight away; there is no form to save here. */
    fun takePhoto() = photoPicker.capture(PhotoTarget.Record(recordId), photoRoom)
    fun choosePhotos() = photoPicker.pick(PhotoTarget.Record(recordId), photoRoom)

    private val recordId = savedStateHandle.route<RecordDetailRoute>().recordId

    /** Null while loading and after the record is deleted. The vehicle's log is watched, for its visit and what a delete moves. */
    val detail: StateFlow<RecordDetail?> = service.observeRecord(recordId).flatMapLatest { r ->
        if (r == null) flowOf(null) else logs.observe(r.record.vehicleId).map { log -> log?.let { r to it } }
    }.mapLatest { pair ->
        val (r, log) = pair ?: return@mapLatest null
        val vehicle = log.vehicle
        val all = log.records
        val tracked = log.tracked.associateBy { it.id }
        val unit = vehicle.unit
        val entries = log.entries
        val visits = all.map { it.record.toVisit() }
        val me = r.record.toVisit()
        val byId = all.associateBy { it.record.id }
        fun rows(of: List<VisitRecord>) = of.mapNotNull { byId[it.id]?.visitRow(unit) }
        val entry = r.toEntry(log.swap, log.answering)

        RecordDetail(
            record = r,
            vehicleName = vehicle.shortName(),
            unit = unit,
            readOnly = vehicle.archivedOn != null,
            lines = r.items.map { item ->
                val reminder = item.scheduleItemId?.let(tracked::get)
                val latest = reminder != null && LogEdits.isLatestFor(reminder.id, entry, entries)
                WorkLine(
                    label = item.label,
                    category = ServiceCatalog.categoryOf(item.catalogId)?.label,
                    reminderNote = when {
                        reminder == null -> null
                        latest -> AppText.res.getString(R.string.record_next_due, reminder.interval.nextDueAfter(LastDone(entry.date, entry.odometer)).text(unit))
                        else -> AppText.res.getString(R.string.record_done_again)
                    },
                    current = latest,
                )
            },
            deleteImpact = log.whatIf(all.filter { it.record.id != recordId }.map { it.toDraft() }).changes.map { it.text(unit) },
            visit = rows(Visits.others(me, visits)),
            offers = rows(Visits.offers(me, visits)),
            choices = rows(Visits.choices(me, visits)),
        )
    }.screenState(viewModelScope, null)

    fun link(otherId: Long) = viewModelScope.launch { actions.link(recordId, otherId) }
    fun unlink() = viewModelScope.launch { actions.unlink(recordId) }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        val r = detail.value?.record ?: return@launch
        actions.delete(r)
        onDeleted()
    }
}

/** "Oil & filter change, Tire rotation" and "May 4, 2026 · 60,012 mi · Lex Brodie's · imported". */
internal fun ServiceRecordWithItems.visitRow(unit: DistanceUnit) = VisitRow(
    id = record.id,
    work = items.joinToString(", ") { it.label },
    about = listOf(
        record.date.format(longDate),
        record.odometer.asDistance(unit),
        LogEdits.who(record.diy, record.shopName, null, record.enteredAsLastDone, record.imported).text,
    ).joinToString(" · "),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    onBack: () -> Unit,
    /** The record is gone; this screen has nothing left to show. */
    onDeleted: () -> Unit,
    /** Asked for from the edit form's "Delete this record". */
    confirmDelete: Boolean,
    onConfirmDeleteShown: () -> Unit,
    onEdit: (vehicleId: Long, recordId: Long) -> Unit,
    onOpenPhoto: (recordId: Long, index: Int) -> Unit,
    onOpenRecord: (recordId: Long) -> Unit,
    vm: RecordDetailViewModel = hiltViewModel(),
) {
    val detail = vm.detail.collectAsStateWithLifecycle().value ?: return
    val record = detail.record.record
    var confirmingDelete by remember { mutableStateOf(false) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete && !detail.readOnly) {
            confirmingDelete = true
            onConfirmDeleteShown()
        }
    }

    val problem by vm.photoProblem.collectAsStateWithLifecycle()
    var choosingVisit by remember { mutableStateOf(false) }
    RecordDetailContent(
        detail, problem, onBack, onEdit, onOpenPhoto,
        onTakePhoto = vm::takePhoto, onChoosePhotos = vm::choosePhotos, onDelete = { confirmingDelete = true },
        onOpenRecord = onOpenRecord, onLink = { vm.link(it) }, onUnlink = { vm.unlink() }, onChooseVisit = { choosingVisit = true },
    )

    if (choosingVisit) {
        SameVisitSheet(detail.choices, onDismiss = { choosingVisit = false }, onPick = { id ->
            choosingVisit = false
            vm.link(id)
        })
    }

    if (confirmingDelete) {
        GloveboxSheet(onDismiss = { confirmingDelete = false }) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.record_delete_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val n = detail.lines.size
                    Text(
                        listOfNotNull(
                            record.date.format(longDate),
                            record.odometer.asDistance(detail.unit),
                            pluralStringResource(R.plurals.log_items, n, n.asNumber()),
                            photoCount(detail.record.photos.size),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (detail.record.photos.isNotEmpty()) {
                    Text(stringResource(R.string.record_photos_go_too), style = MaterialTheme.typography.bodyMedium)
                }
                if (detail.deleteImpact.isEmpty()) {
                    Text(stringResource(R.string.record_no_change), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.record_go_back_to),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            detail.deleteImpact.forEach {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.record_undo_for, LogEdits.UNDO_SECONDS.asNumber()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { confirmingDelete = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = {
                            confirmingDelete = false
                            vm.delete(onDeleted)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = PlexMono)
    }
}

/** A service record as given, with no ViewModel: what [RecordDetailScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun RecordDetailContent(
    detail: RecordDetail,
    photoProblem: String?,
    onBack: () -> Unit,
    onEdit: (vehicleId: Long, recordId: Long) -> Unit,
    onOpenPhoto: (recordId: Long, index: Int) -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhotos: () -> Unit,
    onDelete: () -> Unit,
    onOpenRecord: (recordId: Long) -> Unit = {},
    onLink: (otherId: Long) -> Unit = {},
    onUnlink: () -> Unit = {},
    onChooseVisit: () -> Unit = {},
) {
    val record = detail.record.record
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(stringResource(R.string.record_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (!detail.readOnly) {
                OutlinedButton(onClick = { onEdit(record.vehicleId, record.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.edit))
                }
            }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilledCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${detail.vehicleName} · ${LogEdits.who(record.diy, record.shopName, null, record.enteredAsLastDone, record.imported).text}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(record.date.format(longDate), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row {
                        Stat(stringResource(R.string.odometer), record.odometer.asDistance(detail.unit), Modifier.weight(1f))
                        Stat(stringResource(R.string.log_total_cost), record.costCents?.let(Money::format) ?: stringResource(R.string.record_not_entered), Modifier.weight(1f))
                    }
                }
            }

            VisitSection(detail, onOpenRecord, onLink, onUnlink, onChooseVisit)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val n = detail.lines.size
                SectionLabel(pluralStringResource(R.plurals.record_work_done, n, n.asNumber()))
                FilledCard {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        detail.work.forEachIndexed { i, job ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            WorkJobRow(job)
                        }
                    }
                }
            }

            detail.breakdown?.let { CostBreakdownCard(it, detail.unpriced) }

            record.notes?.let { notes ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.record_notes))
                    FilledCard { Text(notes, Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
                }
            }

            val photos = detail.record.photos
            // An archived vehicle's record shows its photos but can't take more.
            if (photos.isNotEmpty() || !detail.readOnly) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.record_photos))
                    val canAdd = !detail.readOnly && photos.size < RecordPhotos.MAX_PER_RECORD
                    // Three to a row, as on the canvas; the Add tile takes the next free place.
                    (photos.indices.toList() + listOfNotNull((-1).takeIf { canAdd })).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { i ->
                                if (i >= 0) {
                                    PhotoThumb(
                                        photos[i].fileName,
                                        stringResource(R.string.photo_of, (i + 1).asNumber(), photos.size.asNumber()),
                                        Modifier.weight(1f).aspectRatio(1f),
                                        onClick = { onOpenPhoto(record.id, i) },
                                    )
                                } else {
                                    AttachMenu(onTake = onTakePhoto, onChoose = onChoosePhotos, modifier = Modifier.weight(1f).aspectRatio(1f)) { open ->
                                        OutlinedButton(
                                            onClick = open,
                                            modifier = Modifier.fillMaxSize(),
                                            shape = MaterialTheme.shapes.small,
                                            contentPadding = PaddingValues(4.dp),
                                        ) { Text(stringResource(R.string.record_add_photo)) }
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    val problem = photoProblem
                    Text(
                        problem ?: stringResource(R.string.record_photos_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // With no visit or offer above, linking is still one tap away, at the foot of the page.
            if (!detail.readOnly && detail.visit.isEmpty() && detail.offers.isEmpty() && detail.choices.isNotEmpty()) {
                InkOutlinedButton(onClick = onChooseVisit, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.record_visit_choose))
                }
            }

            if (detail.readOnly) {
                Text(
                    stringResource(R.string.record_archived),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.record_delete)) }
            }
        }
    }
}

/**
 * The other records in this one's visit, each opening that record, with Unlink; or, while it is in none,
 * the records that look like the same visit, each with Link. Nothing is linked without a tap.
 */
@Composable
private fun VisitSection(
    detail: RecordDetail,
    onOpenRecord: (Long) -> Unit,
    onLink: (Long) -> Unit,
    onUnlink: () -> Unit,
    onChooseVisit: () -> Unit,
) {
    when {
        detail.visit.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.record_visit_title))
            FilledCard {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    detail.visit.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        VisitRowItem(row, onClick = { onOpenRecord(row.id) }) { Chevron() }
                    }
                }
            }
            Text(stringResource(R.string.record_visit_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!detail.readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (detail.choices.isNotEmpty()) {
                        InkOutlinedButton(onClick = onChooseVisit, modifier = Modifier.weight(1f).heightIn(min = 48.dp), quiet = true) {
                            Text(stringResource(R.string.record_visit_add))
                        }
                    }
                    InkOutlinedButton(onClick = onUnlink, modifier = Modifier.weight(1f).heightIn(min = 48.dp), quiet = true) {
                        Text(stringResource(R.string.record_visit_unlink))
                    }
                }
            }
        }
        detail.offers.isNotEmpty() && !detail.readOnly -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.record_visit_offer_title))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        pluralStringResource(R.plurals.record_visit_offer, detail.offers.size, detail.offers.size.asNumber()),
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    detail.offers.forEach { row ->
                        VisitRowItem(row, onClick = { onOpenRecord(row.id) }) {
                            OutlinedButton(onClick = { onLink(row.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.record_visit_link))
                            }
                        }
                    }
                }
            }
            if (detail.choices.size > detail.offers.size) {
                TextButton(onClick = onChooseVisit, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.record_visit_choose)) }
            }
        }
    }
}

/** One other record: its work, then when, the reading and who did it. [trailing] is a chevron or a Link button. */
@Composable
private fun VisitRowItem(row: VisitRow, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Ink even on the offer's tint, where the card's accent would make it read as a link.
            Text(row.work, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(row.about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** "Same visit as…": the vehicle's other records, the same day first. A tap links them; nothing in either changes. */
@Composable
private fun SameVisitSheet(choices: List<VisitRow>, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    GloveboxSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.record_visit_choose), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.record_visit_choose_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.navigationBarsPadding().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)) {
            itemsIndexed(choices, key = { _, row -> row.id }) { i, row ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                VisitRowItem(row, onClick = { onPick(row.id) }) {}
            }
        }
    }
}

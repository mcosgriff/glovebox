package io.cosgriff.glovebox.ui.history

import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.components.SheetHandle
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalBottomSheet
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
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.LastDone
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
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.cosgriff.glovebox.ui.service.answering
import javax.inject.Inject

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
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    private val service: ServiceDao,
    settings: SettingsRepository,
    private val actions: RecordActions,
    private val photoPicker: RecordPhotoPicker,
) : ViewModel() {
    val photoProblem: StateFlow<String?> = photoPicker.problem

    private val photoRoom: Int get() = RecordPhotos.room(detail.value?.record?.photos?.size ?: 0)

    /** Added to the record straight away; there is no form to save here. */
    fun takePhoto() = photoPicker.capture(PhotoTarget.Record(recordId), photoRoom)
    fun choosePhotos() = photoPicker.pick(PhotoTarget.Record(recordId), photoRoom)

    private val recordId = savedStateHandle.route<RecordDetailRoute>().recordId

    /** Null while loading and after the record is deleted. */
    val detail: StateFlow<RecordDetail?> = service.observeRecord(recordId).mapLatest { r ->
        if (r == null) return@mapLatest null
        val vehicle = vehicles.observe(r.record.vehicleId).first()
        val items = schedule.allFor(r.record.vehicleId)
        val tracked = items
            .filter { it.tracked }
            .mapNotNull { it.toTracked(settings.settings.first().defaultRule, vehicle?.clusterSwap()) }
            .associateBy { it.id }
        val swap = vehicle?.clusterSwap()
        val unit = vehicle?.unit ?: DistanceUnit.MILES
        val entries = service.recordsFor(r.record.vehicleId).map { it.toEntry(swap, items.answering()) }
        val entry = r.toEntry(swap, items.answering())

        RecordDetail(
            record = r,
            vehicleName = vehicle?.let { "${it.year} ${it.nickname ?: it.model}" }.orEmpty(),
            unit = unit,
            readOnly = vehicle?.archivedOn != null,
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
            deleteImpact = LogEdits.reminderChanges(tracked.values.toList(), entries, entries.filter { it.id != entry.id })
                .map { it.text(unit) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        val r = detail.value?.record ?: return@launch
        actions.delete(r)
        onDeleted()
    }
}

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
    RecordDetailContent(
        detail, problem, onBack, onEdit, onOpenPhoto,
        onTakePhoto = vm::takePhoto, onChoosePhotos = vm::choosePhotos, onDelete = { confirmingDelete = true },
    )

    if (confirmingDelete) {

        ModalBottomSheet(onDismissRequest = { confirmingDelete = false }, dragHandle = { SheetHandle() }) {
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
                        shape = RoundedCornerShape(16.dp),
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val n = detail.lines.size
                SectionLabel(pluralStringResource(R.plurals.record_work_done, n, n.asNumber()))
                FilledCard {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        detail.lines.forEachIndexed { i, line ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(line.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        line.category ?: stringResource(R.string.log_custom),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                line.reminderNote?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (line.current) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                                            shape = RoundedCornerShape(12.dp),
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

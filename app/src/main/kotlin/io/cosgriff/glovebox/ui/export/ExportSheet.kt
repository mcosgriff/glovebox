package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.ui.components.CheckRow
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.domain.ExportText
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.recallComponent
import io.cosgriff.glovebox.ui.service.photoCount
import io.cosgriff.glovebox.ui.service.wordedLines
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.whatItTakes
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ExportDates
import io.cosgriff.glovebox.domain.ExportDocument
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportScheduleLine
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.Exports
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.LicensePlates
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.ui.components.DatePickerSheet
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.service.toTracked
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import io.cosgriff.glovebox.ui.service.answering
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.domain.ExportPaper
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.ui.vehicle.toStored
import javax.inject.Inject

/** A vehicle that can be exported, with everything about it read once when the sheet opens. */
data class Exportable(val id: Long, val name: String, val archived: Boolean, val vehicle: ExportVehicle)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val vehicles: VehicleDao,
    private val service: ServiceDao,
    private val schedule: ScheduleDao,
    private val recalls: RecallDao,
    private val odometer: OdometerDao,
    private val documents: DocumentDao,
    private val settings: SettingsRepository,
    private val photos: RecordPhotoStore,
    private val saver: ExportSaver,
) : ViewModel() {
    /** Null while loading. Garage vehicles first, then archived ones. */
    var all by mutableStateOf<List<Exportable>?>(null)
        private set

    /** The vehicle to export; null is all of them. */
    var vehicleId by mutableStateOf<Long?>(null)
    var options by mutableStateOf(ExportOptions())
    var working by mutableStateOf(false)
        private set

    private val today: LocalDate get() = LocalDate.now()

    /** What would be written with the choices as they stand. */
    val document: ExportDocument?
        get() = all?.let { list -> Exports.document(list.filter { vehicleId == null || it.id == vehicleId }.map { it.vehicle }, options, today) }

    val fileName: String
        get() = Exports.fileName(all?.firstOrNull { it.id == vehicleId }?.name, today, options.format)

    /** Called each time the sheet opens: reads the data afresh and starts from the given vehicle and format. */
    fun open(startVehicleId: Long?, format: ExportFormat) {
        vehicleId = startVehicleId
        options = ExportOptions(format = format)
        viewModelScope.launch {
            val rule = settings.settings.first().defaultRule
            val list = vehicles.all().map { v -> Exportable(v.id, v.nickname ?: v.model, v.archivedOn != null, exportVehicle(v, rule)) }
            all = list
            if (list.none { it.id == vehicleId }) vehicleId = list.firstOrNull()?.id
        }
    }

    private suspend fun exportVehicle(v: VehicleEntity, rule: io.cosgriff.glovebox.domain.DueRule): ExportVehicle {
        val records = service.recordsFor(v.id)
        val swap = v.clusterSwap()
        val items = schedule.allFor(v.id)
        val entries = records.map { it.toEntry(swap, items.answering()) }
        val readings = odometer.readingsFor(v.id).toReadings(swap)
        val stored = recalls.recallsFor(v.id).associateBy { it.id }
        return ExportVehicle(
            title = VehicleText.title(v.year, v.make, v.model, v.trim) ?: AppText.res.getString(R.string.unknown_vehicle),
            nickname = v.nickname,
            vin = v.vin,
            plate = LicensePlates.label(v.plate, v.plateRegion),
            unit = v.unit,
            odometer = VehicleReminders.currentOdometer(entries, readings),
            soldOn = v.archivedOn,
            clusterSwap = swap,
            whatItTakes = v.whatItTakes().wordedLines(),
            documents = VehicleDocuments.sorted(documents.documentsFor(v.id).map { it.toStored() }).map { ExportPaper(it.type.label, it.name, it.date) },
            records = records.map { r ->
                ExportRecord(
                    date = r.record.date,
                    odometer = r.record.odometer,
                    items = r.items.map { it.label },
                    doneBy = if (r.record.diy) AppText.res.getString(R.string.who_diy) else r.record.shopName ?: AppText.res.getString(R.string.export_a_shop),
                    costCents = r.record.costCents,
                    notes = r.record.notes,
                    photoFiles = r.photos.map { it.fileName },
                    enteredAsLastDone = r.record.enteredAsLastDone,
                    imported = r.record.imported,
                )
            },
            schedule = items.filter { it.tracked }.mapNotNull { item ->
                val tracked = item.toTracked(rule, swap) ?: return@mapNotNull null
                // As the odometer read that day, to match the record it comes from.
                ExportScheduleLine(item.name, tracked.interval.text(v.unit, item.firstMiles), ServiceLog.lastDone(item.id, entries)?.let { swap.asReadOn(it) })
            },
            recalls = Recalls.sorted(stored.values.map { it.toStored() }).map { r ->
                val row = stored.getValue(r.id)
                ExportRecall(Recalls.campaignLabel(r.notice.campaign), recallComponent(r.notice.component), row.completedOn, row.archivedOn)
            },
        )
    }

    fun change(newOptions: ExportOptions) {
        options = newOptions
    }

    fun chooseVehicle(id: Long?) {
        vehicleId = id
    }

    /** Writes the export to a working file, then hands it to Android's "save as". */
    fun export(onStarted: () -> Unit) {
        val doc = document ?: return
        if (working) return
        working = true
        val name = fileName
        viewModelScope.launch {
            val file = saver.newFile()
            withContext(Dispatchers.IO) {
                file.outputStream().use { out ->
                    when (doc.options.format) {
                        ExportFormat.MARKDOWN -> out.write(ExportText(AppExportWords).markdown(doc).toByteArray())
                        ExportFormat.PDF -> PdfExport { photos.loadWithin(it, RecordPhotos.PDF_EDGE) }.write(doc, out)
                    }
                }
            }
            working = false
            saver.save(file, name, doc.options.format)
            onStarted()
        }
    }
}

/** Design: 6b · Export sheet. [vehicleId] null starts on the first vehicle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(vehicleId: Long?, onDismiss: () -> Unit, format: ExportFormat = ExportFormat.PDF, vm: ExportViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(vehicleId, format) { vm.open(vehicleId, format) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = the "from" date, false = "to"
    val today = LocalDate.now()
    val o = vm.options

    picking?.let { from ->
        val custom = o.dates as? ExportDates.Custom ?: ExportDates.Custom(today.withDayOfYear(1), today)
        DatePickerSheet(
            initial = if (from) custom.from else custom.to,
            onPick = { picked ->
                // Keep the range the right way round whichever end was moved.
                vm.change(o.copy(dates = if (from) custom.copy(from = picked, to = maxOf(picked, custom.to)) else custom.copy(from = minOf(picked, custom.from), to = picked)))
                picking = null
            },
            onDismiss = { picking = null },
        )
    }

    GloveboxSheet(onDismiss = onDismiss) {
        val all = vm.all
        val doc = vm.document
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.settings_export), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (all == null || doc == null) {
                CircularProgressIndicator(Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally))
                return@Column
            }
            if (all.isEmpty()) {
                Text(stringResource(R.string.export_no_vehicle), color = scheme.onSurfaceVariant)
                return@Column
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(ExportFormat.PDF to stringResource(R.string.export_pdf), ExportFormat.MARKDOWN to stringResource(R.string.export_markdown)).forEachIndexed { i, (f, label) ->
                    SegmentedButton(
                        selected = o.format == f,
                        onClick = { vm.change(o.copy(format = f)) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = scheme.secondary, activeContentColor = scheme.onSecondary),
                        icon = {},
                    ) { Text(label) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Dropdown(
                    label = stringResource(R.string.details_vehicle),
                    value = all.firstOrNull { it.id == vm.vehicleId }?.label() ?: stringResource(R.string.home_all_vehicles),
                    options = all.map { it.label() to { vm.chooseVehicle(it.id) } } +
                        listOfNotNull((stringResource(R.string.home_all_vehicles) to { vm.chooseVehicle(null) }).takeIf { all.size > 1 }),
                    modifier = Modifier.weight(1f),
                )
                Dropdown(
                    label = stringResource(R.string.export_dates),
                    value = when (o.dates) {
                        ExportDates.AllTime -> stringResource(R.string.export_all_time)
                        ExportDates.ThisYear -> stringResource(R.string.export_this_year)
                        is ExportDates.Custom -> stringResource(R.string.export_custom)
                    },
                    options = listOf(
                        stringResource(R.string.export_all_time) to { vm.change(o.copy(dates = ExportDates.AllTime)) },
                        stringResource(R.string.export_this_year) to { vm.change(o.copy(dates = ExportDates.ThisYear)) },
                        stringResource(R.string.export_custom_pick) to { vm.change(o.copy(dates = o.dates as? ExportDates.Custom ?: ExportDates.Custom(today.withDayOfYear(1), today))) },
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            (o.dates as? ExportDates.Custom)?.let { range ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { picking = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.export_from, range.from.format(longDate)), color = scheme.onSurface)
                    }
                    OutlinedButton(onClick = { picking = false }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.export_to, range.to.format(longDate)), color = scheme.onSurface)
                    }
                }
            }

            RowsCard {
                Column {
                    CheckRow(stringResource(R.string.export_costs), o.costsAndNotes) { vm.change(o.copy(costsAndNotes = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    CheckRow(stringResource(R.string.export_schedule), o.scheduleAndRecalls) { vm.change(o.copy(scheduleAndRecalls = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    CheckRow(stringResource(R.string.export_takes), o.whatItTakes) { vm.change(o.copy(whatItTakes = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    CheckRow(stringResource(R.string.export_documents), o.documents) { vm.change(o.copy(documents = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    val pdf = o.format == ExportFormat.PDF
                    // The choice is remembered while Markdown is selected, so switching back to PDF restores it.
                    CheckRow(stringResource(if (pdf) R.string.export_photos else R.string.export_photos_pdf_only), o.photos && pdf, enabled = pdf) { vm.change(o.copy(photos = it)) }
                }
            }

            val photoCount = doc.photoFiles.size
            if (photoCount > 0) {
                val megabytes = RecordPhotos.pdfBytes(photoCount) / 1_000_000.0
                Text(
                    stringResource(
                        if (megabytes >= RecordPhotos.LARGE_EXPORT_MB) R.string.export_photos_large else R.string.export_photos_size,
                        photoCount(photoCount).orEmpty(),
                        megabytes.megabytesText(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (megabytes >= RecordPhotos.LARGE_EXPORT_MB) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                Text(vm.fileName, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono)
            }
            Text(
                stringResource(R.string.export_not_encrypted),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimaryContainer,
            )

            val n = doc.recordCount
            val hasContent = n > 0 || doc.vehicles.any { it.schedule.isNotEmpty() || it.recalls.isNotEmpty() || it.whatItTakes.isNotEmpty() || it.documents.isNotEmpty() }
            Button(
                onClick = { vm.export(onStarted = onDismiss) },
                enabled = hasContent && !vm.working,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(
                    when {
                        vm.working -> stringResource(R.string.export_preparing)
                        else -> stringResource(R.string.export_button, AppExportWords.records(n))
                    },
                )
            }
            if (!hasContent) {
                Text(stringResource(R.string.export_nothing), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

private fun Exportable.label(): String = if (archived) AppText.res.getString(R.string.export_vehicle_sold, name) else name

private fun Double.megabytesText(): String =
    if (this < 1) AppText.res.getString(R.string.export_under_mb) else AppText.res.getString(R.string.size_mb, toInt().coerceAtLeast(1).asNumber())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(label: String, value: String, options: List<Pair<String, () -> Unit>>, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, pick) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        pick()
                        open = false
                    },
                )
            }
        }
    }
}

package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.data.log.VehicleLog
import io.cosgriff.glovebox.data.log.VehicleLogs
import androidx.compose.runtime.setValue
import io.cosgriff.glovebox.ui.service.nicknameOrModel
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.ExportDates
import io.cosgriff.glovebox.domain.ExportDocument
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportJob
import io.cosgriff.glovebox.domain.ExportPartRow
import io.cosgriff.glovebox.data.db.charges
import io.cosgriff.glovebox.data.db.priceOf
import io.cosgriff.glovebox.data.db.rowsOf
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportScheduleLine
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.Exports
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.LicensePlates
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.ui.components.DatePickerSheet
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.data.log.clusterSwap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.domain.ExportPaper
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.ui.vehicle.toStored
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.ReportFacts
import io.cosgriff.glovebox.ui.components.ZoomableImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

/** A vehicle that can be exported, with everything about it read once when the sheet opens. */
data class Exportable(val id: Long, val name: String, val archived: Boolean, val vehicle: ExportVehicle)

/** What the sheet shows of the file before it is saved: PDF page thumbnails, or the Markdown's first lines. */
sealed interface ExportPreview {
    /** [pages] are the first two pages at thumbnail size; [pageCount] counts photo pages too. */
    class Pdf(val pages: List<ImageBitmap>, val pageCount: Int) : ExportPreview

    class Markdown(val firstLines: String) : ExportPreview
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val vehicles: VehicleDao,
    private val logs: VehicleLogs,
    private val recalls: RecallDao,
    private val documents: DocumentDao,
    private val photos: RecordPhotoStore,
    private val saver: ExportSaver,
    fonts: ReportFonts,
) : ViewModel() {
    private val renderer = ReportRenderer(fonts)

    /** Null while loading. Garage vehicles first, then archived ones. */
    var all by mutableStateOf<List<Exportable>?>(null)
        private set

    /** The vehicle to export; null is all of them. */
    var vehicleId by mutableStateOf<Long?>(null)
    var options by mutableStateOf(ExportOptions())
    var working by mutableStateOf(false)
        private set

    /** Null until the first one is ready; then the last one made, kept on screen while the next is drawn. */
    var preview by mutableStateOf<ExportPreview?>(null)
        private set

    private val today: LocalDate get() = LocalDate.now()

    /** What would be written with the choices as they stand. */
    val document: ExportDocument?
        get() = all?.let { list -> Exports.document(list.filter { vehicleId == null || it.id == vehicleId }.map { it.vehicle }, options, today) }

    val fileName: String
        get() = Exports.fileName(all?.firstOrNull { it.id == vehicleId }?.name, today, options.format)

    init {
        // Redrawn off the main thread once the choices settle, so ticking through the options stays smooth.
        viewModelScope.launch {
            snapshotFlow { document }
                .filterNotNull()
                .debounce(PREVIEW_DELAY_MS)
                .collectLatest { doc -> preview = withContext(Dispatchers.Default) { preview(doc) } }
        }
    }

    private fun preview(doc: ExportDocument): ExportPreview = when (doc.options.format) {
        ExportFormat.MARKDOWN -> ExportPreview.Markdown(ExportText(AppExportWords).markdown(doc).lines().take(PREVIEW_LINES).joinToString("\n"))
        ExportFormat.PDF -> {
            val pages = renderer.layout(doc)
            ExportPreview.Pdf(pages.take(2).mapIndexed { i, page -> renderer.bitmap(page, i + 1, pages.size, THUMBNAIL_PX).asImageBitmap() }, pages.size)
        }
    }

    /** Page [index] of the PDF as it stands, large enough to zoom into. A photo page shows only its caption. */
    suspend fun page(index: Int): ImageBitmap? {
        val doc = document ?: return null
        return withContext(Dispatchers.Default) {
            val pages = renderer.layout(doc)
            pages.getOrNull(index)?.let { renderer.bitmap(it, index + 1, pages.size, FULL_PX).asImageBitmap() }
        }
    }

    /** Called each time the sheet opens: reads the data afresh and starts from the given vehicle and format. */
    fun open(startVehicleId: Long?, format: ExportFormat) {
        vehicleId = startVehicleId
        options = ExportOptions(format = format)
        viewModelScope.launch {
            // A vehicle deleted meanwhile is left out.
            val list = vehicles.all().mapNotNull { v -> logs.load(v.id)?.let { Exportable(v.id, v.nicknameOrModel(), v.archivedOn != null, exportVehicle(it)) } }
            all = list
            if (list.none { it.id == vehicleId }) vehicleId = list.firstOrNull()?.id
        }
    }

    private suspend fun exportVehicle(log: VehicleLog): ExportVehicle {
        val v = log.vehicle
        val records = log.records
        val items = log.items
        val swap = log.swap
        val stored = recalls.recallsFor(v.id).associateBy { it.id }
        val categories = items.associate { it.id to ServiceGroup.ofItem(it.templateKey, it.service) }
        return ExportVehicle(
            title = VehicleText.title(v.year, v.make, v.model, v.trim) ?: AppText.res.getString(R.string.unknown_vehicle),
            nickname = v.nickname,
            vin = v.vin,
            plate = LicensePlates.label(v.plate, v.plateRegion),
            unit = v.unit,
            odometer = log.odometer,
            soldOn = v.archivedOn,
            clusterSwap = swap,
            whatItTakes = v.whatItTakes().wordedLines(),
            documents = VehicleDocuments.sorted(documents.documentsFor(v.id).map { it.toStored() }).map { ExportPaper(it.type.label, it.name, it.date, it.expiresOn) },
            records = records.map { r ->
                val jobs = r.jobs.sortedBy { it.position }
                val jobIndex = jobs.withIndex().associate { (i, j) -> j.id to i }
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
                    visitId = r.record.visitId,
                    diy = r.record.diy,
                    categories = r.items.map { line ->
                        line.catalogId?.let { ServiceGroup.ofLine(it) } ?: line.scheduleItemId?.let(categories::get) ?: ServiceGroup.OTHER
                    },
                    jobs = jobs.map { job ->
                        val labels = r.items.associate { it.id to it.label }
                        ExportJob(
                            job.wording, job.laborCents, r.priceOf(job).partsCents,
                            r.rowsOf(job).map { ExportPartRow(it.wording, it.quantityMilli, it.eachCents, it.totalCents, it.itemId?.let(labels::get)) },
                        )
                    },
                    jobOf = if (jobs.isEmpty()) emptyList() else r.items.map { line -> line.jobId?.let(jobIndex::get) },
                    charges = r.charges,
                )
            },
            schedule = items.filter { it.tracked }.mapNotNull { item ->
                val tracked = log.tracked(item.id) ?: return@mapNotNull null
                // As the odometer read that day, to match the record it comes from.
                ExportScheduleLine(item.name, tracked.text(v.unit), ServiceLog.lastDone(item.id, log.entries)?.let { swap.asReadOn(it) }, item.id)
            },
            recalls = Recalls.sorted(stored.values.map { it.toStored() }).map { r ->
                val row = stored.getValue(r.id)
                ExportRecall(Recalls.campaignLabel(r.notice.campaign), recallComponent(r.notice.component), row.completedOn, row.archivedOn)
            },
            specs = listOfNotNull(v.drivetrain?.substringBefore('/'), v.engine).joinToString(" · ").ifEmpty { null },
            report = ReportFacts(log.statuses(today), categories, ServiceLog.readings(log.entries, log.updates)),
        )
    }

    fun change(newOptions: ExportOptions) {
        options = newOptions
    }

    fun chooseVehicle(id: Long?) {
        vehicleId = id
    }

    /**
     * Asks Android where to save the export, then writes it straight there: nothing is kept in the app, and photos
     * are decrypted into memory one page at a time.
     */
    fun export(onStarted: () -> Unit) {
        val doc = document ?: return
        if (working) return
        saver.saveExport(fileName, doc.options.format) { out ->
            when (doc.options.format) {
                ExportFormat.MARKDOWN -> out.write(ExportText(AppExportWords).markdown(doc).toByteArray())
                ExportFormat.PDF -> renderer.writePdf(doc, out) { photos.loadWithin(it, RecordPhotos.PDF_EDGE) }
            }
        }
        onStarted()
    }

    private companion object {
        const val PREVIEW_DELAY_MS = 250L
        const val PREVIEW_LINES = 14

        /** Thumbnails are 135 dp wide; this is sharp up to a density of three. */
        const val THUMBNAIL_PX = 420

        /** Twice the design's size, to zoom into. */
        const val FULL_PX = 1632
    }
}

/** Design: 6b · Export sheet. [vehicleId] null starts on the first vehicle. */
@Composable
fun ExportSheet(vehicleId: Long?, onDismiss: () -> Unit, format: ExportFormat = ExportFormat.PDF, vm: ExportViewModel = hiltViewModel()) {
    LaunchedEffect(vehicleId, format) { vm.open(vehicleId, format) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = the "from" date, false = "to"
    var viewing by remember { mutableStateOf<Int?>(null) }
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
    viewing?.let { start ->
        val count = (vm.preview as? ExportPreview.Pdf)?.pageCount ?: 0
        if (count > 0) PageViewer(start, count, load = vm::page, onDismiss = { viewing = null })
    }

    GloveboxSheet(onDismiss = onDismiss) {
        ExportSheetContent(
            all = vm.all,
            vehicleId = vm.vehicleId,
            options = o,
            document = vm.document,
            fileName = vm.fileName,
            working = vm.working,
            preview = vm.preview,
            today = today,
            onChange = vm::change,
            onChooseVehicle = vm::chooseVehicle,
            onPickDate = { picking = it },
            onViewPage = { viewing = it },
            onExport = { vm.export(onStarted = onDismiss) },
        )
    }
}

/** The sheet's content, from plain state; [onPickDate] true is the range's start. */
@Composable
fun ExportSheetContent(
    all: List<Exportable>?,
    vehicleId: Long?,
    options: ExportOptions,
    document: ExportDocument?,
    fileName: String,
    working: Boolean,
    preview: ExportPreview?,
    today: LocalDate,
    onChange: (ExportOptions) -> Unit,
    onChooseVehicle: (Long?) -> Unit,
    onPickDate: (Boolean) -> Unit,
    onViewPage: (Int) -> Unit,
    onExport: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val o = options
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_export), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (all == null || document == null) {
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
                    onClick = { onChange(o.copy(format = f)) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = scheme.secondary, activeContentColor = scheme.onSecondary),
                    icon = {},
                ) { Text(label) }
            }
        }

        Preview(o.format, preview, onViewPage)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Dropdown(
                label = stringResource(R.string.details_vehicle),
                value = all.firstOrNull { it.id == vehicleId }?.label() ?: stringResource(R.string.home_all_vehicles),
                options = all.map { it.label() to { onChooseVehicle(it.id) } } +
                    listOfNotNull((stringResource(R.string.home_all_vehicles) to { onChooseVehicle(null) }).takeIf { all.size > 1 }),
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
                    stringResource(R.string.export_all_time) to { onChange(o.copy(dates = ExportDates.AllTime)) },
                    stringResource(R.string.export_this_year) to { onChange(o.copy(dates = ExportDates.ThisYear)) },
                    stringResource(R.string.export_custom_pick) to { onChange(o.copy(dates = o.dates as? ExportDates.Custom ?: ExportDates.Custom(today.withDayOfYear(1), today))) },
                ),
                modifier = Modifier.weight(1f),
            )
        }
        (o.dates as? ExportDates.Custom)?.let { range ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onPickDate(true) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.export_from, range.from.format(longDate)), color = scheme.onSurface)
                }
                OutlinedButton(onClick = { onPickDate(false) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.export_to, range.to.format(longDate)), color = scheme.onSurface)
                }
            }
        }

        RowsCard {
            Column {
                CheckRow(stringResource(R.string.export_costs), o.costsAndNotes) { onChange(o.copy(costsAndNotes = it)) }
                HorizontalDivider(color = scheme.outlineVariant)
                CheckRow(stringResource(R.string.export_schedule), o.scheduleAndRecalls) { onChange(o.copy(scheduleAndRecalls = it)) }
                HorizontalDivider(color = scheme.outlineVariant)
                CheckRow(stringResource(R.string.export_takes), o.whatItTakes) { onChange(o.copy(whatItTakes = it)) }
                HorizontalDivider(color = scheme.outlineVariant)
                CheckRow(stringResource(R.string.export_documents), o.documents) { onChange(o.copy(documents = it)) }
                HorizontalDivider(color = scheme.outlineVariant)
                val pdf = o.format == ExportFormat.PDF
                // The choice is remembered while Markdown is selected, so switching back to PDF restores it.
                CheckRow(stringResource(if (pdf) R.string.export_photos else R.string.export_photos_pdf_only), o.photos && pdf, enabled = pdf) { onChange(o.copy(photos = it)) }
            }
        }

        val photoCount = document.photoFiles.size
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
            Text(fileName, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono)
        }
        Text(
            stringResource(R.string.export_not_encrypted),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onPrimaryContainer,
        )

        val n = document.recordCount
        val hasContent = n > 0 || document.vehicles.any { it.schedule.isNotEmpty() || it.recalls.isNotEmpty() || it.whatItTakes.isNotEmpty() || it.documents.isNotEmpty() }
        Button(
            onClick = onExport,
            enabled = hasContent && !working,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(
                when {
                    working -> stringResource(R.string.export_preparing)
                    else -> stringResource(R.string.export_button, AppExportWords.records(n))
                },
            )
        }
        if (!hasContent) {
            Text(stringResource(R.string.export_nothing), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/** The PDF's first two pages as thumbnails, or the Markdown's first lines; a placeholder of the same size until ready. */
@Composable
private fun Preview(format: ExportFormat, preview: ExportPreview?, onViewPage: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val box = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            format == ExportFormat.PDF && preview is ExportPreview.Pdf -> {
                Row(
                    Modifier.fillMaxWidth().clip(PREVIEW_SHAPE).background(scheme.surfaceContainerHighest).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                ) {
                    preview.pages.forEachIndexed { i, page ->
                        val label = stringResource(R.string.export_preview_page, (i + 1).asNumber())
                        // The page is a picture of a document, so it is labelled as the button it is, not described.
                        Box(
                            Modifier.size(THUMB_WIDTH, THUMB_HEIGHT)
                                .shadow(4.dp, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .clickable(role = Role.Button) { onViewPage(i) }
                                .semantics { contentDescription = label },
                        ) {
                            Image(page, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Captions(stringResource(R.string.export_pages, pluralStringResource(R.plurals.export_page_count, preview.pageCount, preview.pageCount.asNumber())), stringResource(R.string.export_pdf_contents))
            }
            format == ExportFormat.MARKDOWN && preview is ExportPreview.Markdown -> {
                Text(
                    preview.firstLines,
                    modifier = box.clip(PREVIEW_SHAPE).background(scheme.surface).border(1.dp, scheme.outlineVariant, PREVIEW_SHAPE).padding(horizontal = 14.dp, vertical = 12.dp),
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    overflow = TextOverflow.Clip,
                )
                Captions(stringResource(R.string.export_md_contents), stringResource(R.string.export_md_no_charts))
            }
            else -> Box(box.clip(PREVIEW_SHAPE).background(scheme.surfaceContainerHighest), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun Captions(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(left, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(right, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The PDF a page at a time, full screen: pinch or double-tap to zoom, swipe between pages. */
@Composable
private fun PageViewer(start: Int, count: Int, load: suspend (Int) -> ImageBitmap?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pager = rememberPagerState(initialPage = start.coerceIn(0, count - 1)) { count }
        var zoomed by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.export_preview_close)) }
                Text(
                    stringResource(R.string.report_page, (pager.currentPage + 1).asNumber(), count.asNumber()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            HorizontalPager(state = pager, userScrollEnabled = !zoomed, modifier = Modifier.weight(1f).fillMaxWidth()) { i ->
                val image by produceState<ImageBitmap?>(null, i) { value = load(i) }
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    if (image == null) CircularProgressIndicator()
                    ZoomableImage(image, stringResource(R.string.export_preview_page, (i + 1).asNumber()), key = i, onZoomChange = { if (i == pager.currentPage) zoomed = it })
                }
            }
        }
    }
}

private val PREVIEW_HEIGHT = 199.dp
private val THUMB_WIDTH = 135.dp
private val THUMB_HEIGHT = 175.dp
private val PREVIEW_SHAPE = RoundedCornerShape(14.dp)

private fun Exportable.label(): String = if (archived) AppText.res.getString(R.string.export_vehicle_sold, name) else name

private fun Double.megabytesText(): String =
    if (this < 1) AppText.res.getString(R.string.export_under_mb) else AppText.res.getString(R.string.size_mb, toInt().coerceAtLeast(1).asNumber())

/**
 * A labelled choice that opens a menu, drawn as the design's `select`: the label above, a white field with a border.
 * The border is drawn around the button, not inside it, so it isn't read as the button's text colour.
 */
@Composable
private fun Dropdown(label: String, value: String, options: List<Pair<String, () -> Unit>>, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(48.dp).border(1.5.dp, scheme.outline, shape).background(scheme.surface, shape).padding(2.dp)) {
            Row(
                Modifier.fillMaxSize().clip(shape).clickable(role = Role.Button) { open = true }.semantics { contentDescription = "$label, $value" }.padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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
}

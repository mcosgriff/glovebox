package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.domain.ReadingProblem
import io.cosgriff.glovebox.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.LoggedReading
import io.cosgriff.glovebox.domain.ChartPoint
import io.cosgriff.glovebox.domain.MileageChart
import io.cosgriff.glovebox.domain.MileageReadings
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.domain.ReadingRow
import io.cosgriff.glovebox.ui.MileageReadingsRoute
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.components.DatePickerSheet
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.PlexMono
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class MileageReadingsState(
    val vehicleName: String,
    /** Newest first: the standalone readings and the ones logged with a service record. */
    val rows: List<ReadingRow>,
    val swap: ClusterSwap?,
    val unit: DistanceUnit,
)

/** Puts a reading back as it was. A singleton with its own scope because Undo can be tapped after the screen has closed. */
@Singleton
class MileageReadingUndo @Inject constructor(private val odometer: OdometerDao, private val vehicles: VehicleDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun restore(reading: OdometerReadingEntity) {
        scope.launch {
            // The vehicle itself may have been deleted since.
            if (vehicles.byId(reading.vehicleId) != null) odometer.put(reading)
        }
    }
}

@HiltViewModel
class MileageReadingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    service: ServiceDao,
    private val odometer: OdometerDao,
    private val undo: MileageReadingUndo,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<MileageReadingsRoute>().vehicleId

    private var entities: Map<Long, OdometerReadingEntity> = emptyMap()

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<MileageReadingsState?> = combine(
        vehicles.observe(vehicleId),
        odometer.observe(vehicleId),
        service.observeRecords(vehicleId),
    ) { vehicle, readings, records ->
        if (vehicle == null) return@combine null
        entities = readings.associateBy { it.id }
        val swap = vehicle.clusterSwap()
        MileageReadingsState(
            vehicleName = "${vehicle.year} ${vehicle.nickname ?: vehicle.model}",
            // Everything as typed; MileageReadings puts it in dash terms to compare.
            rows = MileageReadings.rows(
                readings.map { LoggedReading(it.id, it.date, it.odometer) } +
                    records.map { LoggedReading(it.record.id, it.record.date, it.record.odometer, fromRecord = true) },
                swap,
                vehicle.unit,
            ),
            swap = swap,
            unit = vehicle.unit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [onDone] gets the Undo, which puts the old date and value back. */
    fun correct(id: Long, date: LocalDate, miles: Int, onDone: (undo: () -> Unit) -> Unit) = viewModelScope.launch {
        val old = entities[id] ?: return@launch
        if (old.date == date && old.odometer == miles) return@launch
        odometer.correct(id, date, miles)
        onDone { undo.restore(old) }
    }

    /** [onDone] gets the Undo. */
    fun delete(id: Long, onDone: (reading: OdometerReadingEntity, undo: () -> Unit) -> Unit) = viewModelScope.launch {
        val old = entities[id] ?: return@launch
        odometer.delete(id)
        onDone(old) { undo.restore(old) }
    }
}

/**
 * Every odometer reading on a vehicle, so a wrong one can be found and put right after the Undo for
 * it has gone. No rendering on the canvas; follows the screens round it. Readings typed on the Update
 * mileage sheet (and the final odometer logged when a vehicle is sold) are corrected or deleted here;
 * one logged with a service is corrected on its record, which this links to.
 */
@Composable
fun MileageReadingsScreen(
    onBack: () -> Unit,
    onOpenRecord: (recordId: Long) -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    vm: MileageReadingsViewModel = hiltViewModel(),
) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val unit = state.unit
    val res = LocalResources.current
    // The reading's id, so the sheet survives the list changing under it.
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }

    state.rows.firstOrNull { !it.reading.fromRecord && it.reading.id == editing }?.let { row ->
        ReadingSheet(
            reading = row.reading,
            unit = unit,
            onSave = { date, miles ->
                editing = null
                vm.correct(row.reading.id, date, miles) { undo -> onMessage(res.getString(R.string.readings_corrected, miles.asDistance(unit)), res.getString(R.string.undo), undo) }
            },
            onDelete = {
                editing = null
                vm.delete(row.reading.id) { old, undo -> onMessage(res.getString(R.string.readings_deleted, old.odometer.asDistance(unit)), res.getString(R.string.undo), undo) }
            },
            onDismiss = { editing = null },
        )
    }

    // How far back the chart reaches, and the reading tapped on it: both survive Android restarting the app.
    var years by rememberSaveable { mutableStateOf<Int?>(null) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    MileageReadingsContent(
        state, LocalDate.now(), years, selected,
        onBack = onBack,
        onYears = {
            years = it
            selected = null
        },
        onSelect = { selected = it },
        onOpen = { row -> if (row.reading.fromRecord) onOpenRecord(row.reading.id) else editing = row.reading.id },
    )
}

/** Mileage readings as given, with no ViewModel: what [MileageReadingsScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun MileageReadingsContent(
    state: MileageReadingsState,
    today: LocalDate,
    /** How far back the chart reaches; null for every reading. */
    years: Int?,
    /** The chart's tapped reading, by [ChartPoint.key]. */
    selected: String?,
    onBack: () -> Unit,
    onYears: (Int?) -> Unit,
    onSelect: (String?) -> Unit,
    /** What a reading opens: its record, or the sheet that corrects or deletes it. */
    onOpen: (ReadingRow) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val unit = state.unit
    val most = remember(state, today) { MileageChart.yearsOnRecord(state.rows, today) }
    // A range as long as the readings, or longer (they were corrected since), is all of them.
    val reach = years?.takeIf { it < most }
    val series = remember(state, reach, today) { MileageChart.series(state.rows, state.swap, reach, today) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.readings_title), onBack, subtitle = state.vehicleName)

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                stringResource(R.string.readings_intro),
                color = scheme.onSurfaceVariant,
            )
            if (state.swap != null) {
                Text(
                    stringResource(R.string.readings_swap, state.swap.on.format(longDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (state.rows.isNotEmpty()) {
                MileageChartCard(
                    series, state.rows.size, reach, most, state.rows.minOf { it.reading.date }, unit, selected, onYears, onSelect,
                    onOpen = { onOpen(it.row) },
                )
            }

            if (state.rows.isEmpty()) {
                Text(stringResource(R.string.readings_none), color = scheme.onSurface)
            } else {
                val doubtful = state.rows.count { it.outOfLine || it.jump != null }
                SectionLabel(
                    pluralStringResource(R.plurals.readings_count, state.rows.size, state.rows.size.asNumber()).let {
                        if (doubtful > 0) stringResource(R.string.readings_count_to_check, it, doubtful.asNumber()) else it
                    },
                )
                FilledCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        state.rows.forEachIndexed { i, row ->
                            if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                            ReadingLine(row, unit, onClick = { onOpen(row) })
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ReadingLine(row: ReadingRow, unit: DistanceUnit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val reading = row.reading
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(if (reading.fromRecord) R.string.readings_open_record else R.string.readings_correct_or_delete), onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(reading.odometer.asDistance(unit), style = MaterialTheme.typography.titleMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                if (row.current) {
                    Surface(color = scheme.primaryContainer, shape = CircleShape) {
                        Text(
                            stringResource(R.string.readings_odometer_tag),
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Text(
                stringResource(if (reading.fromRecord) R.string.readings_from_service else R.string.readings_from_update, reading.date.format(longDate)),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            // Says which one to look at, never that it is wrong: only the user knows what the dash read.
            val doubt = when {
                row.outOfLine -> stringResource(R.string.readings_out_of_line)
                row.jump != null -> stringResource(R.string.readings_jump, row.jump!!.asDistance(unit))
                else -> null
            }
            doubt?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = scheme.error) }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
    }
}

/** Corrects one standalone reading's value or date, or deletes it. What is typed is saved as typed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingSheet(reading: LoggedReading, unit: DistanceUnit, onSave: (date: LocalDate, miles: Int) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var day by rememberSaveable { mutableLongStateOf(reading.date.toEpochDay()) }
    var text by rememberSaveable { mutableStateOf(reading.odometer.toString()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(day)
    val miles = OdometerInput.parse(text.typedNumber())
    val problem = MileageReadings.problem(miles, date, LocalDate.now())?.let {
        stringResource(
            when (it) {
                ReadingProblem.NOT_A_READING -> R.string.readings_problem_not_a_reading
                ReadingProblem.IN_FUTURE -> R.string.readings_problem_future
            },
        )
    }

    if (pickingDate) {
        DatePickerSheet(
            initial = date,
            onPick = {
                day = it.toEpochDay()
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }

    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.readings_correct_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.readings_was, reading.odometer.asDistance(unit), reading.date.format(longDate)),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).trimStart('0').take(MileageUpdate.MAX_DIGITS) },
                label = { Text(stringResource(R.string.odometer_in, unit.symbolWord)) },
                supportingText = problem?.takeIf { showErrors }?.let { { Text(it) } },
                isError = showErrors && problem != null,
                singleLine = true,
                textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text(stringResource(R.string.date_button, date.format(longDate)))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { if (miles != null && problem == null) onSave(date, miles) else showErrors = true },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            // Deleting is undone from the snackbar, so it asks nothing first.
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = scheme.error)) {
                Text(stringResource(R.string.readings_delete), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

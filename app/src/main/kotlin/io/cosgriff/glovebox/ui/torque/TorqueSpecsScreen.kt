package io.cosgriff.glovebox.ui.torque

import androidx.compose.ui.platform.LocalResources
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.flow
import io.cosgriff.glovebox.domain.KnownSpecs
import io.cosgriff.glovebox.domain.KnownSpecPlan
import io.cosgriff.glovebox.domain.KnownSpecList
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.TorqueExport
import io.cosgriff.glovebox.domain.WrittenFile
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.export.SaveAsImportFile
import io.cosgriff.glovebox.ui.service.toSaved
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueSpecs
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.TorqueSpecsRoute
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class TorqueSpecsState(
    val vehicle: VehicleEntity,
    /** Grouped by category in the order the chips show, then by name. */
    val specs: List<TorqueSpecEntity>,
    /** The unit chosen in Settings, which this screen starts in. */
    val settingsUnit: TorqueUnit,
    /** The bundled list for this model, with what adding it would do; null when there is none or nothing is left to add. */
    val known: Pair<KnownSpecList, KnownSpecPlan>? = null,
    /** Bundled list id to the name of its source, for the line on a row that came from one. */
    val knownSources: Map<String, String> = emptyMap(),
) {
    /** Only the categories in use get a chip. */
    val categories: List<TorqueCategory> get() = TorqueCategory.entries.filter { c -> specs.any { TorqueCategory.of(it.category) == c } }
}

/** Puts a deleted spec back. A singleton with its own scope because Undo can be tapped after the screen has closed. */
@Singleton
class TorqueSpecUndo @Inject constructor(private val torque: TorqueDao, private val vehicles: VehicleDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun restore(spec: TorqueSpecEntity) {
        scope.launch {
            // The vehicle itself may have been deleted since.
            if (vehicles.byId(spec.vehicleId) != null) torque.insert(spec)
        }
    }
}

@HiltViewModel
class TorqueSpecsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    private val torque: TorqueDao,
    settings: SettingsRepository,
    private val undo: TorqueSpecUndo,
    private val saver: ExportSaver,
    catalog: KnownSpecCatalog,
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<TorqueSpecsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<TorqueSpecsState?> = combine(
        vehicles.observe(vehicleId),
        torque.observe(vehicleId),
        settings.settings,
        flow { emit(catalog.lists()) },
    ) { vehicle, specs, s, lists ->
        if (vehicle == null) return@combine null
        // Offered only for the model a list is for, and only while it has something this vehicle lacks.
        val known = KnownSpecs.match(lists, vehicle.year, vehicle.make, vehicle.model)
            ?.let { list -> list to KnownSpecs.plan(list, specs.map { it.toSaved() }) }
            ?.takeIf { it.second.changes > 0 }
        TorqueSpecsState(
            vehicle = vehicle,
            specs = specs.sortedWith(compareBy<TorqueSpecEntity> { TorqueCategory.of(it.category).ordinal }.thenBy { it.name.lowercase() }),
            settingsUnit = s.torqueUnit,
            known = known,
            knownSources = lists.associate { it.id to it.source.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The rows as an import file, with values in the unit the user works in. */
    fun written(): WrittenFile = state.value?.let { s -> TorqueExport.file(s.specs.map { it.toSaved() }, s.settingsUnit) } ?: WrittenFile(null, 0)

    fun saveAsFile(text: String) {
        val v = state.value?.vehicle ?: return
        viewModelScope.launch { saver.saveImportFile(text, TorqueExport.fileName("${v.year} ${v.make} ${v.model}")) }
    }

    /** [draft] with id 0 is a new spec. An edit keeps what the editor doesn't show: that the row came from a bundled list. */
    fun save(draft: SpecDraft) = viewModelScope.launch {
        val before = state.value?.specs?.firstOrNull { it.id == draft.id && draft.id != 0L }
        val entity = (before ?: TorqueSpecEntity(vehicleId = vehicleId, name = draft.name, category = draft.category.name)).copy(
            name = draft.name,
            category = draft.category.name,
            socketSize = draft.socketSize,
            newtonMeters = draft.torque?.newtonMeters,
            source = draft.source.name,
            note = draft.note,
        )
        if (before == null) torque.insert(entity) else torque.update(entity)
    }

    /**
     * Adds the bundled list's values: new rows, and rows that were only a name. Every value arrives as a
     * reference to double-check, marked for good with the list it came from. A row the user has put
     * anything in is never touched.
     */
    fun addKnown() = viewModelScope.launch {
        val (list, _) = state.value?.known ?: return@launch
        val rows = torque.specsFor(vehicleId)
        // Worked out again from what is saved now, in case the list on screen was stale.
        val plan = KnownSpecs.plan(list, rows.map { it.toSaved() })
        val byId = rows.associateBy { it.id }
        fun TorqueSpecEntity.from(item: ImportedTorqueSpec) = copy(
            socketSize = item.socket,
            newtonMeters = item.torque?.newtonMeters,
            source = TorqueSource.REFERENCE.name,
            note = item.note,
            knownList = list.id,
        )
        torque.applyImport(
            plan.new.map { TorqueSpecEntity(vehicleId = vehicleId, name = it.name, category = it.category.name).from(it) },
            plan.fills.mapNotNull { (item, saved) -> byId[saved.id]?.from(item) },
        )
    }

    /** [onDeleted] gets the Undo. */
    fun delete(spec: TorqueSpecEntity, onDeleted: (undo: () -> Unit) -> Unit) = viewModelScope.launch {
        torque.delete(spec)
        onDeleted { undo.restore(spec) }
    }

    /** Named rows with nothing filled in, skipping any name already there. */
    fun addCommon() = viewModelScope.launch {
        val have = state.value?.specs.orEmpty().map { it.name.lowercase() }.toSet()
        torque.insertAll(
            TorqueSpecs.common.filter { it.label.lowercase() !in have }
                .map { TorqueSpecEntity(vehicleId = vehicleId, name = it.label, category = it.category.name) },
        )
    }
}

/** Design: 4e · Torque & fastener specs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorqueSpecsScreen(
    onBack: () -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    vm: TorqueSpecsViewModel = hiltViewModel(),
) {
    val res = LocalResources.current
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val v = state.vehicle
    val scheme = MaterialTheme.colorScheme
    // Starts in the Settings unit; changing it here is only for this visit.
    var unitName by rememberSaveable { mutableStateOf<String?>(null) }
    val unit = unitName?.let(TorqueUnit::valueOf) ?: state.settingsUnit
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = state.categories
    // A filter can outlive the last spec in it.
    val category = categoryName?.let(TorqueCategory::valueOf)?.takeIf { it in categories }
    /** Id of the spec in the editor; 0 for a new one, null when closed. */
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }

    editing?.let { id ->
        val spec = state.specs.firstOrNull { it.id == id }
        if (id == 0L || spec != null) {
            SpecEditorSheet(
                spec = spec,
                listSource = spec?.knownList?.let { state.knownSources[it] ?: stringResource(R.string.torque_a_reference_list) },
                unit = unit,
                startCategory = category ?: TorqueCategory.ENGINE,
                onSave = {
                    editing = null
                    vm.save(it)
                },
                onDelete = {
                    editing = null
                    spec?.let { s -> vm.delete(s) { undo -> onMessage(res.getString(R.string.torque_deleted, s.name), res.getString(R.string.undo), undo) } }
                },
                onDismiss = { editing = null },
            )
        }
    }

    val shown = state.specs.filter {
        (category == null || TorqueCategory.of(it.category) == category) && TorqueSpecs.matches(query, it.name, it.socketSize, it.note)
    }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        stringResource(R.string.details_torque),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        listOfNotNull("${v.year} ${v.nickname ?: v.model}", v.engine).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.specs.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.documents_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.torque_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = vm::addCommon,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, scheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    ) {
                        Text(stringResource(R.string.torque_start_common))
                    }
                    Text(
                        stringResource(R.string.torque_start_common_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    state.known?.let { (list, plan) -> KnownOffer(list, plan, onAdd = vm::addKnown) }
                }
            }
        } else {
            item {
                Column(Modifier.padding(start = 20.dp, top = 2.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.torque_search)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.torque_show_in),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColors().soon,
                        )
                        UnitSwitch(unit, onPick = { unitName = it.name }, modifier = Modifier.weight(1.6f))
                    }
                    if (categories.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(stringResource(R.string.history_all), selected = category == null) { categoryName = null }
                            categories.forEach { c -> Chip(c.label, selected = category == c) { categoryName = c.name } }
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(stringResource(R.string.torque_nothing_matches), Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = scheme.onSurfaceVariant)
                }
            }
            items(shown, key = { it.id }) { spec ->
                SpecCard(
                    spec,
                    unit,
                    listSource = spec.knownList?.let { state.knownSources[it] ?: stringResource(R.string.torque_a_reference_list) },
                    onClick = { editing = spec.id },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        item {
            Column(Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(
                    onClick = { editing = 0L },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text(stringResource(R.string.torque_add))
                }
                if (state.specs.isNotEmpty() && TorqueSpecs.common.any { t -> state.specs.none { it.name.equals(t.label, ignoreCase = true) } }) {
                    TextButton(onClick = vm::addCommon, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(stringResource(R.string.torque_add_missing))
                    }
                }
                if (state.specs.isNotEmpty()) state.known?.let { (list, plan) -> KnownOffer(list, plan, onAdd = vm::addKnown) }
                Text(
                    stringResource(R.string.torque_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                if (state.specs.isNotEmpty()) {
                    SaveAsImportFile(
                        caption = stringResource(R.string.torque_save_caption),
                        write = vm::written,
                        onSave = vm::saveAsFile,
                    )
                }
            }
        }
    }
}

@Composable
internal fun UnitSwitch(unit: TorqueUnit, onPick: (TorqueUnit) -> Unit, modifier: Modifier = Modifier) {
    // Canvas: N·m, ft-lb, in-lb, with the chosen one solid ink as elsewhere in the app.
    val order = listOf(TorqueUnit.NEWTON_METER, TorqueUnit.FOOT_POUND, TorqueUnit.INCH_POUND)
    SingleChoiceSegmentedButtonRow(modifier) {
        order.forEachIndexed { i, u ->
            SegmentedButton(
                selected = u == unit,
                onClick = { onPick(u) },
                shape = SegmentedButtonDefaults.itemShape(i, order.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondary,
                    activeContentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                icon = {},
            ) { Text(u.symbolWord, maxLines = 1) }
        }
    }
}

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
    )
}

/** [listSource] names where the bundled list this row started from came from; null for a row that never came from one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecCard(spec: TorqueSpecEntity, unit: TorqueUnit, listSource: String?, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val torque = spec.newtonMeters?.let(::Torque)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(spec.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        spec.socketSize?.let { Tag(it, scheme.surfaceVariant, statusColors().soon, mono = true) }
                        when {
                            torque == null -> Tag(stringResource(R.string.torque_tag_needs_value), scheme.primaryContainer, scheme.onPrimaryContainer)
                            // Somebody else's figure, not yet checked: the loudest tag on the screen.
                            TorqueSource.of(spec.source) == TorqueSource.REFERENCE -> Tag(stringResource(R.string.torque_tag_double_check), scheme.primary, scheme.onPrimary)
                            TorqueSource.of(spec.source) == TorqueSource.FACTORY -> Tag(stringResource(R.string.torque_tag_factory), scheme.tertiaryContainer, scheme.onTertiaryContainer)
                            else -> Tag(stringResource(R.string.torque_tag_yours), scheme.surfaceVariant, statusColors().soon)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (torque != null) {
                        Text(torque.text(unit), style = MaterialTheme.typography.titleLarge, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                        Text(
                            TorqueUnit.entries.filter { it != unit }.joinToString(" · ") { torque.text(it) },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = PlexMono,
                            color = scheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            stringResource(R.string.fluids_tap_to_add),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = PlexMono,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }
            }
            spec.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = statusColors().soon) }
            // Stays for good, whatever the tag above says now: this row began as somebody else's figure.
            listSource?.let { Text(stringResource(R.string.torque_started_from, it), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        }
    }
}

/**
 * The offer of a bundled list. It says whose values they are and that each is to be checked, every
 * time it is shown: a torque figure from a website is not one from the manual.
 */
@Composable
private fun KnownOffer(list: KnownSpecList, plan: KnownSpecPlan, onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, scheme.onSurface),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
        ) {
            Text(stringResource(R.string.torque_add_known, list.title))
        }
        Text(offer(list, plan), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun Tag(text: String, container: Color, content: Color, mono: Boolean = false) {
    Surface(color = container, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = if (mono) PlexMono else null,
            fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Bold,
            color = content,
        )
    }
}

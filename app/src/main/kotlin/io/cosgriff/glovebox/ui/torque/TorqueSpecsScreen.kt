package io.cosgriff.glovebox.ui.torque

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
import androidx.compose.ui.text.font.FontFamily
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
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<TorqueSpecsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<TorqueSpecsState?> = combine(
        vehicles.observe(vehicleId),
        torque.observe(vehicleId),
        settings.settings,
    ) { vehicle, specs, s ->
        if (vehicle == null) return@combine null
        TorqueSpecsState(
            vehicle = vehicle,
            specs = specs.sortedWith(compareBy<TorqueSpecEntity> { TorqueCategory.of(it.category).ordinal }.thenBy { it.name.lowercase() }),
            settingsUnit = s.torqueUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [draft] with id 0 is a new spec. */
    fun save(draft: SpecDraft) = viewModelScope.launch {
        val entity = TorqueSpecEntity(
            id = draft.id,
            vehicleId = vehicleId,
            name = draft.name,
            category = draft.category.name,
            socketSize = draft.socketSize,
            newtonMeters = draft.torque?.newtonMeters,
            source = draft.source.name,
            note = draft.note,
        )
        if (draft.id == 0L) torque.insert(entity) else torque.update(entity)
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
            TorqueSpecs.common.filter { it.name.lowercase() !in have }
                .map { TorqueSpecEntity(vehicleId = vehicleId, name = it.name, category = it.category.name) },
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
                unit = unit,
                startCategory = category ?: TorqueCategory.ENGINE,
                onSave = {
                    editing = null
                    vm.save(it)
                },
                onDelete = {
                    editing = null
                    spec?.let { s -> vm.delete(s) { undo -> onMessage("${s.name} deleted", "Undo", undo) } }
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
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        "Torque & fastener specs",
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
                    Text("Nothing saved yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Keep the socket sizes and torque values you look up, so they're here next time. " +
                            "Glovebox has no factory values: you enter them from the service manual.",
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
                        Text("Start with common fasteners")
                    }
                    Text(
                        "Adds named rows such as Oil drain plug and Lug nuts, with the values left for you to fill in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                Column(Modifier.padding(start = 20.dp, top = 2.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search — drain plug, lug nut…") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "SHOW TORQUE IN",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColors().soon,
                        )
                        UnitSwitch(unit, onPick = { unitName = it.name }, modifier = Modifier.weight(1.6f))
                    }
                    if (categories.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("All", selected = category == null) { categoryName = null }
                            categories.forEach { c -> Chip(c.label, selected = category == c) { categoryName = c.name } }
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text("Nothing matches.", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = scheme.onSurfaceVariant)
                }
            }
            items(shown, key = { it.id }) { spec ->
                SpecCard(spec, unit, onClick = { editing = spec.id }, modifier = Modifier.padding(horizontal = 20.dp))
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
                    Text("+ Add a spec")
                }
                if (state.specs.isNotEmpty() && TorqueSpecs.common.any { t -> state.specs.none { it.name.equals(t.name, ignoreCase = true) } }) {
                    TextButton(onClick = vm::addCommon, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Add the common fasteners that are missing")
                    }
                }
                Text(
                    "Enter torque in any unit — it converts automatically. Always check the factory service manual; " +
                        "a wrong torque on lug nuts or drain plugs can cause damage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
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
            ) { Text(u.symbol, maxLines = 1) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecCard(spec: TorqueSpecEntity, unit: TorqueUnit, onClick: () -> Unit, modifier: Modifier) {
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
                            torque == null -> Tag("NEEDS VALUE", scheme.primaryContainer, scheme.onPrimaryContainer)
                            TorqueSource.of(spec.source) == TorqueSource.FACTORY -> Tag("FACTORY MANUAL", scheme.tertiaryContainer, scheme.onTertiaryContainer)
                            else -> Tag("YOUR ENTRY", scheme.surfaceVariant, statusColors().soon)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (torque != null) {
                        Text(torque.format(unit), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        Text(
                            TorqueUnit.entries.filter { it != unit }.joinToString(" · ") { torque.format(it) },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = scheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Tap to add",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }
            }
            spec.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = statusColors().soon) }
        }
    }
}

@Composable
private fun Tag(text: String, container: Color, content: Color, mono: Boolean = false) {
    Surface(color = container, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = if (mono) FontFamily.Monospace else null,
            fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Bold,
            color = content,
        )
    }
}

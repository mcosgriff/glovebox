package io.cosgriff.glovebox.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportFormat
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.SavedTorqueSpec
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueImport
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** An import file the user picked: its name and what it says. Held in memory only, never written anywhere. */
data class PickedImport(val name: String, val text: String)

/**
 * Lets the user pick an import file with Android's file picker. A singleton with its launcher in
 * the activity, like the other pickers: choosing a file leaves the app, which can lock it.
 */
@Singleton
class ImportPicker @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var launch: (() -> Unit)? = null

    private val _picked = MutableStateFlow<PickedImport?>(null)
    val picked: StateFlow<PickedImport?> = _picked.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    fun attach(launcher: () -> Unit) {
        launch = launcher
    }

    fun detach(launcher: () -> Unit) {
        if (launch === launcher) launch = null
    }

    fun pick() {
        _problem.value = null
        launch?.invoke()
    }

    /** Null when the user backed out. */
    fun onPicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val text = withContext(Dispatchers.IO) { read(uri) }
            when (text) {
                null -> _problem.value = "That file couldn't be opened."
                TOO_BIG -> _problem.value = "That file is too big to be an import file (the most is 2 MB)."
                else -> _picked.value = PickedImport(displayName(uri) ?: "Import file", text)
            }
        }
    }

    fun clear() {
        _picked.value = null
        _problem.value = null
    }

    /** Stops reading as soon as the file is past the size cap, so a huge file is never held in memory. */
    private fun read(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (out.size() <= ImportFormat.MAX_BYTES) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            if (out.size() > ImportFormat.MAX_BYTES) TOO_BIG else out.toString(Charsets.UTF_8.name())
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private fun displayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        // Not valid JSON, so it can never be mistaken for a file's contents.
        const val TOO_BIG = "\u0000too-big"
    }
}

typealias TorquePlan = ImportPlan<ImportedTorqueSpec, SavedTorqueSpec>

/** What the chosen file comes to for the chosen vehicle. */
sealed interface ImportPreview {
    data class Invalid(val file: ImportFile.Invalid) : ImportPreview
    data class NotYet(val kindLabel: String) : ImportPreview

    /** A good file, but there are several vehicles and none has been chosen. */
    data object NeedsVehicle : ImportPreview
    data class Torque(val plan: TorquePlan) : ImportPreview
}

data class ImportResult(val vehicleId: Long, val vehicleName: String, val added: Int, val filled: Int, val replaced: Int, val unchanged: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ImportViewModel @Inject constructor(
    vehicles: VehicleDao,
    settings: SettingsRepository,
    private val torque: TorqueDao,
    private val picker: ImportPicker,
) : ViewModel() {
    val vehicles: StateFlow<List<VehicleEntity>?> = vehicles.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val torqueUnit: StateFlow<TorqueUnit> = settings.settings.map { it.torqueUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorqueUnit.FOOT_POUND)

    val picked: StateFlow<PickedImport?> = picker.picked
    val problem: StateFlow<String?> = picker.problem

    private val chosenVehicle = MutableStateFlow<Long?>(null)

    /** The vehicle to import into: the one chosen, or the only one there is. */
    val vehicleId: StateFlow<Long?> = combine(this.vehicles, chosenVehicle) { list, chosen ->
        chosen?.takeIf { id -> list.orEmpty().any { it.id == id } } ?: list?.singleOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val parsed = picker.picked.mapLatest { p -> p?.let { withContext(Dispatchers.Default) { ImportFormat.parse(it.text) } } }

    /** Existing rows by id, as they stood when the preview was worked out; what a replace writes over. */
    private var existing: Map<Long, TorqueSpecEntity> = emptyMap()

    /** Null with no file picked. Worked out afresh for each file and vehicle; nothing is written by it. */
    val preview: StateFlow<ImportPreview?> = combine(parsed, vehicleId) { file, id -> file to id }.mapLatest { (file, id) ->
        when (file) {
            null -> null
            is ImportFile.Invalid -> ImportPreview.Invalid(file)
            is ImportFile.NotYet -> ImportPreview.NotYet(file.kind.label)
            is ImportFile.TorqueSpecs -> if (id == null) {
                ImportPreview.NeedsVehicle
            } else {
                val rows = torque.specsFor(id)
                existing = rows.associateBy { it.id }
                ImportPreview.Torque(TorqueImport.plan(file.items, rows.map { it.toSaved() }))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _result = MutableStateFlow<ImportResult?>(null)
    val result: StateFlow<ImportResult?> = _result.asStateFlow()

    fun chooseVehicle(id: Long) {
        chosenVehicle.value = id
    }

    fun pick() {
        _result.value = null
        picker.pick()
    }

    fun clear() {
        _result.value = null
        picker.clear()
    }

    /** Writes [plan] in one transaction. Rows the file doesn't mention are never touched. */
    fun import(plan: TorquePlan, mode: ImportMode) = viewModelScope.launch {
        val id = vehicleId.value ?: return@launch
        val vehicle = vehicles.value?.firstOrNull { it.id == id } ?: return@launch
        val added = plan.new.map {
            TorqueSpecEntity(
                vehicleId = id,
                name = it.name,
                category = it.category.name,
                socketSize = it.socket,
                newtonMeters = it.torque?.newtonMeters,
                source = it.source.name,
                note = it.note,
            )
        }
        // The saved row keeps its own name and category: they are what matched, give or take capitals.
        val replaced = plan.replaced(mode).mapNotNull { (item, saved) ->
            existing[saved.id]?.copy(socketSize = item.socket, newtonMeters = item.torque?.newtonMeters, source = item.source.name, note = item.note)
        }
        torque.applyImport(added, replaced)
        _result.value = ImportResult(
            vehicleId = id,
            vehicleName = "${vehicle.year} ${vehicle.nickname ?: vehicle.model}",
            added = added.size,
            filled = plan.fills.size,
            replaced = replaced.size - plan.fills.size,
            unchanged = plan.total - added.size - replaced.size,
        )
        picker.clear()
    }

    private fun TorqueSpecEntity.toSaved() = SavedTorqueSpec(
        id = id,
        name = name,
        category = TorqueCategory.of(category),
        socket = socketSize,
        torque = newtonMeters?.let(::Torque),
        source = TorqueSource.of(source),
        note = note,
    )
}

private const val README = "https://github.com/mcosgriff/glovebox/blob/main/docs/import/README.md"

/** Settings → Import. The format, and what an import may and may not change, are in `docs/import/README.md`. */
@Composable
fun ImportScreen(onBack: () -> Unit, onOpenTorqueSpecs: (vehicleId: Long) -> Unit, vm: ImportViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    val vehicles = vm.vehicles.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val vehicleId by vm.vehicleId.collectAsStateWithLifecycle()
    val picked by vm.picked.collectAsStateWithLifecycle()
    val problem by vm.problem.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val unit by vm.torqueUnit.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var mode by rememberSaveable(picked?.name) { mutableStateOf(ImportMode.ADD_NEW) }
    var confirming by rememberSaveable { mutableStateOf(false) }

    val plan = (preview as? ImportPreview.Torque)?.plan
    if (confirming && plan != null) {
        ReplaceDialog(
            plan = plan,
            unit = unit,
            onConfirm = {
                confirming = false
                vm.import(plan, ImportMode.REPLACE_MATCHING)
            },
            onDismiss = { confirming = false },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Import", Modifier.padding(start = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Add torque specs to a vehicle from a JSON file. The file is checked first, and you see what it would do before anything is saved.",
                    color = scheme.onSurfaceVariant,
                )
                TextButton(onClick = { uriHandler.openUri(README) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Text("How to write a file (opens in your browser)")
                }
            }

            if (vehicles.isEmpty()) {
                Text("Add a vehicle first: an import goes into one of your vehicles.", color = scheme.onSurfaceVariant)
                return@Column
            }

            result?.let { r ->
                ResultCard(r, onOpen = { onOpenTorqueSpecs(r.vehicleId) })
                OutlinedButton(onClick = vm::pick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Import another file")
                }
                return@Column
            }

            if (vehicles.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("IMPORT INTO")
                    Card {
                        Column(Modifier.selectableGroup()) {
                            vehicles.forEachIndexed { i, v ->
                                if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                                Choice(
                                    title = "${v.year} ${v.nickname ?: v.model}",
                                    detail = listOfNotNull(v.trim, v.engine).joinToString(" · ").ifEmpty { null },
                                    selected = v.id == vehicleId,
                                    onClick = { vm.chooseVehicle(v.id) },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("FILE")
                val file = picked
                if (file == null) {
                    Button(onClick = vm::pick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Choose a file") }
                } else {
                    Card {
                        Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(file.name, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyLarge, fontFamily = PlexMono)
                            TextButton(onClick = vm::pick, modifier = Modifier.heightIn(min = 48.dp)) { Text("Change") }
                        }
                    }
                }
                problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.error) }
            }

            when (val p = preview) {
                null -> Unit
                is ImportPreview.Invalid -> ErrorsCard(p.file)
                is ImportPreview.NotYet -> Text(
                    "This is a ${p.kindLabel.lowercase()} file. This version of Glovebox imports torque specs only.",
                    color = scheme.onSurface,
                )
                ImportPreview.NeedsVehicle -> Text("The file is fine. Choose which vehicle it goes into.", color = scheme.onSurface)
                is ImportPreview.Torque -> {
                    PlanCard(p.plan, unit)
                    if (p.plan.differs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("ROWS THAT DIFFER FROM YOURS")
                            Card {
                                Column(Modifier.selectableGroup()) {
                                    Choice("Add new only", "Yours stay exactly as they are", mode == ImportMode.ADD_NEW) { mode = ImportMode.ADD_NEW }
                                    HorizontalDivider(color = scheme.outlineVariant)
                                    Choice(
                                        "Replace matching",
                                        rows(p.plan.differs.size) + " of yours " + (if (p.plan.differs.size == 1) "takes" else "take") + " the file's values",
                                        mode == ImportMode.REPLACE_MATCHING,
                                    ) { mode = ImportMode.REPLACE_MATCHING }
                                }
                            }
                        }
                    }
                    val changes = p.plan.changes(mode)
                    if (changes == 0) {
                        Text(
                            if (p.plan.differs.isEmpty()) {
                                "Nothing to import: everything in this file is already on the vehicle."
                            } else {
                                "Nothing to import unless you replace the rows that differ."
                            },
                            color = scheme.onSurface,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { if (mode == ImportMode.REPLACE_MATCHING && p.plan.differs.isNotEmpty()) confirming = true else vm.import(p.plan, mode) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            ) {
                                Text("Import ${rows(changes)}")
                            }
                            Text(
                                "Nothing on the vehicle is deleted. There is no Undo, so check the list above.",
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun rows(n: Int): String = if (n == 1) "1 row" else "$n rows"

/** "100 ft-lb · 21 mm · Star pattern", or what to say for a row that is only a name. */
private fun describe(torque: Torque?, socket: String?, note: String?, unit: TorqueUnit): String =
    listOfNotNull(torque?.format(unit), socket, note).joinToString(" · ").ifEmpty { "Name only" }

private fun ImportedTorqueSpec.describe(unit: TorqueUnit) = describe(torque, socket, note, unit) + if (source == TorqueSource.FACTORY) " · factory manual" else ""
private fun SavedTorqueSpec.describe(unit: TorqueUnit) = describe(torque, socket, note, unit) + if (source == TorqueSource.FACTORY && torque != null) " · factory manual" else ""

@Composable
private fun PlanCard(plan: TorquePlan, unit: TorqueUnit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("TORQUE SPECS · ${plan.total} IN THE FILE")
        Card {
            Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Group("${plan.new.size} new", "Added to the vehicle", plan.new.map { it.name to it.describe(unit) })
                Group("${plan.fills.size} fill in a row you left empty", "Yours has only a name so far", plan.fills.map { it.first.name to it.first.describe(unit) })
                Group(
                    "${plan.differs.size} different from yours",
                    "Left alone unless you choose to replace them",
                    plan.differs.map { (item, saved) -> saved.name to "Yours: ${saved.describe(unit)}\nFile: ${item.describe(unit)}" },
                    color = scheme.onPrimaryContainer,
                )
                if (plan.same.isNotEmpty()) {
                    Text("${plan.same.size} already the same · left alone", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                Text(
                    "Check every value against your service manual, whoever wrote the file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Group(title: String, detail: String, lines: List<Pair<String, String>>, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    if (lines.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        lines.take(SHOWN).forEach { (name, what) ->
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium)
                Text(what, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
            }
        }
        if (lines.size > SHOWN) Text("and ${lines.size - SHOWN} more", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

/** Long files are summarised on the page; the confirmation for a replace lists every row it changes. */
private const val SHOWN = 12

@Composable
private fun ErrorsCard(file: ImportFile.Invalid) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("THIS FILE CAN'T BE IMPORTED")
        Card {
            Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                file.errors.forEach { e ->
                    Column {
                        if (e.path.isNotEmpty()) Text(e.path, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, color = scheme.error)
                        Text(e.message.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (file.more > 0) Text("and ${file.more} more", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        Text("Nothing was saved. Fix the file and choose it again.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultCard(r: ImportResult, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("IMPORTED INTO ${r.vehicleName.uppercase()}")
        Card {
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(r.added to "added", r.filled to "filled in", r.replaced to "replaced with the file's values").filter { it.first > 0 }.forEach { (n, what) ->
                    Text("${rows(n)} $what", style = MaterialTheme.typography.bodyLarge, color = statusColors().ok)
                }
                if (r.unchanged > 0) Text("${rows(r.unchanged)} left as they were", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("See torque specs") }
    }
}

@Composable
private fun ReplaceDialog(plan: TorquePlan, unit: TorqueUnit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace ${rows(plan.differs.size)} of yours?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                plan.differs.forEach { (item, saved) ->
                    Column {
                        Text(saved.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Yours: ${saved.describe(unit)}", style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
                        Text("Becomes: ${item.describe(unit)}", style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono)
                    }
                }
                Text("This can't be undone.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = scheme.error)) { Text("Replace and import") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Choice(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), color = scheme.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

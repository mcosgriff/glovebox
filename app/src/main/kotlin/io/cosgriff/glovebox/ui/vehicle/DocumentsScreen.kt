package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.components.BorderedCard
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.fileSize
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.domain.DocumentProblem
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.domain.DocumentFormat
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.StoredDocument
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.ui.DocumentsRoute
import io.cosgriff.glovebox.ui.components.DatePickerSheet
import io.cosgriff.glovebox.ui.service.longDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/** One vehicle's documents, sorted for the list, with the rows they came from. */
data class DocumentsData(val vehicle: VehicleEntity, val documents: List<Pair<StoredDocument, DocumentEntity>>) {
    val totalBytes: Long get() = documents.sumOf { it.first.sizeBytes }
}

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    private val documents: DocumentDao,
    private val store: DocumentStore,
    private val picker: DocumentPicker,
) : ViewModel() {
    val vehicleId = savedStateHandle.route<DocumentsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val data: StateFlow<DocumentsData?> = combine(vehicles.observe(vehicleId), documents.observe(vehicleId)) { vehicle, rows ->
        vehicle ?: return@combine null
        val byId = rows.associateBy { it.id }
        DocumentsData(vehicle, VehicleDocuments.sorted(rows.map { it.toStored() }).map { it to byId.getValue(it.id) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A document just added to this vehicle, to ask for its details. */
    val added: StateFlow<Long?> = picker.added.map { it?.takeIf { a -> a.vehicleId == vehicleId }?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val problem: StateFlow<DocumentProblem?> = picker.problem
    val working: StateFlow<Boolean> = picker.working

    fun choose() = picker.pick(vehicleId)

    fun capture() = picker.capture(vehicleId)

    fun consumeAdded() = picker.consumeAdded()

    fun clearProblem() = picker.clearProblem()

    fun describe(id: Long, type: DocumentType, name: String, date: LocalDate?) = viewModelScope.launch { documents.describe(id, type.name, name, date) }

    fun delete(document: DocumentEntity) = viewModelScope.launch { store.delete(document) }

    suspend fun copy(document: DocumentEntity): File? = store.openCopy(document)

}

fun DocumentEntity.toStored() = StoredDocument(
    id = id,
    type = VehicleDocuments.type(type),
    name = name,
    date = date,
    format = VehicleDocuments.format(format),
    sizeBytes = sizeBytes,
)

/** A vehicle's paperwork: a history report, the title, the registration. Opened from Vehicle details. */
@Composable
fun DocumentsScreen(onBack: () -> Unit, onView: (Long) -> Unit, vm: DocumentsViewModel = hiltViewModel()) {
    val d = vm.data.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val added by vm.added.collectAsStateWithLifecycle()
    val problem by vm.problem.collectAsStateWithLifecycle()
    val working by vm.working.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var isNew by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    var failed by remember { mutableStateOf<Int?>(null) }

    // A document that has just been added opens its details, so it can be named.
    LaunchedEffect(added) {
        added?.let {
            editing = it
            isNew = true
            vm.consumeAdded()
        }
    }
    LaunchedEffect(Unit) { vm.clearProblem() }

    fun open(row: DocumentEntity, share: Boolean) = scope.launch {
        failed = null
        val copy = vm.copy(row)
        failed = when {
            copy == null -> R.string.documents_unreadable
            !context.hand(copy, VehicleDocuments.format(row.format), share) -> if (share) R.string.documents_no_app_share else R.string.documents_no_app_open
            else -> null
        }
    }

    val byId = d.documents.associate { it.first.id to it }
    editing?.let { id ->
        val (doc, _) = byId[id] ?: return@let
        DetailsDialog(
            document = doc,
            isNew = isNew,
            onSave = { type, name, date ->
                vm.describe(id, type, name, date)
                editing = null
            },
            onDismiss = { editing = null },
            onDiscard = {
                vm.delete(byId.getValue(id).second)
                editing = null
            },
        )
    }
    deleting?.let { id ->
        val (doc, row) = byId[id] ?: return@let
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.documents_delete_title, doc.name)) },
            text = { Text(stringResource(R.string.documents_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(row)
                        deleting = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Column(Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.documents_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${d.vehicle.year} ${d.vehicle.nickname ?: d.vehicle.model}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        if (d.documents.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.documents_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.documents_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(d.documents, key = { it.first.id }) { (doc, row) ->
            DocumentCard(
                document = doc,
                onView = { onView(doc.id) },
                onOpenElsewhere = { open(row, share = false) },
                onShare = { open(row, share = true) },
                onEdit = {
                    editing = doc.id
                    isNew = false
                },
                onDelete = { deleting = doc.id },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item {
            Column(Modifier.padding(start = 20.dp, top = 6.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AddButton(working = working, onChoose = vm::choose, onTake = vm::capture)
                (failed?.let { stringResource(it) } ?: problem?.text)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.error) }
                Text(
                    listOfNotNull(
                        d.documents.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.sentence, documentsSummary(it.size, d.totalBytes)) },
                        stringResource(R.string.documents_note, (VehicleDocuments.MAX_BYTES / (1024 * 1024)).toInt().asNumber()),
                    ).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(document: StoredDocument, onView: () -> Unit, onOpenElsewhere: () -> Unit, onShare: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    BorderedCard(modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.open), onClick = onView)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (document.format == DocumentFormat.PDF) Icons.Outlined.Description else Icons.Outlined.Image,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(document.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        document.type.label,
                        document.date?.format(longDate),
                        stringResource(if (document.format == DocumentFormat.PDF) R.string.documents_pdf_size else R.string.documents_picture_size, fileSize(document.sizeBytes)),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_for, document.name)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.open)) }, onClick = { menu = false; onView() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.documents_open_elsewhere)) }, onClick = { menu = false; onOpenElsewhere() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, onClick = { menu = false; onShare() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.documents_edit_details)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = scheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

/**
 * "Choose a file" or "Take a photo". Taking a photo asks for the camera permission first, because an
 * app that declares that permission (as this one does, for VIN scanning) may not hand off to the camera
 * app without it.
 */
@Composable
private fun AddButton(working: Boolean, onChoose: () -> Unit, onTake: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onTake() else refused = true
    }
    Box {
        OutlinedButton(
            onClick = { open = true },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, scheme.onSurface),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
        ) {
            if (working) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.documents_adding))
            } else {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.documents_add))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.documents_choose)) }, onClick = { open = false; onChoose() })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.documents_take_photo)) },
                onClick = {
                    open = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) onTake()
                    else askCamera.launch(Manifest.permission.CAMERA)
                },
            )
        }
        DropdownMenu(expanded = refused, onDismissRequest = { refused = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.documents_camera_off)) }, onClick = { refused = false; onChoose() })
        }
    }
}

/** Type, name and date. A new document's dialog can also take it back off ([onDiscard]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsDialog(
    document: StoredDocument,
    isNew: Boolean,
    onSave: (DocumentType, String, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    var type by rememberSaveable(document.id) { mutableStateOf(document.type) }
    var name by rememberSaveable(document.id) { mutableStateOf(document.name) }
    var date by rememberSaveable(document.id) { mutableStateOf(document.date) }
    var typeOpen by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val clean = VehicleDocuments.cleanName(name)
    if (picking) {
        DatePickerSheet(
            initial = date ?: LocalDate.now(),
            onPick = {
                date = it
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.documents_added else R.string.documents_edit_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = typeOpen, onExpandedChange = { typeOpen = it }) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.documents_what_it_is)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                        DocumentType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    // A name still as it came from the file, or the old type's own, follows the type.
                                    if (VehicleDocuments.cleanName(name) == null || name == type.label) name = t.label
                                    type = t
                                    typeOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= VehicleDocuments.MAX_NAME) name = it },
                    label = { Text(stringResource(R.string.documents_name)) },
                    singleLine = true,
                    isError = clean == null,
                    supportingText = if (clean == null) ({ Text(stringResource(R.string.documents_give_name)) }) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.choose_date)) { picking = true },
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.documents_date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date?.format(longDate) ?: stringResource(R.string.none), style = MaterialTheme.typography.bodyLarge)
                    }
                    if (date != null) TextButton(onClick = { date = null }) { Text(stringResource(R.string.clear)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { clean?.let { onSave(type, it, date) } }, enabled = clean != null) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            if (isNew) {
                TextButton(onClick = onDiscard, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.documents_dont_keep)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Hands a decrypted copy to another app to view, or to the share sheet. False if nothing on the phone can take it. */
internal fun Context.hand(copy: File, format: DocumentFormat, share: Boolean): Boolean {
    val uri = FileProvider.getUriForFile(this, "$packageName.files", copy)
    val intent = if (share) {
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null,
        )
    } else {
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, format.mimeType)
    }
    return try {
        startActivity(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** "3 documents · 4.2 MB", or "None yet". */
@Composable
fun documentsSummary(count: Int, totalBytes: Long): String =
    if (count == 0) stringResource(R.string.documents_none_yet) else pluralStringResource(R.plurals.documents_summary, count, count.asNumber(), fileSize(totalBytes))

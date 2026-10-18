package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.styledStringResource
import io.cosgriff.glovebox.ui.service.asNumber
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.ui.ClearHistoryRoute
import io.cosgriff.glovebox.ui.export.ExportSheet
import io.cosgriff.glovebox.ui.theme.PlexMono
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What clearing would delete, counted when the screen opens. */
data class ClearHistoryData(val vehicle: VehicleEntity, val imported: Int, val all: Int, val importedPhotos: Int, val allPhotos: Int)

@HiltViewModel
class ClearHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    service: ServiceDao,
    private val wipe: DataWipe,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<ClearHistoryRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    var data by mutableStateOf<ClearHistoryData?>(null)
        private set
    var clearing by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val v = vehicles.byId(vehicleId) ?: return@launch
            data = ClearHistoryData(
                vehicle = v,
                imported = service.countRecords(vehicleId, all = false),
                all = service.countRecords(vehicleId, all = true),
                importedPhotos = service.photoFilesOf(vehicleId, all = false).size,
                allPhotos = service.photoFilesOf(vehicleId, all = true).size,
            )
        }
    }

    /** No Undo: the rows go and their photo files with them. [onCleared] gets how many records went. */
    fun clear(all: Boolean, onCleared: (Int) -> Unit) {
        if (clearing || data == null) return
        clearing = true
        viewModelScope.launch { onCleared(wipe.clearHistory(vehicleId, all)) }
    }
}

private const val CONFIRM = "DELETE"

/**
 * Deletes a vehicle's service records and nothing else, so a Carfax report can be imported again
 * clean. Imported records only by default; every record is the other choice.
 */
@Composable
fun ClearHistoryScreen(onCancel: () -> Unit, onCleared: (Int) -> Unit, vm: ClearHistoryViewModel = hiltViewModel()) {
    val d = vm.data ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    // Imported only, unless there is nothing imported to clear.
    var all by rememberSaveable { mutableStateOf(d.imported == 0) }
    var typed by rememberSaveable { mutableStateOf("") }
    var exporting by rememberSaveable { mutableStateOf(false) }
    val count = if (all) d.all else d.imported
    val photos = if (all) d.allPhotos else d.importedPhotos
    val confirmed = TypedConfirmation.matches(typed, CONFIRM)
    val name = "${d.vehicle.year} ${d.vehicle.nickname ?: d.vehicle.model}"

    if (exporting) ExportSheet(vehicleId = d.vehicle.id, onDismiss = { exporting = false })

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onCancel, modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel))
        }
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(color = scheme.errorContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(28.dp), tint = scheme.error)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.clear_history_title, name),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.clear_history_intro),
                    color = scheme.onSurfaceVariant,
                )
            }

            Surface(
                Modifier.fillMaxWidth().selectableGroup(),
                color = scheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Column {
                    Option(
                        title = stringResource(R.string.clear_history_imported),
                        detail = stringResource(R.string.clear_history_imported_detail, records(d.imported)),
                        selected = !all,
                        enabled = d.imported > 0,
                        onClick = { all = false },
                    )
                    HorizontalDivider(color = scheme.outlineVariant)
                    Option(
                        title = stringResource(R.string.clear_history_all),
                        detail = stringResource(R.string.clear_history_all_detail, records(d.all)),
                        selected = all,
                        enabled = d.all > 0,
                        onClick = { all = true },
                    )
                }
            }

            Text(
                if (photos > 0) {
                    stringResource(R.string.clear_history_deletes_with_photos, records(count), pluralStringResource(R.plurals.photos, photos, photos.asNumber()))
                } else {
                    stringResource(R.string.clear_history_deletes, records(count))
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { exporting = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, scheme.onSurface),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text(stringResource(R.string.export_first))
            }

            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = {
                    Text(styledStringResource(R.string.type_word_to_confirm, CONFIRM, SpanStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)))
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = PlexMono),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmed) scheme.error else scheme.primary,
                    unfocusedBorderColor = if (confirmed) scheme.error else scheme.outline,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.clear(all, onCleared) },
                enabled = confirmed && count > 0 && !vm.clearing,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
            ) {
                Text(if (count == 0) stringResource(R.string.clear_history_nothing) else stringResource(R.string.clear_history_delete, records(count)), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun records(n: Int) = pluralStringResource(R.plurals.records, n, n.asNumber())

@Composable
private fun Option(title: String, detail: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

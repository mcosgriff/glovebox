package io.cosgriff.glovebox.ui.torque

import io.cosgriff.glovebox.ui.components.DangerTextButton
import io.cosgriff.glovebox.ui.components.InkChip
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.service.typedDecimal
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.domain.KnownSpecs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueSpecs
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.domain.VehicleText

/** What the editor hands back; [id] 0 is a new spec, [torque] null leaves the value for later. */
data class SpecDraft(
    val id: Long,
    val name: String,
    val category: TorqueCategory,
    val socketSize: String?,
    val torque: Torque?,
    val source: TorqueSource,
    val note: String?,
)

private const val MAX_NOTE = 140

/** Adds or edits one spec. [spec] null is a new one. Torque is typed in whichever unit is picked here. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpecEditorSheet(
    spec: TorqueSpecEntity?,
    /** Where the bundled list this row started from came from; null for a row that never came from one. */
    listSource: String?,
    unit: TorqueUnit,
    startCategory: TorqueCategory,
    onSave: (SpecDraft) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val stored = spec?.newtonMeters?.let(::Torque)
    var name by rememberSaveable { mutableStateOf(spec?.name.orEmpty()) }
    var categoryName by rememberSaveable { mutableStateOf((spec?.category?.let(TorqueCategory::of) ?: startCategory).name) }
    var size by rememberSaveable { mutableStateOf(spec?.socketSize.orEmpty()) }
    var entryUnitName by rememberSaveable { mutableStateOf(unit.name) }
    val entryUnit = TorqueUnit.valueOf(entryUnitName)
    var torqueText by rememberSaveable { mutableStateOf(stored?.let { TorqueSpecs.editText(it, unit) }.orEmpty()) }
    var factory by rememberSaveable { mutableStateOf(spec?.source?.let(TorqueSource::of) == TorqueSource.FACTORY) }
    var note by rememberSaveable { mutableStateOf(spec?.note.orEmpty()) }
    // A value from a bundled list that nobody has checked yet. Either switch below clears the tag.
    val reference = spec?.source?.let(TorqueSource::of) == TorqueSource.REFERENCE
    var checked by rememberSaveable { mutableStateOf(false) }

    val torque = TorqueSpecs.parse(torqueText.typedDecimal(), entryUnit)
    val torqueBad = torqueText.isNotBlank() && torque == null
    val cleanName = VehicleText.clean(name, VehicleText.MAX_SPEC)

    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (spec == null) R.string.torque_add_title else R.string.torque_edit_title),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(VehicleText.MAX_SPEC) },
                label = { Text(stringResource(R.string.torque_fastener)) },
                placeholder = { Text(stringResource(R.string.fastener_oil_drain_plug)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TorqueCategory.entries.forEach { c -> InkChip(c.label, selected = c.name == categoryName) { categoryName = c.name } }
            }

            OutlinedTextField(
                value = size,
                onValueChange = { size = it.take(30) },
                label = { Text(stringResource(R.string.torque_socket)) },
                placeholder = { Text(stringResource(R.string.torque_socket_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = torqueText,
                    onValueChange = { torqueText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(7) },
                    label = { Text(stringResource(R.string.torque_in, entryUnit.symbolWord)) },
                    supportingText = {
                        Text(
                            when {
                                torqueBad -> stringResource(R.string.torque_bad, Torque(TorqueSpecs.MAX_NEWTON_METERS).text(entryUnit))
                                torque != null -> stringResource(R.string.torque_equals, TorqueUnit.entries.filter { it != entryUnit }.joinToString(" · ") { torque.text(it) })
                                else -> stringResource(R.string.torque_leave_empty)
                            },
                        )
                    },
                    isError = torqueBad,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Switching unit keeps what was typed: it says what the number means, it doesn't convert it.
                UnitSwitch(entryUnit, onPick = { entryUnitName = it.name }, modifier = Modifier.fillMaxWidth())
            }

            if (reference) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked, role = Role.Switch, onValueChange = { checked = it }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.torque_checked), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.torque_checked_note, listSource.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = checked, onCheckedChange = null)
                }
            }

            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = factory, role = Role.Switch, onValueChange = { factory = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.torque_factory), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.torque_factory_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Switch(checked = factory, onCheckedChange = null)
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(MAX_NOTE) },
                label = { Text(stringResource(R.string.torque_note_label)) },
                placeholder = { Text(stringResource(R.string.torque_note_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        SpecDraft(
                            id = spec?.id ?: 0,
                            name = cleanName.orEmpty(),
                            category = TorqueCategory.valueOf(categoryName),
                            socketSize = VehicleText.clean(size, 30),
                            torque = torque,
                            // A source only means something once there is a value; a reference value stays one until vouched for or changed.
                            source = KnownSpecs.sourceAfterEdit(spec?.source?.let(TorqueSource::of), stored, torque, fromFactoryManual = factory, checked = checked),
                            note = VehicleText.clean(note, MAX_NOTE),
                        ),
                    )
                },
                enabled = cleanName != null && !torqueBad,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(stringResource(R.string.save))
            }

            if (spec != null) {
                DangerTextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.torque_delete), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

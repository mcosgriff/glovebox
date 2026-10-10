package io.cosgriff.glovebox.ui.history

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
) : ViewModel() {
    private val recordId = savedStateHandle.toRoute<RecordDetailRoute>().recordId

    /** Null while loading and after the record is deleted. */
    val detail: StateFlow<RecordDetail?> = service.observeRecord(recordId).mapLatest { r ->
        if (r == null) return@mapLatest null
        val vehicle = vehicles.observe(r.record.vehicleId).first()
        val tracked = schedule.allFor(r.record.vehicleId)
            .filter { it.tracked }
            .mapNotNull { it.toTracked(settings.settings.first().defaultRule) }
            .associateBy { it.id }
        val entries = service.recordsFor(r.record.vehicleId).map { it.toEntry() }
        val entry = r.toEntry()

        RecordDetail(
            record = r,
            vehicleName = vehicle?.let { "${it.year} ${it.nickname ?: it.model}" }.orEmpty(),
            lines = r.items.map { item ->
                val reminder = item.scheduleItemId?.let(tracked::get)
                val latest = reminder != null && LogEdits.isLatestFor(reminder.id, entry, entries)
                WorkLine(
                    label = item.label,
                    category = ServiceCatalog.categoryOf(item.catalogId)?.label,
                    reminderNote = when {
                        reminder == null -> null
                        latest -> "Next due " + reminder.interval.nextDueAfter(LastDone(entry.date, entry.odometer)).text()
                        else -> "Done again since"
                    },
                    current = latest,
                )
            },
            deleteImpact = LogEdits.reminderChanges(tracked.values.toList(), entries, entries.filter { it.id != entry.id })
                .map { it.text() },
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
    onEdit: (vehicleId: Long, recordId: Long) -> Unit,
    vm: RecordDetailViewModel = hiltViewModel(),
) {
    val detail = vm.detail.collectAsStateWithLifecycle().value ?: return
    val record = detail.record.record
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Service record", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { onEdit(record.vehicleId, record.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Edit")
            }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WhiteCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${detail.vehicleName} · ${if (record.diy) "DIY" else record.shopName ?: "Shop"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(record.date.format(longDate), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row {
                        Stat("Odometer", record.odometer.asMiles(), Modifier.weight(1f))
                        Stat("Total cost", record.costCents?.let(Money::format) ?: "Not entered", Modifier.weight(1f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val n = detail.lines.size
                SectionLabel("WORK DONE · $n ${if (n == 1) "ITEM" else "ITEMS"}")
                WhiteCard {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        detail.lines.forEachIndexed { i, line ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(line.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        line.category ?: "Custom",
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
                    SectionLabel("NOTES")
                    WhiteCard { Text(notes, Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
                }
            }

            OutlinedButton(
                onClick = { confirmingDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Delete record") }
        }
    }

    if (confirmingDelete) {
        ModalBottomSheet(onDismissRequest = { confirmingDelete = false }) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Delete this service record?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val n = detail.lines.size
                    Text(
                        "${record.date.format(longDate)} · ${record.odometer.asMiles()} · $n ${if (n == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (detail.deleteImpact.isEmpty()) {
                    Text("No reminders change.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "REMINDERS WILL GO BACK TO",
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { confirmingDelete = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            confirmingDelete = false
                            vm.delete(onBack)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) { content() }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
    }
}

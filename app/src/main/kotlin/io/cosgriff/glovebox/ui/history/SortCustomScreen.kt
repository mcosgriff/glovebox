package io.cosgriff.glovebox.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.CatalogRuleDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.toRule
import io.cosgriff.glovebox.domain.CustomGroup
import io.cosgriff.glovebox.domain.CustomLines
import io.cosgriff.glovebox.domain.SavedLine
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.SuggestionSource
import io.cosgriff.glovebox.domain.categoryOf
import io.cosgriff.glovebox.ui.SortCustomRoute
import io.cosgriff.glovebox.ui.components.EmptyState
import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.route
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.catalogCategoryLabel
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.monthYear
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.cosgriff.glovebox.ui.components.CheckRow
import io.cosgriff.glovebox.ui.service.FileUnderDialog
import io.cosgriff.glovebox.ui.service.LineChoice
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import javax.inject.Inject

/** A vehicle's lines as sorting Custom lines sees them. [recallRecords] are the records recalls marked done added. */
internal fun List<ServiceRecordWithItems>.savedLines(recallRecords: Set<Long>): List<SavedLine> =
    flatMap { r ->
        r.items.map { SavedLine(it.id, r.record.id, r.record.date, it.label, it.catalogId, it.scheduleItemId, recall = r.record.id in recallRecords) }
    }

/** The records these recalls' "mark done" added. */
internal fun List<RecallEntity>.recordIds(): Set<Long> = mapNotNull { it.serviceRecordId }.toSet()

data class SortCustomState(val vehicle: VehicleEntity, val groups: List<CustomGroup>)

/** Every catalog line, by category, to pick from. Only the catalog's: a Custom line is filed under one of those. */
internal val catalogChoices: List<LineChoice> by lazy {
    ServiceCatalog.forVehicle(emptyList()).flatMap { c -> c.options.map { LineChoice(it, c) } }
}

@HiltViewModel
class SortCustomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    service: ServiceDao,
    recalls: RecallDao,
    rules: CatalogRuleDao,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<SortCustomRoute>().vehicleId

    /** The user's answer for a group, by key: a line, or null to leave it under Custom. A group not here keeps its suggestion. */
    val chosen = mutableStateMapOf<String, String?>()

    /** "Remember this for future imports", by key; ticked unless here as false. */
    val remember = mutableStateMapOf<String, Boolean>()

    fun choose(key: String, catalogId: String?) {
        chosen[key] = catalogId
    }

    fun remember(key: String, on: Boolean) {
        remember[key] = on
    }

    /** Null while loading, and if the vehicle is gone. Groups go as their lines are filed. */
    val state: StateFlow<SortCustomState?> =
        combine(vehicles.observe(vehicleId), service.observeRecords(vehicleId), recalls.observe(vehicleId), rules.observeAll()) { v, records, recallRows, ruleRows ->
            v?.let { SortCustomState(it, CustomLines.groups(records.savedLines(recallRows.recordIds()), ruleRows.map { r -> r.toRule() })) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Sorts a vehicle's Custom lines in bulk (#310): each wording once, with the line it looks like. */
@Composable
fun SortCustomScreen(onBack: () -> Unit, vm: SortCustomViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return // first frame while loading
    SortCustomContent(s.groups, vm.chosen, vm.remember, onChoose = vm::choose, onRemember = vm::remember, onBack = onBack)
}

/** The sort screen as given, with no ViewModel: what [SortCustomScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun SortCustomContent(
    groups: List<CustomGroup>,
    /** By group key: the line chosen, or null for "leave under Custom"; a group not here keeps its suggestion. */
    chosen: Map<String, String?>,
    /** By group key: false where "Remember this" was unticked. */
    remember: Map<String, Boolean>,
    onChoose: (key: String, catalogId: String?) -> Unit,
    onRemember: (key: String, on: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    // The group whose line is being picked, by key.
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(stringResource(R.string.sort_custom_title), onBack, subtitle = stringResource(R.string.sort_custom_about)) }
        if (groups.isEmpty()) item { EmptyState(stringResource(R.string.sort_custom_none), title = stringResource(R.string.sort_custom_none_title)) }
        items(groups, key = { it.key }) { group ->
            GroupCard(
                group,
                picked = CustomLines.picked(group, chosen),
                remember = remember[group.key] != false,
                onPick = { picking = group.key },
                onLeave = { onChoose(group.key, null) },
                onRemember = { onRemember(group.key, it) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
    groups.firstOrNull { it.key == picking }?.let { group ->
        FileUnderDialog(
            label = group.label,
            lines = catalogChoices,
            onPick = { choice ->
                onChoose(group.key, choice.option.id)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun GroupCard(
    group: CustomGroup,
    picked: String?,
    remember: Boolean,
    onPick: () -> Unit,
    onLeave: () -> Unit,
    onRemember: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    FilledCard(modifier, padding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(group.label, style = MaterialTheme.typography.titleMedium)
            Text(groupDetail(group), style = MaterialTheme.typography.bodySmall, color = muted)
            if (picked == null) {
                Text(stringResource(R.string.sort_custom_stays), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyLarge, color = muted)
                val pickFor = stringResource(R.string.sort_custom_pick_for, group.label)
                TextButton(
                    onClick = onPick,
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp).heightIn(min = 48.dp).semantics { contentDescription = pickFor },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(stringResource(R.string.sort_custom_pick))
                }
            } else {
                Picked(picked, group.suggestion?.takeIf { it.catalogId == picked }?.source)
                // Each group has these, so TalkBack hears which group's they are. No minimum width: Material's
                // would centre a short label out of line with the card's text.
                val changeFor = stringResource(R.string.sort_custom_change_for, group.label)
                val leaveFor = stringResource(R.string.sort_custom_leave_for, group.label)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(
                        onClick = onPick,
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp).heightIn(min = 48.dp).semantics { contentDescription = changeFor },
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text(stringResource(R.string.sort_custom_change))
                    }
                    TextButton(
                        onClick = onLeave,
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp).heightIn(min = 48.dp).semantics { contentDescription = leaveFor },
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text(stringResource(R.string.sort_custom_leave))
                    }
                }
                if (CustomLines.asksToRemember(group, picked)) {
                    val rememberFor = stringResource(R.string.sort_custom_remember_for, group.label)
                    CheckRow(
                        stringResource(R.string.sort_custom_remember),
                        checked = remember,
                        modifier = Modifier.semantics { contentDescription = rememberFor },
                        onChange = onRemember,
                    )
                }
            }
        }
    }
}

/** "3 lines · Mar 2021 to Jun 2023", or one month when they are all in it. */
@Composable
private fun groupDetail(group: CustomGroup): String {
    val lines = pluralStringResource(R.plurals.sort_custom_lines, group.count, group.count.asNumber())
    val first = group.dates.first().format(monthYear)
    val last = group.dates.last().format(monthYear)
    return if (first == last) stringResource(R.string.sort_custom_detail_one, lines, first) else stringResource(R.string.sort_custom_detail, lines, first, last)
}

/** The line picked, and where it came from: [source] when it is the suggestion, otherwise the user's own pick. */
@Composable
private fun Picked(catalogId: String, source: SuggestionSource?) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(lineWithCategory(catalogId), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(
                when (source) {
                    SuggestionSource.YOUR_RULE -> R.string.sort_custom_from_rule
                    SuggestionSource.GLOVEBOX -> R.string.sort_custom_from_wording
                    null -> R.string.sort_custom_from_you
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Water pump · Cooling" */
@Composable
internal fun lineWithCategory(catalogId: String): String =
    stringResource(R.string.sort_custom_line, catalogLabel(catalogId), ServiceCatalog.categoryOf(catalogId)?.id?.let(::catalogCategoryLabel).orEmpty())

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
import io.cosgriff.glovebox.domain.LineSuggestion
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
import javax.inject.Inject

/** A vehicle's lines as sorting Custom lines sees them. [recallRecords] are the records recalls marked done added. */
internal fun List<ServiceRecordWithItems>.savedLines(recallRecords: Set<Long>): List<SavedLine> =
    flatMap { r ->
        r.items.map { SavedLine(it.id, r.record.id, r.record.date, it.label, it.catalogId, it.scheduleItemId, recall = r.record.id in recallRecords) }
    }

/** The records these recalls' "mark done" added. */
internal fun List<RecallEntity>.recordIds(): Set<Long> = mapNotNull { it.serviceRecordId }.toSet()

data class SortCustomState(val vehicle: VehicleEntity, val groups: List<CustomGroup>)

@HiltViewModel
class SortCustomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    service: ServiceDao,
    recalls: RecallDao,
    rules: CatalogRuleDao,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<SortCustomRoute>().vehicleId

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
    SortCustomContent(s.groups, onBack)
}

/** The sort screen as given, with no ViewModel: what [SortCustomScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun SortCustomContent(groups: List<CustomGroup>, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(stringResource(R.string.sort_custom_title), onBack, subtitle = stringResource(R.string.sort_custom_about)) }
        if (groups.isEmpty()) item { EmptyState(stringResource(R.string.sort_custom_none), title = stringResource(R.string.sort_custom_none_title)) }
        items(groups, key = { it.key }) { GroupCard(it, Modifier.padding(horizontal = 20.dp)) }
    }
}

@Composable
private fun GroupCard(group: CustomGroup, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    FilledCard(modifier, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(group.label, style = MaterialTheme.typography.titleMedium)
            Text(groupDetail(group), style = MaterialTheme.typography.bodySmall, color = muted)
            group.suggestion?.let { Suggested(it) }
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

@Composable
private fun Suggested(suggestion: LineSuggestion) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(lineWithCategory(suggestion.catalogId), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(if (suggestion.source == SuggestionSource.YOUR_RULE) R.string.sort_custom_from_rule else R.string.sort_custom_from_wording),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Water pump · Cooling" */
@Composable
internal fun lineWithCategory(catalogId: String): String =
    stringResource(R.string.sort_custom_line, catalogLabel(catalogId), ServiceCatalog.categoryOf(catalogId)?.id?.let(::catalogCategoryLabel).orEmpty())

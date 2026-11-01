package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ShopRecommendationDao
import io.cosgriff.glovebox.data.db.toModel
import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.domain.LoggedLine
import io.cosgriff.glovebox.domain.ShopRecommendation
import io.cosgriff.glovebox.domain.ShopRecommendations
import io.cosgriff.glovebox.ui.ShopRecommendedRoute
import io.cosgriff.glovebox.ui.components.RecommendationHead
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.route
import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A closed recommendation and how it was closed: by the owner, or by a record of the work (#572). */
data class ClosedRecommendation(
    val recommendation: ShopRecommendation,
    /** The line of the record that did the work, as it reads now; null when closed by hand or nothing matches now. */
    val doneLine: String? = null,
    /** Who did the work: the record's shop, or null for the owner. */
    val doneShop: String? = null,
)

/** The vehicle's kept recommendations (#572, board RecommendedList): the open ones, red first, and those closed. */
data class ShopRecommended(
    val vehicleName: String,
    val open: List<ShopRecommendation>,
    val closed: List<ClosedRecommendation>,
)

@HiltViewModel
class ShopRecommendedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    logs: VehicleLogs,
    recommendations: ShopRecommendationDao,
    private val actions: RecordActions,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<ShopRecommendedRoute>().vehicleId

    /** Null while loading, and once the vehicle has gone. */
    val state: StateFlow<ShopRecommended?> = combine(logs.observe(vehicleId), recommendations.observe(vehicleId)) { log, rows ->
        log ?: return@combine null
        val all = rows.map { it.toModel() }
        val records = log.records.associateBy { it.record.id }
        // A replaced one isn't closed: the newer one from the same shop stands for it.
        val closed = all.filter { !it.open && it.replacedBy == null }.map { r ->
            val record = r.doneBy?.let(records::get)
            ClosedRecommendation(
                r,
                doneLine = record?.let { rec -> ShopRecommendations.doneLine(r, rec.items.map { LoggedLine(it.catalogId, it.label) })?.label },
                doneShop = record?.record?.takeIf { !it.diy }?.shopName,
            )
        }.sortedByDescending { it.recommendation.closedOn ?: it.recommendation.doneOn }
        ShopRecommended(log.vehicle.shortName(), ShopRecommendations.ordered(all.filter { it.open }), closed)
    }.screenState(viewModelScope, null)

    /** Closed by hand: logs nothing and moves no reminder; the bar offers Undo. */
    fun close(r: ShopRecommendation) {
        viewModelScope.launch { actions.closeRecommendation(r.id, r.item.text, LocalDate.now()) }
    }
}

@Composable
fun ShopRecommendedScreen(onBack: () -> Unit, vm: ShopRecommendedViewModel = hiltViewModel()) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return
    ShopRecommendedContent(state, onBack, onClose = vm::close)
}

/** The list as given, with no ViewModel: what [ShopRecommendedScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun ShopRecommendedContent(state: ShopRecommended, onBack: () -> Unit, onClose: (ShopRecommendation) -> Unit) {
    var showClosed by rememberSaveable { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.recommended_title), onBack, subtitle = state.vehicleName)
        Column(Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(stringResource(R.string.recommended_about), style = MaterialTheme.typography.bodyMedium, color = muted)
            val (flagged, rest) = state.open.partition { it.item.colour != null }
            if (state.open.isEmpty()) Text(stringResource(R.string.recommended_none), style = MaterialTheme.typography.bodyLarge)
            if (flagged.isNotEmpty()) Group(pluralStringResource(R.plurals.recommended_inspections, flagged.size, flagged.size.asNumber()), flagged, onClose)
            if (rest.isNotEmpty()) Group(pluralStringResource(R.plurals.recommended_work, rest.size, rest.size.asNumber()), rest, onClose)
            if (state.closed.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { showClosed = !showClosed },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel(pluralStringResource(R.plurals.recommended_closed, state.closed.size, state.closed.size.asNumber()), Modifier.weight(1f))
                        Text(
                            stringResource(if (showClosed) R.string.recommended_hide else R.string.recommended_show),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (showClosed) {
                        RecommendationsCard {
                            state.closed.forEachIndexed { i, c ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                ClosedRow(c)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, items: List<ShopRecommendation>, onClose: (ShopRecommendation) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        RecommendationsCard {
            items.forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OpenRow(r, onClose = { onClose(r) })
            }
        }
    }
}

@Composable
private fun RecommendationsCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) { content() }
    }
}

@Composable
private fun OpenRow(r: ShopRecommendation, onClose: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RecommendationHead(r.item)
        val date = r.date.format(longDate)
        Text(r.shop?.let { stringResource(R.string.recommended_from, it, date) } ?: date, style = MaterialTheme.typography.bodySmall, color = muted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                r.line?.let { stringResource(R.string.recommended_comes_off, catalogLabel(it)) } ?: stringResource(R.string.recommended_comes_off_wording),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            val closeFor = stringResource(R.string.recommended_close_for, r.item.text)
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = closeFor }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(R.string.recommended_close), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ClosedRow(c: ClosedRecommendation) {
    val r = c.recommendation
    val closedOn = r.closedOn
    val doneOn = r.doneOn
    val (how, colour) = when {
        closedOn != null -> stringResource(R.string.recommended_closed_by_you, closedOn.format(longDate)) to MaterialTheme.colorScheme.onSurfaceVariant
        doneOn != null -> {
            val date = doneOn.format(longDate)
            val line = c.doneLine
            val shop = c.doneShop
            when {
                line == null -> stringResource(R.string.recommended_done, date)
                shop != null -> stringResource(R.string.recommended_done_by_shop, date, line, shop)
                else -> stringResource(R.string.recommended_done_by_you, date, line)
            } to statusColors().ok
        }
        else -> return
    }
    Column(Modifier.heightIn(min = 56.dp).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(r.item.text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = statusColors().soon)
        Text(how, style = MaterialTheme.typography.bodySmall, color = colour)
    }
}

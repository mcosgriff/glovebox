package io.cosgriff.glovebox.ui.bulletins

import android.content.res.Resources
import io.cosgriff.glovebox.ui.components.openWebPage
import android.widget.Toast
import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.Bulletin
import io.cosgriff.glovebox.domain.BulletinKind
import io.cosgriff.glovebox.domain.Bulletins
import io.cosgriff.glovebox.domain.VehicleBulletins
import io.cosgriff.glovebox.ui.BulletinsRoute
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.Pill
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.route
import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.theme.PlexMono
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** What the Bulletins screen shows: the vehicle's name, and its year's bulletins if Glovebox has a list for the model. */
data class BulletinsState(val vehicleName: String, val bulletins: VehicleBulletins?)

@HiltViewModel
class BulletinsViewModel @Inject constructor(savedStateHandle: SavedStateHandle, vehicles: VehicleDao, catalog: BulletinCatalog) : ViewModel() {
    private val vehicleId = savedStateHandle.route<BulletinsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<BulletinsState?> = combine(vehicles.observe(vehicleId), catalog.forVehicle(vehicles.observe(vehicleId))) { v, bulletins ->
        v?.let { BulletinsState(it.shortName(), bulletins) }
    }.screenState(viewModelScope, null)
}

/** The maker's free-repair bulletins for a vehicle (#459). */
@Composable
fun BulletinsScreen(onBack: () -> Unit, vm: BulletinsViewModel = hiltViewModel()) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val context = LocalContext.current
    val res = LocalResources.current
    BulletinsContent(state, onBack, onOpenPdf = { url ->
        if (!context.openWebPage(url)) Toast.makeText(context, res.getString(R.string.licences_no_browser, url), Toast.LENGTH_LONG).show()
    })
}

/** The Bulletins screen as given, with no ViewModel: what the screenshot tests draw. */
@Composable
internal fun BulletinsContent(state: BulletinsState, onBack: () -> Unit, onOpenPdf: (url: String) -> Unit) {
    val list = state.bulletins
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenHeader(stringResource(R.string.bulletins_title), onBack, subtitle = state.vehicleName) }
        item {
            Column(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // These are the maker's notes to its dealers; whether a given VIN is included is the dealer's to say.
                Text(stringResource(R.string.bulletins_intro), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.bulletins_end_note), style = MaterialTheme.typography.bodyMedium)
                list?.let {
                    Text(
                        stringResource(R.string.bulletins_as_of, it.asOf.format(longDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        when {
            list == null -> item { Note(stringResource(R.string.bulletins_not_covered)) }
            list.bulletins.isEmpty() -> item { Note(stringResource(R.string.bulletins_none)) }
            else -> items(list.bulletins, key = { it.id }) { b ->
                BulletinCard(b, onOpenPdf, Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp))
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BulletinCard(b: Bulletin, onOpenPdf: (String) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable(b.id) { mutableStateOf(false) }
    // Only offer to expand when the collapsed card hides something.
    var clipped by rememberSaveable(b.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    b.number,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Neutral: a bulletin is something to ask a dealer about, not something overdue.
                Pill(stringResource(b.kind.labelRes), container = scheme.surfaceVariant, content = scheme.onSurfaceVariant, small = true)
            }
            // Read from the maker's words (Bulletins.endedOn): the free repair may no longer be on offer.
            remember(b.id) { Bulletins.endedOn(b.summary) }?.let { ended ->
                Text(stringResource(R.string.bulletins_ended, ended.format(longDate)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            if (b.byWording) {
                Text(stringResource(R.string.bulletins_by_wording), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Text(
                b.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) clipped = clipped || it.hasVisualOverflow },
            )
            val res = LocalResources.current
            val products = b.products.joinToString(", ") { res.getString(R.string.bulletins_product, res.yearRuns(it.years), it.model) }
            Text(
                b.date?.let { stringResource(R.string.bulletins_sent_for, it.format(longDate), products) } ?: stringResource(R.string.bulletins_for, products),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (clipped) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(stringResource(if (expanded) R.string.bulletins_show_less else R.string.bulletins_show_full))
                }
            }
            b.pdf?.let { url ->
                InkOutlinedButton(onClick = { onOpenPdf(url) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.bulletins_open_pdf, b.number), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** 2016, 2017, 2018, 2020 reads "2016–2018, 2020". Model years, so no grouping separators. */
private fun Resources.yearRuns(years: List<Int>): String = Bulletins.yearRuns(years).joinToString(", ") {
    if (it.first == it.last) "${it.first}" else getString(R.string.bulletins_year_run, "${it.first}", "${it.last}")
}

val BulletinKind.labelRes: Int
    get() = when (this) {
        BulletinKind.SERVICE_CAMPAIGN -> R.string.bulletins_kind_campaign
        BulletinKind.WARRANTY_EXTENSION -> R.string.bulletins_kind_warranty
        BulletinKind.OVER_THE_AIR -> R.string.bulletins_kind_over_the_air
    }

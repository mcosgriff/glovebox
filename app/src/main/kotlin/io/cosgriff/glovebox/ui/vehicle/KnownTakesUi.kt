package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.domain.enumOrNull
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.KnownTakes
import io.cosgriff.glovebox.domain.KnownTakesFile
import io.cosgriff.glovebox.domain.KnownTakesList
import io.cosgriff.glovebox.domain.KnownTakesPlan
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.whatItTakes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "What it takes" lists that ship with the app: every file in `assets/known-takes`, read once.
 * What a list is and may hold is `KnownTakes` in `:domain`, where a test reads these same files; here
 * one that doesn't parse is simply left out.
 */
@Singleton
class KnownTakesCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    private val loaded: List<KnownTakesList> by lazy {
        try {
            context.assets.list(KnownTakes.ASSET_DIR).orEmpty().sorted().mapNotNull { name ->
                val text = context.assets.open("${KnownTakes.ASSET_DIR}/$name").bufferedReader().use { it.readText() }
                (KnownTakes.parse(text) as? KnownTakesFile.Ok)?.list
            }
        } catch (_: IOException) {
            emptyList()
        }
    }

    suspend fun lists(): List<KnownTakesList> = withContext(Dispatchers.IO) { loaded }
}

/** The rows of a vehicle filled from a list that are still tagged DOUBLE-CHECK. */
fun VehicleEntity.takesToCheck(): Set<TakesItem> =
    takesToCheck?.split(',')?.mapNotNull { enumOrNull<TakesItem>(it) }?.toSet().orEmpty()

private fun Set<TakesItem>.stored(): String? = TakesItem.entries.filter { it in this }.joinToString(",") { it.name }.ifEmpty { null }

/** What a vehicle's "What it takes" has to do with the lists that ship. */
data class TakesKnown(
    /** The list for this vehicle, if one covers it. */
    val list: KnownTakesList? = null,
    /** What filling from [list] would do; null when it would fill nothing. */
    val offer: KnownTakesPlan? = null,
    /** Rows filled from a list and not yet checked or changed. */
    val toCheck: Set<TakesItem> = emptySet(),
    /** The list the [toCheck] rows came from, named once on the screen; null when none are left to check. */
    val toCheckFrom: KnownTakesList? = null,
)

/**
 * Offering a maker's list for a vehicle's "What it takes", filling from it, and keeping track of which
 * filled rows are still to be checked. Vehicle details and Fluids, filters & parts both use it.
 */
class KnownTakesRows @Inject constructor(private val catalog: KnownTakesCatalog, private val vehicles: VehicleDao) {
    fun observe(vehicle: Flow<VehicleEntity?>): Flow<TakesKnown> = combine(vehicle, flow { emit(catalog.lists()) }) { v, lists ->
        if (v == null) return@combine TakesKnown()
        val list = KnownTakes.match(lists, v.year, v.make, v.model, v.engine, v.fuel)
        val offer = list?.let { KnownTakes.plan(KnownTakes.forVehicle(it, v.drivetrain, v.transmission), v.whatItTakes()) }?.takeIf { it.filled.isNotEmpty() }
        val toCheck = v.takesToCheck()
        TakesKnown(list, offer, toCheck, v.takesKnownList?.let { id -> lists.firstOrNull { it.id == id } }.takeIf { toCheck.isNotEmpty() })
    }

    /** Fills [v]'s empty rows from its list, as planned from what is stored now, and tags each to check. */
    suspend fun fill(v: VehicleEntity) {
        val list = KnownTakes.match(catalog.lists(), v.year, v.make, v.model, v.engine, v.fuel) ?: return
        val plan = KnownTakes.plan(KnownTakes.forVehicle(list, v.drivetrain, v.transmission), v.whatItTakes())
        if (plan.filled.isEmpty()) return
        vehicles.fillTakes(v.id, plan.result, plan.filled, list.id, (v.takesToCheck() + plan.filled).stored())
    }

    /** [item]'s editor saving [after] over [v]'s rows: the row is written, and its tag settled ([saved]). */
    suspend fun save(v: VehicleEntity, item: TakesItem, after: WhatItTakes, checked: Boolean) {
        saved(v, item, after, checked)
        vehicles.setTakes(v.id, after, item)
    }

    /**
     * After [item]'s editor saved [after] over [v]'s rows: the tag stays only while the owner neither
     * said they [checked] it nor changed it ([KnownTakes.stillToCheck]).
     */
    private suspend fun saved(v: VehicleEntity, item: TakesItem, after: WhatItTakes, checked: Boolean) {
        val toCheck = v.takesToCheck()
        if (item !in toCheck || KnownTakes.stillToCheck(v.whatItTakes().summary(item), after.summary(item), checked)) return
        vehicles.setTakesToCheck(v.id, (toCheck - item).stored())
    }
}

/** The offer of a maker's list: whose figures they are and that each is to be checked, every time it is shown. */
@Composable
fun KnownTakesOffer(list: KnownTakesList, plan: KnownTakesPlan, onFill: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InkOutlinedButton(onClick = onFill, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.known_takes_fill, list.title))
        }
        val offer = stringResource(
            R.string.known_takes_offer,
            pluralStringResource(R.plurals.known_takes_rows, plan.filled.size, plan.filled.size.asNumber()),
            list.source.name,
            list.source.note,
        )
        Text(
            if (plan.kept.isEmpty()) offer else stringResource(R.string.known_interval_offer_kept, offer, pluralStringResource(R.plurals.known_takes_kept, plan.kept.size, plan.kept.size.asNumber())),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** Said once on a screen with rows still tagged: where they came from, and to check them. */
@Composable
fun KnownTakesNote(from: KnownTakesList, modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.known_takes_note, from.source.name, from.title),
        modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** In an editor of a row still tagged: the owner saying they have checked it against their manual. */
@Composable
internal fun CheckedAgainstManual(checked: MutableState<Boolean>) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked.value, role = Role.Checkbox, onValueChange = { checked.value = it }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked.value, onCheckedChange = null)
        Text(stringResource(R.string.known_takes_checked), style = MaterialTheme.typography.bodyMedium)
    }
}

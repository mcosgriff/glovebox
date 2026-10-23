package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.service.subtitle
import io.cosgriff.glovebox.ui.components.EmptyState
import io.cosgriff.glovebox.ui.components.DoubleCheckTag
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.BorderedCard
import io.cosgriff.glovebox.ui.components.orNullIfBlank
import io.cosgriff.glovebox.ui.components.copyText
import io.cosgriff.glovebox.ui.components.CheckedField
import io.cosgriff.glovebox.ui.components.Chevron
import androidx.annotation.StringRes
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.line
import io.cosgriff.glovebox.ui.service.label
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.ui.FluidsPartsRoute
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.whatItTakes
import io.cosgriff.glovebox.ui.theme.PlexMono
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FluidsPartsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicles: VehicleDao,
    private val knownTakes: KnownTakesRows,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<FluidsPartsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val vehicle: StateFlow<VehicleEntity?> = vehicles.observe(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A maker's list for this vehicle's "What it takes", and the rows filled from one still to check. */
    val known: StateFlow<TakesKnown> = knownTakes.observe(vehicles.observe(vehicleId))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TakesKnown())

    fun fillKnown() = viewModelScope.launch { vehicle.value?.let { knownTakes.fill(it) } }

    /**
     * One of the rows past oil, tires and wipers; [values] are its fields in order, each checked, null for
     * empty. [checked] is the owner saying they checked a row filled from a list against their manual.
     */
    fun save(item: TakesItem, values: List<Any?>, checked: Boolean) = viewModelScope.launch {
        val v = vehicle.value ?: return@launch
        val before = v.whatItTakes()
        val after = moreFields(item, before).zip(values).fold(before) { w, (field, value) -> field.write(w, value) }
        knownTakes.save(v, item, after, checked)
    }
}

/**
 * What the vehicle takes past oil, tires and wipers, laid out like Torque & fastener specs: a card
 * for each one entered, then a card for each still to add, which opens its editor in one tap.
 */
@Composable
fun FluidsPartsScreen(onBack: () -> Unit, vm: FluidsPartsViewModel = hiltViewModel()) {
    val v = vm.vehicle.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val takes = v.whatItTakes()
    val known by vm.known.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    var editing by rememberSaveable { mutableStateOf<TakesItem?>(null) }
    editing?.let { item ->
        MoreDialog(item, takes, toCheck = item in known.toCheck, onSave = { values, ok -> vm.save(item, values, ok); editing = null }, onDismiss = { editing = null })
    }

    val entered = takes.entered()
    val notEntered = takes.notEntered()
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(stringResource(R.string.fluids_title), onBack, subtitle = v.subtitle())
        }
        if (entered.isEmpty()) {
            item {
                EmptyState(stringResource(R.string.fluids_empty_body))
            }
        }
        known.toCheckFrom?.let { from -> item { KnownTakesNote(from, Modifier.padding(horizontal = 20.dp)) } }
        items(entered, key = { it.name }) { item ->
            TakesCard(item, takes, toCheck = item in known.toCheck, onClick = { editing = item }, modifier = Modifier.padding(horizontal = 20.dp))
        }
        known.list?.let { list -> known.offer?.let { plan -> item { KnownTakesOffer(list, plan, onFill = vm::fillKnown, Modifier.padding(horizontal = 20.dp)) } } }
        if (notEntered.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.fluids_add_section), Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp)) }
            items(notEntered, key = { it.name }) { item ->
                TakesCard(item, takes, toCheck = false, onClick = { editing = item }, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
        item {
            Text(
                stringResource(R.string.fluids_note),
                Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** One row of what it takes as a card: its value, or "Tap to add" when nothing is entered. A long press copies its part numbers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TakesCard(item: TakesItem, takes: WhatItTakes, toCheck: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val value = takes.line(item)
    val parts = takes.partNumbers(item)
    val partsLabel = stringResource(R.string.details_part_numbers)
    BorderedCard(modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClickLabel = parts?.let { stringResource(R.string.details_copy_parts) },
                    onLongClick = parts?.let { p -> { copyText(context, partsLabel, p, sensitive = false); haptics.performHapticFeedback(HapticFeedbackType.LongPress) } },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (toCheck) DoubleCheckTag()
                value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono) }
            }
            if (value == null) {
                Text(
                    stringResource(R.string.fluids_tap_to_add),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onPrimaryContainer,
                )
            } else {
                Chevron()
            }
        }
    }
}

/** One field of a row past oil, tires and wipers: how it is labelled, and the check that gives its stored form. */
private class MoreField(
    @StringRes val label: Int,
    @StringRes val error: Int,
    val check: (String) -> Any?,
    val current: Any?,
    /** Puts this field's checked value (null for empty) into the vehicle's "What it takes". */
    val write: (WhatItTakes, Any?) -> WhatItTakes,
    @StringRes val supporting: Int? = null,
    val keyboard: KeyboardType = KeyboardType.Text,
)

private fun moreFields(item: TakesItem, w: WhatItTakes): List<MoreField> {
    val name = R.string.fluids_name_error
    val part = R.string.oil_filter_error
    return when (item) {
        TakesItem.TRANSMISSION -> listOf(MoreField(R.string.fluids_fluid, name, VehicleSpecEntry::fluidName, w.transmissionFluid, { t, x -> t.copy(transmissionFluid = x as String?) }, R.string.fluids_transmission_note))
        TakesItem.DIFFERENTIAL -> listOf(MoreField(R.string.fluids_oil, R.string.fluids_gear_error, VehicleSpecEntry::gearOil, w.differentialOil, { t, x -> t.copy(differentialOil = x as String?) }, R.string.fluids_differential_note))
        TakesItem.TRANSFER_CASE -> listOf(MoreField(R.string.fluids_oil, R.string.fluids_gear_error, VehicleSpecEntry::gearOil, w.transferCaseOil, { t, x -> t.copy(transferCaseOil = x as String?) }, R.string.fluids_transfer_note))
        TakesItem.COOLANT -> listOf(MoreField(R.string.takes_coolant, name, VehicleSpecEntry::fluidName, w.coolant, { t, x -> t.copy(coolant = x as String?) }, R.string.fluids_coolant_note))
        TakesItem.BRAKE_FLUID -> listOf(MoreField(R.string.oil_grade, R.string.fluids_brake_error, VehicleSpecEntry::brakeFluid, w.brakeFluid, { t, x -> t.copy(brakeFluid = x as String?) }, R.string.fluids_brake_note))
        TakesItem.AIR_FILTERS -> listOf(
            MoreField(R.string.fluids_engine_air, part, VehicleSpecEntry::partNumbers, w.engineAirFilter, { t, x -> t.copy(engineAirFilter = x as String?) }),
            MoreField(R.string.fluids_cabin_air, part, VehicleSpecEntry::partNumbers, w.cabinAirFilter, { t, x -> t.copy(cabinAirFilter = x as String?) }),
        )
        TakesItem.SPARK_PLUGS -> listOf(
            MoreField(R.string.fluids_part_number, part, VehicleSpecEntry::partNumbers, w.sparkPlug, { t, x -> t.copy(sparkPlug = x as String?) }),
            MoreField(R.string.fluids_gap, R.string.fluids_gap_error, VehicleSpecEntry::plugGap, w.sparkPlugGap, { t, x -> t.copy(sparkPlugGap = x as String?) }, R.string.fluids_gap_note),
        )
        TakesItem.BATTERY -> listOf(
            MoreField(R.string.fluids_group, R.string.fluids_group_error, VehicleSpecEntry::batteryGroup, w.batteryGroup, { t, x -> t.copy(batteryGroup = x as String?) }, R.string.fluids_group_note),
            MoreField(R.string.fluids_cca, R.string.fluids_cca_error, VehicleSpecEntry::cca, w.batteryCca, { t, x -> t.copy(batteryCca = x as Int?) }, keyboard = KeyboardType.Number),
        )
        TakesItem.BULBS -> listOf(
            MoreField(R.string.fluids_low_beam, R.string.fluids_low_beam_error, VehicleSpecEntry::bulbCode, w.bulbLow, { t, x -> t.copy(bulbLow = x as String?) }),
            MoreField(R.string.fluids_high_beam, R.string.fluids_high_beam_error, VehicleSpecEntry::bulbCode, w.bulbHigh, { t, x -> t.copy(bulbHigh = x as String?) }),
            MoreField(R.string.fluids_fog, R.string.fluids_fog_error, VehicleSpecEntry::bulbCode, w.bulbFog, { t, x -> t.copy(bulbFog = x as String?) }),
        )
        TakesItem.OIL, TakesItem.TIRES, TakesItem.WIPERS -> emptyList()
    }
}

/** The editor for any row past oil, tires and wipers: its fields, each checked, saved in their stored form. */
@Composable
private fun MoreDialog(item: TakesItem, takes: WhatItTakes, toCheck: Boolean, onSave: (List<Any?>, Boolean) -> Unit, onDismiss: () -> Unit) {
    val checked = rememberSaveable(item) { mutableStateOf(false) }
    val fields = remember(item) { moreFields(item, takes) }
    val texts = rememberSaveable(item, saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        fields.map { it.current?.toString().orEmpty() }.toMutableStateList()
    }
    val valid = fields.indices.all { i -> texts[i].isBlank() || fields[i].check(texts[i]) != null }
    SpecEditor(
        title = item.label,
        valid = valid,
        onSave = { onSave(fields.indices.map { i -> texts[i].orNullIfBlank()?.let { fields[i].check(it) } }, checked.value) },
        onDismiss = onDismiss,
        checkedAgainstManual = checked.takeIf { toCheck },
    ) {
        fields.forEachIndexed { i, f ->
            CheckedField(texts[i], { texts[i] = it }, stringResource(f.label), stringResource(f.error), f.check, supporting = f.supporting?.let { stringResource(it) }, keyboard = f.keyboard)
        }
    }
}


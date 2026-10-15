package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.ui.FluidsPartsRoute
import io.cosgriff.glovebox.ui.schedule.SectionLabel
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
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<FluidsPartsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val vehicle: StateFlow<VehicleEntity?> = vehicles.observe(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** One of the rows past oil, tires and wipers; [values] are its fields in order, each checked, null for empty. */
    fun save(item: TakesItem, values: List<Any?>) = viewModelScope.launch {
        fun s(i: Int) = values.getOrNull(i) as String?
        when (item) {
            TakesItem.TRANSMISSION -> vehicles.setTransmissionFluid(vehicleId, s(0))
            TakesItem.DIFFERENTIAL -> vehicles.setDifferentialOil(vehicleId, s(0))
            TakesItem.TRANSFER_CASE -> vehicles.setTransferCaseOil(vehicleId, s(0))
            TakesItem.COOLANT -> vehicles.setCoolant(vehicleId, s(0))
            TakesItem.BRAKE_FLUID -> vehicles.setBrakeFluid(vehicleId, s(0))
            TakesItem.AIR_FILTERS -> vehicles.setAirFilters(vehicleId, s(0), s(1))
            TakesItem.SPARK_PLUGS -> vehicles.setSparkPlugs(vehicleId, s(0), s(1))
            TakesItem.BATTERY -> vehicles.setBattery(vehicleId, s(0), values.getOrNull(1) as Int?)
            TakesItem.BULBS -> vehicles.setBulbs(vehicleId, s(0), s(1), s(2))
            TakesItem.OIL, TakesItem.TIRES, TakesItem.WIPERS -> Unit
        }
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
    val scheme = MaterialTheme.colorScheme
    var editing by rememberSaveable { mutableStateOf<TakesItem?>(null) }
    editing?.let { item -> MoreDialog(item, takes, onSave = { vm.save(item, it); editing = null }, onDismiss = { editing = null }) }

    val entered = takes.entered()
    val notEntered = takes.notEntered()
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        "Fluids, filters & parts",
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        listOfNotNull("${v.year} ${v.nickname ?: v.model}", v.engine).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (entered.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nothing saved yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Keep the fluids, part numbers and sizes you look up, so they're here at the parts counter. " +
                            "Each one shows beside its job on Log service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(entered, key = { it.name }) { item ->
            TakesCard(item, takes, onClick = { editing = item }, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (notEntered.isNotEmpty()) {
            item { SectionLabel("ADD", Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp)) }
            items(notEntered, key = { it.name }) { item ->
                TakesCard(item, takes, onClick = { editing = item }, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
        item {
            Text(
                "You enter these. Glovebox doesn't look them up, so check your owner's or service manual. Hold a card to copy its part numbers.",
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
private fun TakesCard(item: TakesItem, takes: WhatItTakes, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val value = takes.summary(item)
    val parts = takes.partNumbers(item)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClickLabel = parts?.let { "Copy the part numbers" },
                    onLongClick = parts?.let { p -> { copyText(context, "Part numbers", p, sensitive = false); haptics.performHapticFeedback(HapticFeedbackType.LongPress) } },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono) }
            }
            if (value == null) {
                Text(
                    "Tap to add",
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
private class MoreField(val label: String, val error: String, val check: (String) -> Any?, val current: Any?, val supporting: String? = null, val keyboard: KeyboardType = KeyboardType.Text)

private fun moreFields(item: TakesItem, w: WhatItTakes): List<MoreField> {
    val name = "Up to 40 characters"
    val part = "Letters, digits and dashes; up to three, separated by commas"
    return when (item) {
        TakesItem.TRANSMISSION -> listOf(MoreField("Fluid", name, VehicleSpecEntry::fluidName, w.transmissionFluid, "Such as ATF WS, Dexron VI or the DSG fluid's number"))
        TakesItem.DIFFERENTIAL -> listOf(MoreField("Oil", "A grade like 75W-90 GL-5, or the oil's name", VehicleSpecEntry::gearOil, w.differentialOil, "Such as 75W-85 GL-5"))
        TakesItem.TRANSFER_CASE -> listOf(MoreField("Oil", "A grade like 75W-90 GL-5, or the oil's name", VehicleSpecEntry::gearOil, w.transferCaseOil, "A gear oil grade, or the fluid's name"))
        TakesItem.COOLANT -> listOf(MoreField("Coolant", name, VehicleSpecEntry::fluidName, w.coolant, "Such as Toyota Super Long Life or G13"))
        TakesItem.BRAKE_FLUID -> listOf(MoreField("Grade", "DOT 3, DOT 4, DOT 5 or DOT 5.1", VehicleSpecEntry::brakeFluid, w.brakeFluid, "As on the reservoir cap: DOT 3, DOT 4…"))
        TakesItem.AIR_FILTERS -> listOf(
            MoreField("Engine air filter part number", part, VehicleSpecEntry::partNumbers, w.engineAirFilter),
            MoreField("Cabin air filter part number", part, VehicleSpecEntry::partNumbers, w.cabinAirFilter),
        )
        TakesItem.SPARK_PLUGS -> listOf(
            MoreField("Part number", part, VehicleSpecEntry::partNumbers, w.sparkPlug),
            MoreField("Gap", "Such as 0.044 in or 1.1 mm", VehicleSpecEntry::plugGap, w.sparkPlugGap, "In inches or millimetres"),
        )
        TakesItem.BATTERY -> listOf(
            MoreField("Group size", "A BCI group such as 24F, 35 or H6", VehicleSpecEntry::batteryGroup, w.batteryGroup, "As printed on the battery"),
            MoreField("Cold-cranking amps", "100 to 1,500", VehicleSpecEntry::cca, w.batteryCca, keyboard = KeyboardType.Number),
        )
        TakesItem.BULBS -> listOf(
            MoreField("Low beam", "A bulb code such as H11, 9006 or D4S", VehicleSpecEntry::bulbCode, w.bulbLow),
            MoreField("High beam", "A bulb code such as 9005 or HB3", VehicleSpecEntry::bulbCode, w.bulbHigh),
            MoreField("Fog light", "A bulb code such as H11 or H16", VehicleSpecEntry::bulbCode, w.bulbFog),
        )
        TakesItem.OIL, TakesItem.TIRES, TakesItem.WIPERS -> emptyList()
    }
}

/** The editor for any row past oil, tires and wipers: its fields, each checked, saved in their stored form. */
@Composable
private fun MoreDialog(item: TakesItem, takes: WhatItTakes, onSave: (List<Any?>) -> Unit, onDismiss: () -> Unit) {
    val fields = remember(item) { moreFields(item, takes) }
    val texts = rememberSaveable(item, saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        fields.map { it.current?.toString().orEmpty() }.toMutableStateList()
    }
    val valid = fields.indices.all { i -> texts[i].isBlank() || fields[i].check(texts[i]) != null }
    SpecEditor(
        title = item.label,
        valid = valid,
        onSave = { onSave(fields.indices.map { i -> texts[i].orNullIfBlank()?.let { fields[i].check(it) } }) },
        onDismiss = onDismiss,
    ) {
        fields.forEachIndexed { i, f ->
            CheckedField(texts[i], { texts[i] = it }, f.label, f.error, f.check, supporting = f.supporting, keyboard = f.keyboard)
        }
    }
}


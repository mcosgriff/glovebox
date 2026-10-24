package io.cosgriff.glovebox.ui.components

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Powertrain
import org.junit.Assert.assertEquals
import org.junit.Test

class PowertrainTextTest {
    @Test
    fun `every powertrain has its own name`() {
        assertEquals(
            listOf(R.string.powertrain_gas, R.string.powertrain_hybrid, R.string.powertrain_plug_in, R.string.powertrain_flex_fuel, R.string.powertrain_electric),
            listOf(Powertrain.GAS, Powertrain.HYBRID, Powertrain.PLUG_IN, Powertrain.FLEX_FUEL, Powertrain.ELECTRIC).map { it.label() },
        )
        assertEquals(Powertrain.entries.size, Powertrain.entries.map { it.label() }.toSet().size)
    }
}

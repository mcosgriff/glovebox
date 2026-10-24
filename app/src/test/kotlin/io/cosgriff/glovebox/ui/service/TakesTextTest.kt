package io.cosgriff.glovebox.ui.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.TakesPart
import io.cosgriff.glovebox.domain.TakesRole
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.domain.WorkHint
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "What it takes" worded: each part as entered, with its unit, and the hint beside a Log service line. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class TakesTextTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    @Test
    fun `each kind of part says its unit, as it was entered`() {
        assertEquals("0W-20", TakesPart.Entered("0W-20").text())
        assertEquals(string(R.string.takes_quarts, "6.6"), TakesPart.Capacity(6.6, VehicleSpecEntry.Volume.QUARTS).text())
        assertEquals(string(R.string.takes_litres, "6"), TakesPart.Capacity(6.0, VehicleSpecEntry.Volume.LITRES).text())
        assertEquals(string(R.string.takes_psi, "35"), TakesPart.Pressure(35, VehicleSpecEntry.Pressure.PSI).text())
        assertEquals(string(R.string.takes_kpa, "240"), TakesPart.Pressure(240, VehicleSpecEntry.Pressure.KPA).text())
        assertEquals(string(R.string.takes_inches, "26"), TakesPart.Inches(26).text())
        assertEquals(string(R.string.takes_cca, "650"), TakesPart.Cca(650).text())
        assertEquals(
            string(R.string.takes_both, string(R.string.takes_inches, "26"), string(R.string.takes_inches, "17")),
            TakesPart.Both(TakesPart.Inches(26), TakesPart.Inches(17)).text(),
        )
        assertEquals(string(R.string.takes_role_filter, "MADE-UP-1"), TakesPart.For(TakesRole.OIL_FILTER, TakesPart.Entered("MADE-UP-1")).text())
        // Every role has wording of its own.
        val roles = TakesRole.entries.map { TakesPart.For(it, TakesPart.Entered("X")).text() }
        assertEquals(TakesRole.entries.size, roles.toSet().size)
        assertEquals(TakesItem.entries.size, TakesItem.entries.map { it.label }.toSet().size)
        assertEquals(string(R.string.takes_oil), TakesItem.OIL.label)
    }

    @Test
    fun `a row reads on one line, and only rows with something entered are listed`() {
        val takes = WhatItTakes(oilGrade = "0W-20", oilCapacity = 6.6, oilCapacityUnit = VehicleSpecEntry.Volume.QUARTS, batteryCca = 650)
        assertEquals("0W-20 · ${string(R.string.takes_quarts, "6.6")}", takes.line(TakesItem.OIL))
        assertNull(takes.line(TakesItem.BULBS))
        val worded = takes.wordedLines()
        assertEquals(listOf(TakesItem.OIL.label, TakesItem.BATTERY.label), worded.map { it.first })
        assertEquals(string(R.string.takes_cca, "650"), worded.last().second)
    }

    @Test
    fun `a Log service hint is what the vehicle takes, or how often, in its unit`() {
        assertEquals("0W-20", WorkHint.Takes(listOf(TakesPart.Entered("0W-20"))).text(DistanceUnit.MILES))
        assertEquals(
            string(R.string.interval_every, Interval(8_000, 6).text(DistanceUnit.KILOMETERS)),
            WorkHint.Every(Interval(8_000, 6)).text(DistanceUnit.KILOMETERS),
        )
    }
}

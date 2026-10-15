package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkHintTest {
    private val specs = WhatItTakes(
        oilGrade = "0W-20", oilCapacity = 6.2, oilCapacityUnit = VehicleSpecEntry.Volume.QUARTS, oilFilter = "04152-YZZA1",
        tireSize = "265/70R17", transmissionFluid = "ATF WS", differentialOil = "75W-85 GL-5", engineAirFilter = "17801-0P051",
    )

    @Test
    fun `what the vehicle takes comes before the interval`() {
        assertEquals("0W-20 \u00b7 6.2 qt \u00b7 filter 04152-YZZA1", ServiceCatalog.hint("oil", Interval(7_500, 12), specs, DistanceUnit.MILES))
        assertEquals("265/70R17", ServiceCatalog.hint("new-tires", null, specs, DistanceUnit.MILES))
        assertEquals("ATF WS", ServiceCatalog.hint("trans-fluid", Interval(60_000, null), specs, DistanceUnit.MILES))
        assertEquals("75W-85 GL-5", ServiceCatalog.hint("front-diff-check", null, specs, DistanceUnit.MILES))
        assertEquals("17801-0P051", ServiceCatalog.hint("engine-air", null, specs, DistanceUnit.MILES))
    }

    @Test
    fun `a line that resets a tracked reminder says how often it comes round`() {
        assertEquals("Every 5,000 mi / 6 mo", ServiceCatalog.hint("rotation", Interval(5_000, 6), specs, DistanceUnit.MILES))
        assertEquals("Every 36 mo", ServiceCatalog.hint("brake-fluid", Interval(null, 36), specs, DistanceUnit.MILES))
        assertEquals("Every 7,500 mi / 12 mo", ServiceCatalog.hint("oil", Interval(7_500, 12), WhatItTakes(), DistanceUnit.MILES))
    }

    @Test
    fun `nothing written down and no reminder means no hint`() {
        assertNull(ServiceCatalog.hint("wipers", null, specs, DistanceUnit.MILES)) // blank is not a spec
        assertNull(ServiceCatalog.hint("belt", null, specs, DistanceUnit.MILES))
    }

    @Test
    fun `every spec the hint knows belongs to a real catalog line`() {
        val ids = ServiceCatalog.categories.flatMap { it.items }.map { it.id }.toSet()
        val known = listOf(
            "oil", "new-tires", "rotation", "wipers", "trans-fluid", "diff-fluid", "front-diff", "rear-diff", "front-diff-check",
            "rear-diff-check", "transfer-case", "transfer-case-check", "coolant", "brake-fluid", "engine-air", "cabin-air", "spark-plugs",
            "battery", "bulbs",
        )
        known.forEach { assert(it in ids) { "$it is not in the catalog" } }
    }
}

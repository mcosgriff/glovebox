package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.TakesPart.Entered
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
        assertEquals(
            takes(Entered("0W-20"), TakesPart.Capacity(6.2, VehicleSpecEntry.Volume.QUARTS), TakesPart.For(TakesRole.OIL_FILTER, Entered("04152-YZZA1"))),
            ServiceCatalog.hint("oil", Interval(7_500, 12), specs),
        )
        assertEquals(takes(Entered("265/70R17")), ServiceCatalog.hint("new-tires", null, specs))
        assertEquals(takes(Entered("ATF WS")), ServiceCatalog.hint("trans-fluid", Interval(60_000, null), specs))
        assertEquals(takes(Entered("75W-85 GL-5")), ServiceCatalog.hint("front-diff-check", null, specs))
        assertEquals(takes(Entered("17801-0P051")), ServiceCatalog.hint("engine-air", null, specs))
    }

    @Test
    fun `a line that resets a tracked reminder says how often it comes round`() {
        assertEquals(WorkHint.Every(Interval(5_000, 6)), ServiceCatalog.hint("rotation", Interval(5_000, 6), specs))
        assertEquals(WorkHint.Every(Interval(null, 36)), ServiceCatalog.hint("brake-fluid", Interval(null, 36), specs))
        assertEquals(WorkHint.Every(Interval(7_500, 12)), ServiceCatalog.hint("oil", Interval(7_500, 12), WhatItTakes()))
        assertEquals(WorkHint.Every(Interval(10_000, 12), monthsWorkedOut = true), ServiceCatalog.hint("oil", Interval(10_000, 12), WhatItTakes(), monthsWorkedOut = true))
    }

    @Test
    fun `nothing written down and no reminder means no hint`() {
        assertNull(ServiceCatalog.hint("wipers", null, specs))
        assertNull(ServiceCatalog.hint("belt", null, specs))
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

    private fun takes(vararg parts: TakesPart) = WorkHint.Takes(parts.toList())
}

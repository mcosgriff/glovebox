package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkHintTest {
    private val specs = VehicleSpecs(oil = "0W-20 \u00b7 6.2 qt", tireSize = "265/70R17", wiperBlades = " ")

    @Test
    fun `what the vehicle takes comes before the interval`() {
        assertEquals("0W-20 \u00b7 6.2 qt", ServiceCatalog.hint("oil", Interval(7_500, 12), specs, DistanceUnit.MILES))
        assertEquals("265/70R17", ServiceCatalog.hint("new-tires", null, specs, DistanceUnit.MILES))
    }

    @Test
    fun `a line that resets a tracked reminder says how often it comes round`() {
        assertEquals("Every 5,000 mi / 6 mo", ServiceCatalog.hint("rotation", Interval(5_000, 6), specs, DistanceUnit.MILES))
        assertEquals("Every 36 mo", ServiceCatalog.hint("brake-fluid", Interval(null, 36), specs, DistanceUnit.MILES))
        assertEquals("Every 7,500 mi / 12 mo", ServiceCatalog.hint("oil", Interval(7_500, 12), VehicleSpecs(), DistanceUnit.MILES))
    }

    @Test
    fun `nothing written down and no reminder means no hint`() {
        assertNull(ServiceCatalog.hint("wipers", null, specs, DistanceUnit.MILES)) // blank is not a spec
        assertNull(ServiceCatalog.hint("belt", null, specs, DistanceUnit.MILES))
    }

    @Test
    fun `every spec the hint knows belongs to a real catalog line`() {
        val ids = ServiceCatalog.categories.flatMap { it.items }.map { it.id }.toSet()
        listOf("oil", "new-tires", "wipers").forEach { assert(it in ids) { "$it is not in the catalog" } }
    }
}

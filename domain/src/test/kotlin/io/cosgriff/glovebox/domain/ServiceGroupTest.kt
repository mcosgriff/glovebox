package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceGroupTest {
    @Test
    fun `a line's group follows its catalog category, and a line with none is other`() {
        assertEquals(ServiceGroup.OIL_ENGINE, ServiceGroup.ofLine("oil"))
        assertEquals(ServiceGroup.OIL_ENGINE, ServiceGroup.ofLine("engine-air"))
        assertEquals(ServiceGroup.TIRES, ServiceGroup.ofLine("rotation"))
        assertEquals(ServiceGroup.BRAKES, ServiceGroup.ofLine("front-pads"))
        assertEquals(ServiceGroup.FLUIDS_4WD, ServiceGroup.ofLine("trans-fluid"))
        assertEquals(ServiceGroup.FLUIDS_4WD, ServiceGroup.ofLine("coolant"))
        assertEquals(ServiceGroup.FLUIDS_4WD, ServiceGroup.ofLine("transfer-case"))
        assertEquals(ServiceGroup.SUSPENSION, ServiceGroup.ofLine("shocks"))
        assertEquals(ServiceGroup.SUSPENSION, ServiceGroup.ofLine("tie-rods"))
        assertEquals(ServiceGroup.FUEL_EXHAUST, ServiceGroup.ofLine("fuel-filter"))
        assertEquals(ServiceGroup.FUEL_EXHAUST, ServiceGroup.ofLine("muffler"))
        assertEquals(ServiceGroup.OTHER, ServiceGroup.ofLine("cabin-air"))
        assertEquals(ServiceGroup.OTHER, ServiceGroup.ofLine("wipers"))
        assertEquals(ServiceGroup.OTHER, ServiceGroup.ofLine(null))
        assertEquals(ServiceGroup.OTHER, ServiceGroup.ofLine("not-a-line"))
    }

    @Test
    fun `a schedule item's group comes through its template key, or else its service`() {
        assertEquals(ServiceGroup.OIL_ENGINE, ServiceGroup.ofItem(templateKey = "oil", service = null))
        assertEquals(ServiceGroup.BRAKES, ServiceGroup.ofItem(templateKey = null, service = "brake-inspect"))
        assertEquals(ServiceGroup.SUSPENSION, ServiceGroup.ofItem(templateKey = null, service = "shocks"))
        assertEquals(ServiceGroup.OTHER, ServiceGroup.ofItem(templateKey = null, service = null))
    }
}

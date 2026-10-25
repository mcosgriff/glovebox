package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VinRefreshTest {
    private enum class Field { ENGINE, ENGINE_CODE, TPMS, SERIES }

    @Test
    fun `only what is empty here and present in the decode is offered`() {
        val saved = mapOf(Field.ENGINE to "4.0L 6-cyl", Field.ENGINE_CODE to null, Field.TPMS to " ", Field.SERIES to null)
        val found = mapOf(Field.ENGINE to "3.5L 6-cyl", Field.ENGINE_CODE to "1GR-FE", Field.TPMS to "Direct", Field.SERIES to null)

        assertEquals(mapOf(Field.ENGINE_CODE to "1GR-FE", Field.TPMS to "Direct"), VinRefresh.additions(saved, found))
    }

    @Test
    fun `a decode with nothing new offers nothing`() {
        assertEquals(emptyMap<Field, String>(), VinRefresh.additions(mapOf(Field.ENGINE to "4.0L 6-cyl"), mapOf(Field.ENGINE to "4.0L 6-cyl", Field.TPMS to "")))
    }

    @Test
    fun `a decode is of this vehicle when its year and make match, whatever the case`() {
        assertTrue(VinRefresh.sameVehicle(2019, "Toyota", 2019, "TOYOTA"))
        // The model may have been corrected when the vehicle was added, so it isn't compared.
        assertTrue(VinRefresh.sameVehicle(2019, "Toyota ", 2019, "toyota"))
    }

    @Test
    fun `a decode of another year or make, or one that couldn't say, is not this vehicle`() {
        assertFalse(VinRefresh.sameVehicle(2019, "Toyota", 2018, "TOYOTA"))
        assertFalse(VinRefresh.sameVehicle(2019, "Toyota", 2019, "FORD"))
        assertFalse(VinRefresh.sameVehicle(2019, "Toyota", null, "TOYOTA"))
        assertFalse(VinRefresh.sameVehicle(2019, "Toyota", 2019, null))
    }
}

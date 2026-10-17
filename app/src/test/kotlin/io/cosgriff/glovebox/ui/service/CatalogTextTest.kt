package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.domain.ServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogTextTest {
    @Test
    fun `every catalog line and category has a name, and nothing else does`() {
        assertEquals(ServiceCatalog.categories.flatMap { it.items }.map { it.id }.toSet(), catalogLines.keys)
        assertEquals(ServiceCatalog.categories.map { it.id }.toSet() + ServiceCatalog.YOUR_ITEMS, catalogCategories.keys)
    }
}

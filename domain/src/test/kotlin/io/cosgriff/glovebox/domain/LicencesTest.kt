package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LicencesTest {
    private val apache = Licence("Apache-2.0", "Apache License 2.0", "https://example.test/apache", "Apache text")
    private val mit = Licence("MIT", "MIT License", null, "MIT text")
    private val own = Licence("abc123", "", "https://example.test/own", null)
    private val known = mapOf(apache.id to apache, mit.id to mit, own.id to own)

    private fun lib(id: String, name: String, vararg licences: String) = BundledLibrary(id, name, "1.0", licences.toList())

    @Test
    fun `groups by licence, biggest first, libraries by name`() {
        val groups = Licences.grouped(
            listOf(lib("g:zeta", "Zeta", "Apache-2.0"), lib("g:alpha", "alpha", "Apache-2.0"), lib("g:solo", "Solo", "MIT")),
            known,
        )
        assertEquals(listOf("Apache License 2.0", "MIT License"), groups.map { it.name })
        assertEquals(listOf("alpha", "Zeta"), groups[0].libraries.map { it.name })
    }

    @Test
    fun `a library under two licences is listed under both`() {
        val groups = Licences.grouped(listOf(lib("g:both", "Both", "Apache-2.0", "MIT")), known)
        assertEquals(setOf("Apache License 2.0", "MIT License"), groups.map { it.name }.toSet())
        assertEquals("1 library under 2 licences", Licences.summary(groups))
    }

    @Test
    fun `a nameless licence is named after its only library`() {
        val groups = Licences.grouped(listOf(lib("g:cipher", "Cipher", "abc123")), known)
        assertEquals("Cipher licence", groups.single().name)
    }

    @Test
    fun `nothing bundled goes unlisted`() {
        val groups = Licences.grouped(listOf(lib("g:odd", "Odd", "Weird-1.0"), lib("g:none", "")), known)
        assertEquals(setOf("Weird-1.0", "Licence not recorded"), groups.map { it.name }.toSet())
        assertEquals("g:none", Licences.displayName(groups.first { it.name == "Licence not recorded" }.libraries.single()))
    }

    @Test
    fun `asset name is the coordinate with the colon replaced`() {
        assertEquals("licences/net.zetetic_sqlcipher-android.txt", Licences.assetName("net.zetetic:sqlcipher-android"))
    }

    @Test
    fun `wording is reflowed a paragraph at a time and no word is lost`() {
        val text = "   TITLE\n      Version 3\n\n  First line\n  second line.\n\n\n\nLast.\n"
        assertEquals(listOf("TITLE Version 3", "First line second line.", "Last."), Licences.paragraphs(text))
        assertEquals(emptyList<String>(), Licences.paragraphs("  \n "))
    }

    @Test
    fun `summary counts each library once`() {
        val groups = Licences.grouped(listOf(lib("g:a", "A", "Apache-2.0"), lib("g:b", "B", "Apache-2.0"), lib("g:c", "C", "MIT")), known)
        assertEquals("3 libraries under 2 licences", Licences.summary(groups))
    }
}

package io.cosgriff.glovebox

import io.cosgriff.glovebox.notify.ShortcutTask
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The launcher shortcuts (#235) are in two files, one per package, since a shortcut names its app by
 * package and the debug build's differs. They must stay the same otherwise, and each must open one of
 * the [ShortcutTask]s.
 */
class ShortcutsTest {
    private data class Shortcut(val attributes: Map<String, String>, val intent: Map<String, String>, val extras: Map<String, String>)

    private fun read(path: String): List<Shortcut> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        fun Element.attrs() = (0 until attributes.length).associate { attributes.item(it).nodeName to attributes.item(it).nodeValue }
        fun Element.children(tag: String) = getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }
        return doc.documentElement.children("shortcut").map { s ->
            val intent = s.children("intent").single()
            Shortcut(s.attrs(), intent.attrs(), intent.children("extra").associate { it.getAttribute("android:name") to it.getAttribute("android:value") })
        }
    }

    private val release = read("src/main/res/xml/shortcuts.xml")
    private val debug = read("src/debug/res/xml/shortcuts.xml")

    @Test
    fun `each build's file names its own package`() {
        assertEquals(setOf("io.cosgriff.glovebox"), release.map { it.intent["android:targetPackage"] }.toSet())
        assertEquals(setOf("io.cosgriff.glovebox.debug"), debug.map { it.intent["android:targetPackage"] }.toSet())
    }

    @Test
    fun `the debug file is the same apart from the package`() {
        fun List<Shortcut>.withoutPackage() = map { it.copy(intent = it.intent - "android:targetPackage") }
        assertEquals(release.withoutPackage(), debug.withoutPackage())
    }

    @Test
    fun `every shortcut opens a task, and every task has a shortcut`() {
        assertEquals(setOf(ShortcutActivity::class.java.name), release.map { it.intent["android:targetClass"] }.toSet())
        assertEquals(ShortcutTask.entries.map { it.kind }, release.map { it.extras["glovebox.destination"] })
    }
}

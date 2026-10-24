package io.cosgriff.glovebox.ui.vehicle

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.domain.DocumentProblem
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.ui.DocumentsRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** A vehicle's documents: listed, named, deleted for good, and added through the picker the activity owns. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class DocumentsScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val viewed = mutableListOf<Long>()
    private var picks = 0
    private val captures = mutableListOf<Uri>()

    private fun title(id: Long, fileName: String = "document-title.pdf") = runBlocking {
        g.db.documents().insert(
            DocumentEntity(vehicleId = id, type = DocumentType.TITLE.name, name = "Title, front", fileName = fileName, format = "PDF", sizeBytes = 2_048, addedOn = LocalDate.now(), date = LocalDate.of(2024, 3, 2)),
        )
    }

    private fun show(id: Long, until: String) {
        g.documentPicker.attach({ picks++ }, { captures += it })
        val vm = g.viewModel { DocumentsViewModel(routeHandle(DocumentsRoute(id)), g.db.vehicles(), g.db.documents(), g.documentStore, g.documentPicker) }
        compose.setContent { GloveboxTheme { DocumentsScreen(onBack = {}, onView = { viewed += it }, vm = vm) } }
        compose.awaitThat { compose.onAllNodes(hasText(until, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun menu(name: String = "Title, front") = compose.onNode(hasContentDescription(g.string(R.string.more_for, name))).click()
    private fun item(label: Int) = compose.onNode(hasText(g.string(label)) and hasClickAction()).click()

    @Test
    fun `a vehicle with no documents says what they are for`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id, until = g.string(R.string.documents_empty_body))
    }

    @Test
    fun `a document opens in the app, from its card or its menu`() {
        val id = g.add(VehicleFixtures.wagon)
        val doc = title(id)
        show(id, until = "Title, front")
        compose.onNode(hasText("Title, front", substring = true) and hasClickAction()).click()
        menu()
        item(R.string.open)
        assertEquals(listOf(doc, doc), viewed)
    }

    @Test
    fun `its details are renamed and retyped, and a blank name can't be saved`() {
        val id = g.add(VehicleFixtures.wagon)
        val doc = title(id)
        show(id, until = "Title, front")
        menu()
        item(R.string.documents_edit_details)
        val name = compose.onNode(hasSetTextAction() and hasText(g.string(R.string.documents_name)))
        name.performTextClearance()
        compose.onNodeWithText(g.string(R.string.documents_give_name)).assertExists()
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).assertIsNotEnabled()
        name.performTextInput("Title, both sides")
        // The type, from its menu; the date, cleared.
        compose.onNode(hasText(DocumentType.TITLE.label) and hasClickAction()).click()
        compose.onNode(hasText(DocumentType.REGISTRATION.label) and hasClickAction()).click()
        compose.onNode(hasText(g.string(R.string.clear)) and hasClickAction()).click()
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).click()

        compose.awaitThat { g.db.documents().byId(doc)?.name == "Title, both sides" }
        val row = runBlocking { g.db.documents().byId(doc) }!!
        assertEquals(DocumentType.REGISTRATION.name, row.type)
        assertNull(row.date)
    }

    @Test
    fun `a document still named for its type follows a new type`() {
        val id = g.add(VehicleFixtures.wagon)
        val doc = runBlocking {
            g.db.documents().insert(DocumentEntity(vehicleId = id, type = "OTHER", name = DocumentType.OTHER.label, fileName = "d.jpg", format = "JPEG", sizeBytes = 900, addedOn = LocalDate.now()))
        }
        show(id, until = DocumentType.OTHER.label)
        menu(DocumentType.OTHER.label)
        item(R.string.documents_edit_details)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)).click()
        compose.onNode(hasText(DocumentType.WARRANTY.label) and hasClickAction()).click()
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).click()
        compose.awaitThat { g.db.documents().byId(doc)?.type == DocumentType.WARRANTY.name }
        assertEquals(DocumentType.WARRANTY.label, runBlocking { g.db.documents().byId(doc) }!!.name)
    }

    @Test
    fun `deleting asks first, then takes the row and its file`() {
        val id = g.add(VehicleFixtures.wagon)
        val file = File(g.crypto.photoDir, "document-title.pdf").apply { writeText("x") }
        val doc = title(id, file.name)
        show(id, until = "Title, front")
        menu()
        item(R.string.delete)
        compose.onNodeWithText(g.string(R.string.documents_delete_title, "Title, front")).assertExists()
        compose.onNode(hasText(g.string(R.string.cancel)) and hasClickAction()).click()
        assertTrue(file.exists())

        menu()
        item(R.string.delete)
        compose.onNode(hasText(g.string(R.string.delete)) and hasClickAction()).click()
        compose.awaitThat { g.db.documents().byId(doc) == null }
        assertFalse(file.exists())
    }

    @Test
    fun `a document that can't be decrypted says so instead of opening`() {
        val id = g.add(VehicleFixtures.wagon)
        title(id, fileName = "document-gone.pdf")
        show(id, until = "Title, front")
        menu()
        item(R.string.documents_open_elsewhere)
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.documents_unreadable))).fetchSemanticsNodes().isNotEmpty() }
        menu()
        item(R.string.share)
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.documents_unreadable))).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `adding chooses a file through the activity's picker, and an empty one is refused`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id, until = g.string(R.string.documents_empty_body))
        item(R.string.documents_add)
        item(R.string.documents_choose)
        assertEquals(1, picks)

        val empty = File(g.context.cacheDir, "empty.pdf").apply { writeBytes(ByteArray(0)) }
        g.documentPicker.onPicked(Uri.fromFile(empty))
        compose.awaitThat { compose.onAllNodes(hasText(DocumentProblem.EMPTY.text)).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { g.db.documents().documentsFor(id) }.isEmpty())
    }

    @Test
    fun `taking a photo hands the camera a file of its own once the camera is allowed`() {
        val id = g.add(VehicleFixtures.wagon)
        shadowOf(g.context).grantPermissions(Manifest.permission.CAMERA)
        show(id, until = g.string(R.string.documents_empty_body))
        item(R.string.documents_add)
        item(R.string.documents_take_photo)
        assertEquals(1, captures.size)
        assertEquals("content", captures.single().scheme)

        // The camera wrote its photo in the clear, then came back without it being kept: the file goes, and nothing is added.
        val written = File(g.recordPhotos.captureDir, captures.single().lastPathSegment!!).apply { writeText("not a photo") }
        g.documentPicker.onCaptured(saved = false)
        eventuallyBlocking { !written.exists() }
        assertNull(g.documentPicker.problem.value)
        assertTrue(runBlocking { g.db.documents().documentsFor(id) }.isEmpty())
    }

    private fun eventuallyBlocking(condition: () -> Boolean) = runBlocking { eventually { condition() } }

    @Test
    fun `a document just added asks for its details, and can be taken back off at once`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id, until = g.string(R.string.documents_empty_body))
        item(R.string.documents_add)
        item(R.string.documents_choose)
        g.documentPicker.onPicked(Uri.fromFile(File(g.context.cacheDir, "made-up.pdf").apply { writeBytes(pdf) }))

        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.documents_added))).fetchSemanticsNodes().isNotEmpty() }
        val row = runBlocking { g.db.documents().documentsFor(id) }.single()
        assertTrue(File(g.crypto.photoDir, row.fileName).exists())
        // The screen has taken it: it won't open again on its own.
        assertNull(g.documentPicker.added.value)

        compose.onNode(hasText(g.string(R.string.documents_dont_keep)) and hasClickAction()).click()
        compose.awaitThat { g.db.documents().documentsFor(id).isEmpty() }
        assertFalse(File(g.crypto.photoDir, row.fileName).exists())
    }

    @Test
    fun `a readable document is handed to another app only as a decrypted copy in the share folder`() {
        val id = g.add(VehicleFixtures.wagon)
        g.documentPicker.attach({ picks++ }, { captures += it })
        val doc = runBlocking { g.documentStore.addPdf(id, pdf, DocumentType.REPORT, "History report", null, LocalDate.now()) }
        show(id, until = "History report")
        menu("History report")
        item(R.string.documents_open_elsewhere)

        val started = run {
            var intent: Intent? = null
            compose.awaitThat { intent = shadowOf(g.context).nextStartedActivity; intent != null }
            intent!!
        }
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("content", started.data!!.scheme)
        assertEquals("application/pdf", started.type)
        val copy = g.recordPhotos.sharedDir.listFiles()!!.single()
        assertTrue(copy.readBytes().contentEquals(pdf))
        compose.onNodeWithText(g.string(R.string.documents_unreadable)).assertDoesNotExist()
        assertEquals(doc, runBlocking { g.db.documents().documentsFor(id) }.single().id)
    }

    private val pdf = "%PDF-1.4\n% made up for a test\n%%EOF\n".toByteArray()
}

package io.cosgriff.glovebox.ui.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.fragment.app.FragmentActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.ui.screens.PHONE
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Delete all data: typed DELETE unlocks the button, and the phone's own lock is the second check. Nothing here
 * passes that check, so nothing is ever wiped (a wipe ends the process).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = FragmentHostApp::class, qualifiers = PHONE, sdk = [35])
class DeleteAllScreenTest {
    @get:Rule(order = 0) val g = TestGraph()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<FragmentActivity>()

    @Test
    fun `the button stays locked until DELETE is typed, and then asks for the screen lock before anything goes`() {
        val id = runBlocking { g.vehicle() }
        val vm = DeleteAllViewModel(DataWipe(g.context, g.db, DatabaseKeyManager(g.context), g.crypto))
        var cancelled = 0
        var backup = 0
        compose.show { DeleteAllScreen(onCancel = { cancelled++ }, onBackupFirst = { backup++ }, vm = vm) }

        compose.onNodeWithText(text(R.string.delete_all_title)).assertExists()
        val confirm = compose.onNodeWithText(text(R.string.delete_all_confirm_fingerprint))
        confirm.performScrollTo().assertIsNotEnabled()
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("delet")
        confirm.assertIsNotEnabled()
        field.performTextReplacement("DELETE")
        confirm.assertIsEnabled().performClick()

        // Asking for the lock deletes nothing on its own.
        compose.waitForIdle()
        assertFalse(vm.deleting)
        assertEquals(listOf(id), runBlocking { g.db.vehicles().all() }.map { it.id })

        compose.onNodeWithText(text(R.string.delete_all_backup_first)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(text(R.string.cancel)).performClick()
        assertEquals(1, backup)
        assertEquals(1, cancelled)
    }
}

package io.cosgriff.glovebox

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class AppModuleTest {
    @Test
    fun `one failure in the application scope doesn't cancel the rest`() {
        val scope = AppModule_ApplicationScopeFactory.applicationScope()
        val failing = scope.launch(kotlinx.coroutines.CoroutineExceptionHandler { _, _ -> }) { error("An Undo that failed") }
        val other = scope.launch { kotlinx.coroutines.awaitCancellation() }
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(failing.isCancelled)
        assertTrue(other.isActive)
        assertTrue(scope.coroutineContext[Job]!!.isActive)
        scope.cancel()
        ShellTestApp.graph.close()
    }
}

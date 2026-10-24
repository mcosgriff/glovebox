package io.cosgriff.glovebox.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.ui.theme.GloveboxTheme

/** What the shared components' tests need: the app's words, and a component drawn in the app's theme. */
internal object Components {
    val context: Context get() = ApplicationProvider.getApplicationContext()

    fun string(id: Int, vararg args: Any): String = context.getString(id, *args)
}

internal fun ComposeContentTestRule.draw(content: @Composable () -> Unit) = setContent { GloveboxTheme(content = content) }

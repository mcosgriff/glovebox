package io.cosgriff.glovebox.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.ui.theme.GloveboxTheme

/**
 * Exact: any pixel that differs fails. Roborazzi's default allows each pixel a small colour distance,
 * which let a theme token moved by one step pass unseen.
 */
private val exact = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(imageComparator = SimpleImageComparator(maxDistance = 0f), changeThreshold = 0f),
)

/** Light and dark, which every screen is drawn in. */
enum class Shade(val mode: ThemeMode) { LIGHT(ThemeMode.LIGHT), DARK(ThemeMode.DARK) }

/**
 * The phone every screen is drawn on: a common width, tall enough that most screens fit without
 * scrolling, at twice the density to keep the files small.
 */
const val PHONE = "w411dp-h1100dp-xhdpi"

/**
 * Draws [content] as the app shows a screen: in its theme, on the page colour, with the text colour the
 * nav host's `Scaffold` gives every screen. Compares it with the reference `src/test/screenshots/<name>_<shade>.png`.
 */
fun ComposeContentTestRule.shoot(name: String, shade: Shade, content: @Composable () -> Unit) {
    setContent {
        GloveboxTheme(shade.mode) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
        }
    }
    onRoot().captureRoboImage("src/test/screenshots/${name}_${shade.name.lowercase()}.png", roborazziOptions = exact)
}

/**
 * As [shoot], for a bottom sheet or dialog: those are windows of their own, so the whole screen is
 * captured, with the sheet over an empty page.
 */
@OptIn(ExperimentalRoborazziApi::class)
fun ComposeContentTestRule.shootScreen(name: String, shade: Shade, content: @Composable () -> Unit) {
    setContent {
        GloveboxTheme(shade.mode) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
        }
    }
    waitForIdle()
    captureScreenRoboImage("src/test/screenshots/${name}_${shade.name.lowercase()}.png", roborazziOptions = exact)
}

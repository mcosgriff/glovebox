@file:OptIn(ExperimentalRoborazziApi::class)

package io.cosgriff.glovebox.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.dropbox.differ.Image
import com.dropbox.differ.ImageComparator
import com.dropbox.differ.Mask
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anyOf
import org.hamcrest.TypeSafeMatcher
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlin.math.abs

/**
 * Exact: any pixel that differs fails. Roborazzi's default allows each pixel a small colour distance,
 * which let a theme token moved by one step pass unseen.
 */
private val exact = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(imageComparator = SimpleImageComparator(maxDistance = 0f), changeThreshold = 0f),
)

/**
 * For a sheet or dialog: a pixel may be one step out of 255 in a channel, and no more. Those shots blend
 * translucent windows (a scrim, a dim, rounded corners) over the page, and that blend rounds differently
 * on a Mac than on CI's Linux, so the same code drew references one step apart on the two. Anything
 * a change could make, such as a token moved by one tone, is many steps.
 */
private val exactBut1 = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(imageComparator = WithinOneStep, changeThreshold = 0f),
)

private object WithinOneStep : ImageComparator {
    private const val STEP = 1.5f / 255f // one step, with room for the float conversion

    override fun compare(left: Image, right: Image, diff: Mask?): ImageComparator.ComparisonResult {
        var differences = 0
        for (y in 0 until left.height) {
            for (x in 0 until left.width) {
                val a = left.getPixel(x, y)
                val b = right.getPixel(x, y)
                if (abs(a.r - b.r) > STEP || abs(a.g - b.g) > STEP || abs(a.b - b.b) > STEP || abs(a.a - b.a) > STEP) {
                    differences++
                    diff?.setValue(x, y, 1f)
                }
            }
        }
        return ImageComparator.ComparisonResult(differences, left.width * left.height, left.width, left.height)
    }
}

/**
 * A finding that is deliberate, left out for the one kind of element it is about: [check] on an element
 * [element] matches, and nothing else. Each use says why.
 */
private fun allowed(check: Class<out AccessibilityHierarchyCheck>, what: String, element: (ViewHierarchyElement) -> Boolean): Matcher<AccessibilityViewCheckResult> =
    object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
        override fun matchesSafely(result: AccessibilityViewCheckResult) =
            result.accessibilityHierarchyCheck == check && result.element?.let(element) == true

        override fun describeTo(description: Description) {
            description.appendText("${check.simpleName} on $what")
        }
    }

/**
 * Every check in the Accessibility Test Framework's latest set (#234); a warning fails the test as an error
 * does. The touch target, contrast, label and duplicate checks are the ones that find most.
 */
private val accessibility = RoborazziATFAccessibilityCheckOptions(
    checker = RoborazziATFAccessibilityChecker(
        preset = AccessibilityCheckPreset.LATEST,
        suppressions = anyOf(
            // A control that is switched off is greyed out on purpose, and WCAG's contrast rule (1.4.3) leaves
            // inactive controls out; the sheets' Save button is one until there is something to save.
            allowed(TextContrastCheck::class.java, "a disabled control") { !it.isEnabled || it.parentView?.isEnabled == false },
            // A filled text field: the check guesses the text colour from the field's pixels, and a 1dp outline round
            // a few thin figures outnumbers them, so it measures the outline (#B9B6AE, 2:1) instead of the text
            // (onSurface on surface, 17:1). Seen first on Log service's price fields (#481).
            allowed(TextContrastCheck::class.java, "a filled text field") { it.isEditable == true },
        ),
    ),
    failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
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
    // Accessibility first: a finding says what is wrong, where an image that differs only says that it does.
    onRoot().checkRoboAccessibility(accessibility)
    onRoot().captureRoboImage("src/test/screenshots/${name}_${shade.name.lowercase()}.png", roborazziOptions = exact)
}

/** A picture drawn without Compose, such as a page of the PDF report, compared exactly with `src/test/screenshots/<name>.png`. */
fun android.graphics.Bitmap.shoot(name: String) = captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = exact)

/**
 * As [shoot], for a bottom sheet or dialog: those are windows of their own, so the whole screen is
 * captured, with the sheet over an empty page.
 */
fun ComposeContentTestRule.shootScreen(name: String, shade: Shade, content: @Composable () -> Unit) {
    setContent {
        GloveboxTheme(shade.mode) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
        }
    }
    waitForIdle()
    // Each window: the page under the sheet and the sheet itself.
    val roots = onAllNodes(isRoot())
    repeat(roots.fetchSemanticsNodes().size) { roots[it].checkRoboAccessibility(accessibility) }
    captureScreenRoboImage("src/test/screenshots/${name}_${shade.name.lowercase()}.png", roborazziOptions = exactBut1)
}

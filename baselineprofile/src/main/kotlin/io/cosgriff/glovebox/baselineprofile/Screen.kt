package io.cosgriff.glovebox.baselineprofile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/**
 * The words the generator and the benchmark drive the app with: they find things by what the screen
 * says, never by anything inside the app, as `EndToEndTest` does (that test lives in `:app` and can't be
 * shared with this module). A failure names the step it was on and what the screen showed instead.
 */
class Screen(private val device: UiDevice) {
    private var step = "starting"

    fun step(name: String) {
        step = name
    }

    fun containing(text: String): BySelector = By.textContains(text)

    fun see(text: String, timeout: Long = WAIT): UiObject2 = see(By.text(text), timeout)

    /** Waits for it, and scrolls down for it if it doesn't come. */
    fun see(selector: BySelector, timeout: Long = WAIT): UiObject2 =
        device.wait(Until.findObject(selector), timeout)
            ?: run {
                scrollDownFor(selector)
                device.findObject(selector)
            }
            ?: throw AssertionError("While trying to $step: didn't find $selector on the screen. It showed: ${onScreen()}")

    fun has(text: String, timeout: Long): Boolean = device.wait(Until.findObject(By.text(text)), timeout) != null

    fun tap(text: String, timeout: Long = WAIT) = tap(By.text(text), timeout)

    fun tap(selector: BySelector, timeout: Long = WAIT) = stale { tap(see(selector, timeout)) }

    fun tap(target: UiObject2) {
        target.click()
        device.waitForIdle()
        Thread.sleep(SETTLE)
    }

    /** The first on the screen, top to bottom, of several things that say the same. */
    fun first(text: String): UiObject2 {
        see(text)
        return device.findObjects(By.text(text)).minBy { it.visibleBounds.top }
    }

    /** Scrolls down until it is on the screen, and lifts it off the bottom edge, where a tap goes to Android. */
    fun scrolledTo(text: String): BySelector = scrolledTo(By.text(text))

    fun scrolledTo(selector: BySelector): BySelector {
        if (device.wait(Until.findObject(selector), 2_000) == null) scrollDownFor(selector)
        device.findObject(selector)?.let { found ->
            val h = device.displayHeight
            if (found.visibleBounds.bottom > h * 85 / 100) {
                device.swipe(device.displayWidth / 2, h * 6 / 10, device.displayWidth / 2, h * 4 / 10, 40)
                device.waitForIdle()
            }
        }
        device.waitForIdle()
        Thread.sleep(SETTLE)
        see(selector)
        return selector
    }

    /**
     * Flings the screen's list to the bottom and back to the top: what a person does on a long screen,
     * and the frames the benchmark measures.
     */
    fun flingDownAndUp() {
        for (direction in listOf(Direction.DOWN, Direction.UP)) {
            // The list is found afresh for every fling: Compose redraws it as it moves, and an object
            // found before the last fling may be gone.
            for (i in 0 until MAX_FLINGS) {
                val more = try {
                    scrollable().fling(direction)
                } catch (_: StaleObjectException) {
                    true
                }
                device.waitForIdle()
                if (!more) break
            }
        }
    }

    private fun scrollable(): UiObject2 {
        val list = device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }
            ?: throw AssertionError("While trying to $step: nothing on the screen scrolls. It showed: ${onScreen()}")
        // Keep clear of the gesture edges, where a swipe is Android's.
        list.setGestureMargin(device.displayWidth / 10)
        return list
    }

    /** Types into the field labelled [into]. */
    fun type(into: String, text: String) = stale {
        see(into).click()
        device.waitForIdle()
        // The label is inside the field it names; the field itself is what takes the text.
        val editable = device.findObjects(By.clazz("android.widget.EditText")).minByOrNull { distance(it, see(into)) }
            ?: throw AssertionError("While trying to $step: no field to type \"$text\" into for \"$into\".")
        editable.text = text
        device.waitForIdle()
        closeKeyboard()
    }

    /** One of the pickers on the add-vehicle form: open it, type the value, take the row that offers it. */
    fun pick(label: String, field: String, value: String) {
        see("Choose")
        stale { tap(device.findObjects(By.text("Choose")).minBy { distance(it, see(label)) }) }
        type(into = field, text = value)
        stale { tap(device.wait(Until.findObjects(By.textContains(value)), WAIT).maxBy { it.visibleBounds.top }) }
    }

    /** Back closes the keyboard when it is up, and leaves the screen when it isn't: only press it for the keyboard. */
    fun closeKeyboard() {
        if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) {
            device.pressBack()
            device.waitForIdle()
        }
    }

    private fun scrollDownFor(selector: BySelector) {
        val w = device.displayWidth
        val h = device.displayHeight
        for ((from, to) in listOf(h * 7 / 10 to h * 3 / 10, h * 3 / 10 to h * 7 / 10)) {
            repeat(15) {
                if (device.findObject(selector) != null) return
                device.swipe(w / 2, from, w / 2, to, 40)
                device.waitForIdle()
                Thread.sleep(SETTLE)
            }
        }
    }

    /** Compose redraws often; an object found a moment ago can be gone. Look again, a few times. */
    fun <T> stale(times: Int = 3, block: () -> T): T {
        repeat(times - 1) {
            try {
                return block()
            } catch (_: StaleObjectException) {
                device.waitForIdle()
            }
        }
        return block()
    }

    private fun onScreen(): String =
        device.findObjects(By.text(Pattern.compile(".+")))
            .mapNotNull { o -> runCatching { Triple(o.visibleBounds.top, o.visibleBounds.left, o.text) }.getOrNull() }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .take(60)
            .joinToString(" | ") { it.third }

    private fun distance(a: UiObject2, b: UiObject2): Int {
        val p = a.visibleBounds
        val q = b.visibleBounds
        return kotlin.math.abs(p.centerX() - q.centerX()) + kotlin.math.abs(p.centerY() - q.centerY())
    }

    companion object {
        const val WAIT = 8_000L
        const val SETTLE = 400L
        const val LONG = 30_000L
        const val MAX_FLINGS = 10
    }
}

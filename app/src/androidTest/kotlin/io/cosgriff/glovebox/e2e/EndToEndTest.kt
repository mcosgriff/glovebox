package io.cosgriff.glovebox.e2e

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * The app driven from the outside, the way a person uses it: a first launch, a vehicle, its
 * schedule, a logged service, a mileage update, and a backup that is restored.
 *
 * It finds things by the words on the screen, never by anything inside the app, so the same test
 * runs against a minified build (#151).
 *
 * **It needs a fresh install and skips itself otherwise.** It adds a vehicle and then replaces
 * everything with a backup, so it must never run against an app that has someone's data in it; the
 * welcome screen, which only a fresh install shows, is the proof. It also turns the network off
 * while it runs: nothing here needs NHTSA, and a test must not call it.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val pkg: String = instrumentation.targetContext.packageName

    private var step = "starting"

    @Before
    fun start() {
        // Granted up front so Android's own dialog doesn't interrupt the welcome.
        device.executeShellCommand("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        device.executeShellCommand("cmd connectivity airplane-mode enable")
        val context = instrumentation.targetContext
        context.startActivity(context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), LONG)
        assumeTrue(
            "This test replaces everything in the app, so it only runs on a fresh install (the welcome screen wasn't shown).",
            device.wait(Until.hasObject(By.text("Welcome to Glovebox")), LONG),
        )
    }

    @After
    fun finish() {
        device.executeShellCommand("cmd connectivity airplane-mode disable")
    }

    @Test
    fun aVehicleFromFirstLaunchToARestoredBackup() {
        step("add a vehicle by hand")
        tap("Add your first vehicle")
        tap(containing("No VIN?"))
        pick(label = "Year", field = "Type the year", value = "2020")
        pick(label = "Make", field = "Search or type your own", value = "Toyota")
        pick(label = "Model", field = "Search or type your own", value = "4Runner")
        tap("4WD")
        type(into = "Odometer (mi)", text = "45000")
        closeKeyboard()
        tap(scrolledTo(containing("Next: maintenance")))

        step("set up from the maker's schedule")
        see(containing("The maker's schedule for your 2020 4Runner"))
        // Under normal use the four jobs that are only for severe use are listed but not counted.
        see("10 OF 14 SELECTED")
        tap(scrolledTo("Track 10 items"))

        step("vehicle home lists what isn't logged yet")
        see("Not logged yet")
        see("8 TO LOG")
        // The first job by name is the check of the front differential oil.
        see("Inspect front differential oil (4WD)")
        see(containing("Scheduled now, at 45,000 mi"))

        step("Log it ticks that job's own line and saves a record")
        stale { tap(first("Log it")) }
        see("Log service")
        type(into = "Odometer (mi)", text = "45200")
        closeKeyboard()
        tap(scrolledTo("Save service"))
        see("History")
        see("45,200 mi")
        // Its own line: the check, not the change that would also have reset it (#148).
        see("Front differential oil check")
        assertTrue("the change was logged in place of the check", device.findObject(By.text("Front differential oil change")) == null)

        step("the reminder now counts from the record")
        openVehicle()
        see("7 TO LOG")
        see(containing("45,200 mi"))

        step("back up to a file")
        tap("Settings")
        tap(scrolledTo("Encrypted backup"))
        see("Last backup: never")
        type(into = "Backup password", text = PASSWORD)
        type(into = "Confirm password", text = PASSWORD)
        closeKeyboard()
        tap(scrolledTo("Create backup file"))
        // Android's own "save as" screen.
        tap(By.text("SAVE"), timeout = LONG)
        see(containing("Last backup: "), timeout = LONG)
        assertTrue("the backup date wasn't recorded", device.findObject(By.text("Last backup: never")) == null)

        step("change something, so the restore has something to put back")
        device.pressBack()
        openVehicle()
        tap("Update mileage")
        see("Odometer now")
        keypad("46000")
        tap(startingWith("Save"))
        see(containing("46,000 mi"))

        step("restore the backup")
        tap("Settings")
        tap(scrolledTo("Encrypted backup"))
        tap(scrolledTo("Restore from a backup file"))
        tap(By.text("glovebox-backup-${LocalDate.now()}.glovebox"), timeout = LONG)
        type(into = "Backup password", text = PASSWORD)
        closeKeyboard()
        tap("Open backup")
        see(containing("1 vehicle, 1 service record"), timeout = LONG)
        type(into = "Type REPLACE to confirm", text = "REPLACE")
        closeKeyboard()
        tap("Replace and restore")

        step("everything is as it was when the backup was made")
        tap("Garage", timeout = LONG)
        see(containing("45,200 mi"), timeout = LONG)
        see("7 TO LOG")
        assertTrue("the reading added after the backup is still there", device.findObject(By.textContains("46,000 mi")) == null)
    }

    /**
     * Vehicle home. The Garage tab opens the only vehicle directly once per launch and shows the
     * garage list after that, so either may come up.
     */
    private fun openVehicle() {
        tap("Garage")
        if (device.wait(Until.findObject(By.text("Update mileage")), 3_000) == null) tap(containing("2020 4Runner"))
        see("Update mileage")
    }

    // What follows is the small vocabulary the test is written in. Every failure names the step it was on.

    private fun step(name: String) {
        step = name
    }

    private fun containing(text: String): BySelector = By.textContains(text)

    private fun startingWith(text: String): BySelector = By.textStartsWith(text)

    private fun see(text: String, timeout: Long = WAIT): UiObject2 = see(By.text(text), timeout)

    private fun see(selector: BySelector, timeout: Long = WAIT): UiObject2 {
        val found = device.wait(Until.findObject(selector), timeout)
        assertNotNull("While trying to $step: didn't find $selector on the screen. It showed: ${onScreen()}", found)
        return found
    }

    /** The words on the screen, top to bottom, for a failure to say what was there instead. */
    private fun onScreen(): String =
        device.findObjects(By.text(java.util.regex.Pattern.compile(".+")))
            .mapNotNull { o -> runCatching { Triple(o.visibleBounds.top, o.visibleBounds.left, o.text) }.getOrNull() }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .take(60)
            .joinToString(" | ") { it.third }

    private fun tap(text: String, timeout: Long = WAIT) = tap(By.text(text), timeout)

    /** Finds it afresh at the moment of tapping, and again if the screen redrew in between. */
    private fun tap(selector: BySelector, timeout: Long = WAIT) = stale { tap(see(selector, timeout)) }

    private fun tap(target: UiObject2) {
        target.click()
        device.waitForIdle()
        Thread.sleep(SETTLE)
    }

    /** The first on the screen, top to bottom, of several things that say the same. */
    private fun first(text: String): UiObject2 {
        see(text)
        return device.findObjects(By.text(text)).minBy { it.visibleBounds.top }
    }

    /**
     * Scrolls the screen down until [text] is on it, and lets the scrolling stop: tapping something
     * still gliding hits whatever is under the finger by then.
     */
    private fun scrolledTo(text: String): BySelector = scrolledTo(By.text(text))

    private fun scrolledTo(selector: BySelector): BySelector {
        // A screen that has only just opened may not be drawn yet: give it a moment before deciding to scroll.
        if (device.wait(Until.findObject(selector), 2_000) == null) {
            // The screen's own scrolling list, scrolled by the list itself rather than by a swipe that may land on a button.
            device.wait(Until.findObject(By.scrollable(true)), WAIT)?.let { list ->
                stale { list.scrollUntil(Direction.DOWN, Until.findObject(selector)) }
            }
        }
        device.waitForIdle()
        Thread.sleep(SETTLE)
        see(selector)
        return selector
    }

    /** Compose redraws often; an object found a moment ago can be gone. Look again, a few times. */
    private fun <T> stale(times: Int = 3, block: () -> T): T {
        repeat(times - 1) {
            try {
                return block()
            } catch (_: androidx.test.uiautomator.StaleObjectException) {
                device.waitForIdle()
            }
        }
        return block()
    }

    /** Types into the field labelled [into]. */
    private fun type(into: String, text: String) = stale {
        val field = see(into)
        field.click()
        device.waitForIdle()
        // The label is inside the field it names; the field itself is what takes the text.
        val editable = device.findObjects(By.clazz("android.widget.EditText")).minByOrNull { distance(it, see(into)) }
        assertNotNull("While trying to $step: no field to type \"$text\" into for \"$into\".", editable)
        editable!!.text = text
        device.waitForIdle()
    }

    /** The update-mileage sheet has its own keypad, not a text field: a reading is tapped in a digit at a time. */
    private fun keypad(digits: String) {
        digits.forEach { d ->
            // The reading shown above the keys can show the same digit; the key is the lowest one on the screen.
            stale { tap(device.wait(Until.findObjects(By.text(d.toString())), WAIT).maxBy { it.visibleBounds.top }) }
        }
    }

    /** One of the pickers on the add-vehicle form: open it, type the value, take the row that offers it. */
    private fun pick(label: String, field: String, value: String) {
        // Each picker's button sits under its label; with nothing chosen yet it reads "Choose".
        see("Choose")
        stale { tap(device.findObjects(By.text("Choose")).minBy { distance(it, see(label)) }) }
        type(into = field, text = value)
        // The field now says the value too; the row that offers it is the one further down.
        stale { tap(device.wait(Until.findObjects(By.textContains(value)), WAIT).maxBy { it.visibleBounds.top }) }
    }

    /** Back closes the keyboard when it is up, and leaves the screen when it isn't: only press it for the keyboard. */
    private fun closeKeyboard() {
        if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) {
            device.pressBack()
            device.waitForIdle()
        }
    }

    private fun distance(a: UiObject2, b: UiObject2): Int {
        val p = a.visibleBounds
        val q = b.visibleBounds
        return kotlin.math.abs(p.centerX() - q.centerX()) + kotlin.math.abs(p.centerY() - q.centerY())
    }

    private companion object {
        const val WAIT = 8_000L
        const val SETTLE = 400L
        const val LONG = 30_000L
        const val PASSWORD = "a made-up phrase for the test"
    }
}

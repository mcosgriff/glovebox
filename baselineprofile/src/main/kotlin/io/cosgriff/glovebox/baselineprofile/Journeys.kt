package io.cosgriff.glovebox.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import io.cosgriff.glovebox.baselineprofile.Screen.Companion.LONG

/** The release app's id; the plugin's `nonMinifiedRelease` and `benchmarkRelease` builds keep it. */
const val PACKAGE = "io.cosgriff.glovebox"

/**
 * Wipes the app and starts it as a first launch: the welcome. The phone's network is off, so nothing
 * here calls NHTSA, and notifications are allowed up front so Android's dialog doesn't interrupt.
 * Only ever run on the managed emulator (see build.gradle.kts): this clears the app's data.
 */
fun MacrobenchmarkScope.freshStart(screen: Screen) {
    screen.step("start as a first launch")
    killProcess()
    device.executeShellCommand("pm clear $packageName")
    device.executeShellCommand("pm grant $packageName android.permission.POST_NOTIFICATIONS")
    device.executeShellCommand("cmd connectivity airplane-mode enable")
    startActivityAndWait()
    screen.see("Welcome to Glovebox", LONG)
}

/** From the welcome, a 2020 4Runner added by hand and set up from the maker's schedule; ends on vehicle home. */
fun MacrobenchmarkScope.addVehicle(screen: Screen) = with(screen) {
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
    tap(scrolledTo(containing("Track ")))
    see("Not logged yet")
    see("Update mileage")
}

/** Vehicle home, as a cold start lands on it with one vehicle, or through the Garage tab. */
fun MacrobenchmarkScope.openVehicle(screen: Screen) = with(screen) {
    step("open vehicle home")
    if (!has("Update mileage", 3_000)) {
        tap("Garage")
        if (!has("Update mileage", 3_000)) tap(containing("2020 4Runner"))
    }
    see("Update mileage")
}

fun MacrobenchmarkScope.scrollVehicleHome(screen: Screen) = with(screen) {
    step("scroll vehicle home")
    see("Update mileage")
    flingDownAndUp()
}

fun MacrobenchmarkScope.reminders(screen: Screen) = with(screen) {
    step("the Reminders tab")
    tap("Reminders")
    see(containing("4Runner"))
    flingDownAndUp()
}

/**
 * Log service from a reminder on vehicle home, as `EndToEndTest` does: a reading, another category
 * opened and a line ticked there too, saved. Ends on History.
 */
fun MacrobenchmarkScope.logService(screen: Screen) = with(screen) {
    openVehicle(screen)
    step("log a service")
    stale { tap(first("Log it")) }
    see("Log service")
    type(into = "Odometer (mi)", text = "45200")
    closeKeyboard()
    tap(scrolledTo("Brakes"))
    tap(scrolledTo("Front pads"))
    tap(scrolledTo("Save service"))

    step("History after saving")
    see("History", LONG)
    see("45,200 mi")
}

fun MacrobenchmarkScope.history(screen: Screen) = with(screen) {
    step("the History tab")
    tap("History")
    see("45,200 mi")
    tap(containing("45,200 mi"))
    see("Front pads")
    device.pressBack()
    device.waitForIdle()
}

/** Vehicle details, then the Maintenance schedule and its Milestones. */
fun MacrobenchmarkScope.scheduleAndMilestones(screen: Screen) = with(screen) {
    openVehicle(screen)
    step("the Maintenance schedule")
    tap(By.desc("Vehicle details"))
    tap(scrolledTo("Maintenance schedule"))
    see(containing(" TRACKED"))
    flingDownAndUp()

    step("Milestones")
    tap(scrolledTo("Milestones"))
    see(containing("mi"))
    flingDownAndUp()
}

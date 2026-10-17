package io.cosgriff.glovebox.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the app's baseline profile (#222): the classes and methods on the paths below, which Android
 * then compiles ahead of time instead of interpreting them on the first few runs.
 *
 * Run with `./gradlew :app:generateReleaseBaselineProfile`, which builds the app, runs this on the managed
 * emulator and copies the result to `app/src/release/generated/baselineProfiles/`. Commit that. Run it
 * again when the screens change a lot, not on every build.
 *
 * Every pass starts from a wiped app, so each one goes through the welcome.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /** Launch to the welcome. Also the startup profile, which R8 uses to put that code first in the dex. */
    @Test
    fun startup() = rule.collect(PACKAGE, maxIterations = 5, stableIterations = 2, includeInStartupProfile = true) {
        freshStart(Screen(device))
    }

    @Test
    fun journeys() = rule.collect(PACKAGE, maxIterations = 5, stableIterations = 2) {
        val screen = Screen(device)
        freshStart(screen)
        addVehicle(screen)
        scrollVehicleHome(screen)
        reminders(screen)
        logService(screen)
        history(screen)
        scheduleAndMilestones(screen)
    }
}

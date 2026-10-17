package io.cosgriff.glovebox.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures what the baseline profile is for (#222): a cold start to vehicle home, and scrolling it.
 * Startup is timed to the first frame and to vehicle home drawn with its reminders (`ReportDrawn`).
 * Each is run without any ahead-of-time compilation, as an install is before Android has seen the app
 * run, and with the baseline profile, as an install is once `profileinstaller` has written it.
 *
 * Run with `./gradlew :baselineprofile:pixel6Api34BenchmarkReleaseAndroidTest`. The figures come from an
 * emulator: compare them with each other, not with a phone.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithoutProfile() = startup(CompilationMode.None())

    @Test
    fun startupWithProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun scrollWithoutProfile() = scroll(CompilationMode.None())

    @Test
    fun scrollWithProfile() = scroll(CompilationMode.Partial(BaselineProfileMode.Require))

    /** A cold start with one vehicle, which opens straight onto vehicle home. */
    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
        setupBlock = { withOneVehicle() },
    ) {
        startActivityAndWait()
        Screen(device).see("Update mileage")
    }

    private fun scroll(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = mode,
        // No startup mode: a cold one kills the app after setup, and this needs it open on vehicle home.
        startupMode = null,
        iterations = ITERATIONS,
        setupBlock = {
            withOneVehicle()
            killProcess()
            startActivityAndWait()
            openVehicle(Screen(device))
        },
    ) {
        scrollVehicleHome(Screen(device))
    }

    /**
     * The app with one vehicle in it, made once per run from a wiped app. It is kept for the rest of the
     * run: the timings are of an app that is in use, not of the welcome.
     */
    private fun MacrobenchmarkScope.withOneVehicle() {
        if (prepared) return
        val screen = Screen(device)
        freshStart(screen)
        addVehicle(screen)
        killProcess()
        prepared = true
    }

    private companion object {
        const val ITERATIONS = 10
        var prepared = false
    }
}

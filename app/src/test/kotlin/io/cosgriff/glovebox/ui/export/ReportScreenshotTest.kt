package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.domain.ExportJob
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.Exports
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.SummaryLine
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.screens.shoot
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The PDF report's two pages from the made-up sample, drawn as the preview draws them; the paper is light in either theme. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, sdk = [35])
class ReportScreenshotTest {
    @Test
    fun pages() {
        val today = LocalDate.of(2026, 10, 24)
        val renderer = ReportRenderer(ReportFonts(RuntimeEnvironment.getApplication()))
        // The brake check also did a line in each of the other groups, so every colour is on the page.
        val more = listOf("Sway bar links" to ServiceGroup.SUSPENSION, "Fuel filter" to ServiceGroup.FUEL_EXHAUST, "Wiper blades" to ServiceGroup.OTHER)
        // Made-up prices (#481): in June the shop priced the oil change and rotation as one job, so neither chip
        // carries it; in October the owner's oil change is priced alone, on its chip, and the rotation not at all.
        val records = SampleReport.records.mapIndexed { i, r ->
            when (i) {
                1 -> r.copy(summary = r.summary.copy(lines = r.summary.lines + more.map { SummaryLine(it.first, it.second) }))
                2 -> r.withJobs(listOf(ExportJob("Oil change and tire rotation", 4_000, 5_120)), listOf(0, 0), costCents = 9_850)
                3 -> r.withJobs(listOf(ExportJob(null, null, 3_890)), listOf(0, null))
                else -> r
            }
        }
        val pages = renderer.layout(Exports.document(listOf(SampleReport.vehicle(today, records)), ExportOptions(), today))
        pages.forEachIndexed { i, page -> renderer.bitmap(page, i + 1, pages.size, 816).shoot("report_page${i + 1}") }
    }
}

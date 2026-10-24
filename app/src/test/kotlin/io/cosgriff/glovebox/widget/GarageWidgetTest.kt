package io.cosgriff.glovebox.widget

import android.content.ComponentName
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.compose
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.notify.Destination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class GarageWidgetTest {
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph

    @After fun close() = graph.close()

    /** The widget drawn at [size] and put into views, as a launcher would. */
    private fun draw(size: DpSize = DpSize(320.dp, 400.dp)): View = runBlocking {
        GarageWidget().compose(app, size = size).apply(app, FrameLayout(app))
    }

    private fun View.texts(): List<String> = when (this) {
        is TextView -> listOf(text.toString())
        is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).texts() }
        else -> emptyList()
    }

    private fun View.withText(text: String): View? = when {
        this is TextView && this.text.toString() == text -> this
        this is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).withText(text) }
        else -> null
    }

    /** Taps the view showing [text], or the nearest parent that answers taps. */
    private fun View.tap(text: String) {
        var view: View? = checkNotNull(withText(text)) { "No $text in ${texts()}" }
        while (view != null && !view.hasOnClickListeners()) view = view.parent as? View
        checkNotNull(view) { "Nothing to tap around $text" }.performClick()
    }

    private fun opened(): Destination? = Destination.from(shadowOf(app).nextStartedActivity)

    @Test
    fun `with no vehicles it says so, and a tap opens the app`() {
        val widget = draw()
        assertEquals(listOf(app.getString(R.string.widget_empty)), widget.texts())
        widget.tap(app.getString(R.string.widget_empty))
        assertEquals(null, opened())
    }

    @Test
    fun `each vehicle's row shows its name, next service and chip, and opens that vehicle`() {
        val (tacoma, _) = graph.overdueVehicle()
        graph.addVehicle(make = "Honda", model = "Civic", year = 2016)
        val widget = draw()
        val texts = widget.texts()
        assertTrue(texts.toString(), texts.containsAll(listOf("2019 Toyota Tacoma", "2016 Honda Civic")))
        val tacomaRow = widgetViewOf(graph).rows.first { it.vehicleId == tacoma }
        assertTrue(texts.toString(), texts.containsAll(listOf(tacomaRow.next, checkNotNull(tacomaRow.pill).text)))

        widget.tap("2019 Toyota Tacoma")
        assertEquals(Destination.VehicleHome(tacoma), opened())
    }

    @Test
    fun `vehicles past five are counted on a last row, which opens the app`() {
        repeat(6) { graph.addVehicle(model = "Model $it") }
        val widget = draw(DpSize(320.dp, 800.dp))
        val more = app.resources.getQuantityString(R.plurals.widget_more, 1, "1")
        assertTrue(widget.texts().toString(), widget.texts().contains(more))
        widget.tap(more)
        assertEquals(null, opened())
    }

    @Test
    fun `rows that don't fit the widget's height go in a list that scrolls`() {
        repeat(4) { graph.addVehicle(model = "Model $it") }
        val short = draw(DpSize(320.dp, 120.dp))
        // The rows are the list's items, not laid out in the widget itself.
        assertTrue(short.texts().toString(), short.texts().none { it.startsWith("2019 Toyota") })
    }

    @Test
    fun `the widget is offered only while its switch is on`() {
        val widgets = GarageWidgets(app)
        val receiver = ComponentName(app, GarageWidgetReceiver::class.java)
        widgets.follow(true)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, app.packageManager.getComponentEnabledSetting(receiver))
        widgets.follow(true)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, app.packageManager.getComponentEnabledSetting(receiver))
        widgets.follow(false)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, app.packageManager.getComponentEnabledSetting(receiver))
    }

    @Test
    fun `the receiver serves the garage widget`() {
        assertTrue(GarageWidgetReceiver().glanceAppWidget is GarageWidget)
    }
}

private fun widgetViewOf(graph: io.cosgriff.glovebox.ShellGraph): WidgetView =
    runBlocking { WidgetView.of(graph.get<io.cosgriff.glovebox.ui.garage.GarageOverview>().garage.first()) }

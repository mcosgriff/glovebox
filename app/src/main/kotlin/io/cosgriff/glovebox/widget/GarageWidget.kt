package io.cosgriff.glovebox.widget

import io.cosgriff.glovebox.notify.launchIntent
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.core.net.toUri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Attention
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.HomeWidget
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.ui.garage.label
import io.cosgriff.glovebox.ui.garage.nextLine
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.DarkScheme
import io.cosgriff.glovebox.ui.theme.LightScheme
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The home-screen widget (#238): each active vehicle with its garage chip and "Next:" line, worded and
 * worked out exactly as on the garage list. Nothing on it acts; a row opens that vehicle's home, behind
 * the lock, as a notification does. Offered only while Settings' switch is on ([GarageWidgets]).
 */
class GarageWidget : GlanceAppWidget() {
    // Launchers such as Kvaesitso give the widget an exact, often wide and short, size.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val garage = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).garage().garage.first()
        val view = WidgetView.of(garage)
        provideContent { Content(context, view) }
    }
}

class GarageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GarageWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun garage(): GarageOverview
}

/** Everything the widget draws, already in words, so it can be compared to skip a refresh that changes nothing. */
data class WidgetView(val rows: List<WidgetRow>, val more: Int) {
    companion object {
        fun of(garage: Garage): WidgetView {
            val res = AppText.res
            val chosen = HomeWidget.rows(garage.vehicles)
            return WidgetView(
                chosen.shown.map { g ->
                    val v = g.vehicle
                    val name = HomeWidget.name(v.nickname, v.year, v.make, v.model)
                    val title = name.title ?: res.getString(R.string.unknown_vehicle)
                    WidgetRow(
                        vehicleId = v.id,
                        name = name.nickname?.let { res.getString(R.string.widget_name_nicknamed, it, title) } ?: title,
                        pill = g.comingUp.attention?.let { Pill(it.label(res), it.kind) },
                        next = g.comingUp.nextLine(res, v.unit),
                    )
                },
                chosen.more,
            )
        }
    }
}

data class WidgetRow(val vehicleId: Long, val name: String, val pill: Pill?, val next: String)

data class Pill(val text: String, val kind: PillKind)

/** The garage chip's colours: solid accent for overdue, grey for due soon or to log, teal for up to date. */
enum class PillKind { OVERDUE, SOON, OK }

private val Attention.kind: PillKind
    get() = when {
        toLog > 0 -> PillKind.SOON
        state == DueState.OVERDUE -> PillKind.OVERDUE
        state == DueState.DUE_SOON -> PillKind.SOON
        else -> PillKind.OK
    }

/** The canvas colours, light and dark by the phone's own setting; dynamic colour stays off. */
private object Colors {
    private fun pick(light: Color, dark: Color): ColorProvider = ColorProvider(day = light, night = dark)
    val surface = pick(LightScheme.surface, DarkScheme.surface)
    val ink = pick(LightScheme.onSurface, DarkScheme.onSurface)
    val muted = pick(LightScheme.onSurfaceVariant, DarkScheme.onSurfaceVariant)
    val line = pick(LightScheme.outlineVariant, DarkScheme.outlineVariant)

    fun pillBackground(kind: PillKind) = when (kind) {
        PillKind.OVERDUE -> pick(LightScheme.primary, DarkScheme.primary)
        PillKind.SOON -> pick(LightScheme.surfaceVariant, DarkScheme.surfaceVariant)
        PillKind.OK -> pick(LightScheme.tertiaryContainer, DarkScheme.tertiaryContainer)
    }

    fun pillText(kind: PillKind) = when (kind) {
        PillKind.OVERDUE -> pick(LightScheme.onPrimary, DarkScheme.onPrimary)
        PillKind.SOON -> pick(statusColors(dark = false).soon, statusColors(dark = true).soon)
        PillKind.OK -> pick(LightScheme.onTertiaryContainer, DarkScheme.onTertiaryContainer)
    }
}

/** Opens Glovebox at [destination], or as it was left when there is none. The lock comes first either way. */
private fun open(context: Context, destination: Destination?) = actionStartActivity(
    destination.launchIntent(context).apply {
        // Rows differ only in their extras, which Android doesn't compare: a distinct address keeps each
        // row's tap going to its own vehicle.
        data = "glovebox-widget:${(destination as? Destination.VehicleHome)?.vehicleId ?: "app"}".toUri()
    },
)

@Composable
private fun Content(context: Context, view: WidgetView) {
    val res = AppText.res
    Box(
        GlanceModifier.fillMaxSize().appWidgetBackground().background(Colors.surface).cornerRadius(R.dimen.widget_radius),
    ) {
        if (view.rows.isEmpty()) {
            Box(
                GlanceModifier.fillMaxSize().padding(16.dp).clickable(open(context, null)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(res.getString(R.string.widget_empty), style = TextStyle(color = Colors.muted, fontSize = 14.sp))
            }
            return@Box
        }
        // Everything fits: centred in the widget, so a one-vehicle widget isn't a row with space under it.
        // Otherwise a list that scrolls.
        val needed = ROW_HEIGHT * view.rows.size + DIVIDER * (view.rows.size - 1) + if (view.more > 0) DIVIDER + MORE_HEIGHT else 0.dp
        if (needed <= LocalSize.current.height) {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                view.rows.forEach { row -> RowWithDivider(context, row, view) }
                if (view.more > 0) More(context, view.more)
            }
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(view.rows, itemId = { it.vehicleId }) { row -> RowWithDivider(context, row, view) }
                if (view.more > 0) item { More(context, view.more) }
            }
        }
    }
}

private val ROW_HEIGHT = 56.dp
private val MORE_HEIGHT = 48.dp
private val DIVIDER = 1.dp

@Composable
private fun RowWithDivider(context: Context, row: WidgetRow, view: WidgetView) {
    Column(GlanceModifier.fillMaxWidth()) {
        VehicleRow(context, row)
        if (row != view.rows.last() || view.more > 0) {
            Box(GlanceModifier.fillMaxWidth().height(DIVIDER).padding(horizontal = 16.dp)) {
                Box(GlanceModifier.fillMaxSize().background(Colors.line)) {}
            }
        }
    }
}

@Composable
private fun More(context: Context, more: Int) {
    Box(
        GlanceModifier.fillMaxWidth().height(MORE_HEIGHT).padding(horizontal = 16.dp).clickable(open(context, null)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            AppText.res.getQuantityString(R.plurals.widget_more, more, more.asNumber()),
            style = TextStyle(color = Colors.muted, fontSize = 13.sp),
        )
    }
}

@Composable
private fun VehicleRow(context: Context, row: WidgetRow) {
    Row(
        GlanceModifier.fillMaxWidth().height(ROW_HEIGHT).padding(horizontal = 16.dp)
            .clickable(open(context, Destination.VehicleHome(row.vehicleId))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(GlanceModifier.defaultWeight()) {
            Text(row.name, style = TextStyle(color = Colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Text(row.next, style = TextStyle(color = Colors.muted, fontSize = 13.sp), maxLines = 1)
        }
        row.pill?.let { pill ->
            Spacer(GlanceModifier.width(8.dp))
            Box(
                GlanceModifier.background(Colors.pillBackground(pill.kind)).cornerRadius(12.dp).padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    pill.text,
                    style = TextStyle(
                        color = Colors.pillText(pill.kind),
                        fontSize = 12.sp,
                        fontWeight = if (pill.kind == PillKind.OVERDUE) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Keeps the widget in step with Settings: its receiver is enabled only while the switch is on, so the
 * launcher offers it only then, and disabling it removes any placed widget. Also refreshes it.
 */
@Singleton
class GarageWidgets @Inject constructor(@ApplicationContext private val context: Context) {
    private val receiver = ComponentName(context, GarageWidgetReceiver::class.java)

    fun follow(on: Boolean) {
        val wanted = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val pm = context.packageManager
        if (pm.getComponentEnabledSetting(receiver) != wanted) {
            pm.setComponentEnabledSetting(receiver, wanted, PackageManager.DONT_KILL_APP)
        }
    }

    /** Redraws every placed widget from the database; nothing happens when none is placed. */
    suspend fun refresh() = GarageWidget().updateAll(context)
}

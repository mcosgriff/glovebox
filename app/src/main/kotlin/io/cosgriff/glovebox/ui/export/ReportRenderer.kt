package io.cosgriff.glovebox.ui.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueNextState
import io.cosgriff.glovebox.domain.ExportDocument
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportText
import io.cosgriff.glovebox.domain.ExportPhrase
import io.cosgriff.glovebox.domain.ReportChart
import io.cosgriff.glovebox.domain.ReportModel
import io.cosgriff.glovebox.domain.ReportStatus
import io.cosgriff.glovebox.domain.Reports
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.Visits
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.monthYear
import io.cosgriff.glovebox.ui.service.photoCount
import io.cosgriff.glovebox.ui.theme.groupColor
import java.io.OutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

/** The fonts the report is drawn in: IBM Plex, bundled; the system's if one can't be loaded. */
@Singleton
class ReportFonts @Inject constructor(@ApplicationContext context: Context) {
    private fun Context.font(@FontRes id: Int, fallback: Typeface): Typeface = runCatching { ResourcesCompat.getFont(this, id) }.getOrNull() ?: fallback

    val sans: Typeface = context.font(R.font.ibm_plex_sans_regular, Typeface.DEFAULT)
    val sansSemibold: Typeface = context.font(R.font.ibm_plex_sans_semibold, Typeface.DEFAULT_BOLD)
    val sansBold: Typeface = context.font(R.font.ibm_plex_sans_bold, Typeface.DEFAULT_BOLD)
    val mono: Typeface = context.font(R.font.ibm_plex_mono_medium, Typeface.MONOSPACE)
    val monoSemibold: Typeface = context.font(R.font.ibm_plex_mono_semibold, Typeface.MONOSPACE)
}

/** One page of the report, laid out but not yet drawn. */
sealed interface ReportPage {
    val model: ReportModel

    /** Page 1 (design: ReportPage1): the infographic. */
    data class Summary(override val model: ReportModel) : ReportPage

    /** Page 2 and on (design: ReportPage2): the service log and the schedule, as many pages as they take. */
    class Log(override val model: ReportModel, val continued: Boolean, val placed: List<Placed>) : ReportPage

    /** One receipt photo a page, decrypted in memory only when the page is drawn. */
    data class Photo(override val model: ReportModel, val record: ExportRecord, val fileName: String) : ReportPage
}

/** Something drawn on a log page: [height] tall at the width it was made for, drawn with its top left at x, y. */
class Block(val height: Float, val draw: Canvas.(x: Float, y: Float) -> Unit)

class Placed(val x: Float, val y: Float, val block: Block)

/**
 * Draws the PDF report with Android's own [Canvas], laid out in the design's pixels (816 × 1056, US Letter at
 * 96 dpi). A PDF page is 612 × 792 points, so it is drawn at 0.75; a preview is the same drawing into a [Bitmap],
 * so it always matches the file. The paper is white whatever the app's theme.
 */
class ReportRenderer(private val fonts: ReportFonts) {
    private val words = ExportText(AppExportWords)

    /** Every page of [doc], in order: each vehicle's summary, log pages, then its photos when they are included. */
    fun layout(doc: ExportDocument): List<ReportPage> = Reports.build(doc).flatMap { model ->
        listOf<ReportPage>(ReportPage.Summary(model)) + logPages(model) +
            model.vehicle.records.flatMap { r -> r.photoFiles.map { ReportPage.Photo(model, r, it) } }
    }

    /** Writes [doc] as a PDF; [loadPhoto] decrypts a photo into memory, never to a file. */
    suspend fun writePdf(doc: ExportDocument, out: OutputStream, loadPhoto: suspend (fileName: String) -> Bitmap?) {
        val pages = layout(doc)
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { i, page ->
                val photo = (page as? ReportPage.Photo)?.let { loadPhoto(it.fileName) }
                val p = pdf.startPage(PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, i + 1).create())
                p.canvas.scale(PDF_WIDTH / WIDTH, PDF_HEIGHT / HEIGHT)
                draw(page, p.canvas, i + 1, pages.size, photo)
                pdf.finishPage(p)
                photo?.recycle()
            }
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    /** [page] as a picture [widthPx] wide, for the preview. A photo page shows its caption only. */
    fun bitmap(page: ReportPage, number: Int, total: Int, widthPx: Int): Bitmap {
        val heightPx = (widthPx * HEIGHT / WIDTH).roundToInt()
        val bitmap = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(widthPx / WIDTH, heightPx / HEIGHT)
        draw(page, canvas, number, total, photo = null)
        return bitmap
    }

    fun draw(page: ReportPage, canvas: Canvas, number: Int, total: Int, photo: Bitmap?) {
        canvas.drawColor(Color.WHITE)
        when (page) {
            is ReportPage.Summary -> summary(canvas, page.model)
            is ReportPage.Log -> {
                logHeader(canvas, page.model, page.continued)
                page.placed.forEach { with(it.block) { canvas.draw(it.x, it.y) } }
            }
            is ReportPage.Photo -> photoPage(canvas, page, photo)
        }
        footer(canvas, if (page is ReportPage.Summary) footerFirst(page.model, number + 1) else s(R.string.report_footer, page.model.generatedOn.format(longDate)), number, total)
    }

    // ---- Page 1 ----

    private fun summary(c: Canvas, m: ReportModel) {
        val v = m.vehicle
        var y = TOP

        // Brand row
        c.drawRoundRect(RectF(LEFT, y, LEFT + 28f, y + 28f), 8f, 8f, fill(ACCENT))
        wrench(c, LEFT + 6f, y + 6f, 16f)
        val brand = paint(14f, fonts.sansBold).apply { letterSpacing = 0.06f }
        c.textMiddle(s(R.string.report_brand), LEFT + 38f, y + 14f, brand)
        val generated = s(R.string.report_generated, m.generatedOn.format(longDate))
        c.textMiddle(generated, RIGHT - paint(13f, fonts.sans).measureText(generated), y + 14f, paint(13f, fonts.sans, MUTED))
        y += 28f + 14f

        // Title, specs and VIN, and the status pill
        val pill = statusText(m.status)
        val pillPaint = paint(13f, fonts.sansSemibold)
        val pillWidth = pill?.let { 12f + 18f + 8f + pillPaint.measureText(it) + 12f } ?: 0f
        val titlePaint = fitted(v.title, 40f, 24f, fonts.sansBold, CONTENT - pillWidth - 24f)
        c.textTop(TextUtils.ellipsize(v.title, titlePaint, CONTENT - pillWidth - 24f, TextUtils.TruncateAt.END).toString(), LEFT, y, titlePaint)
        y += titlePaint.textSize * 1.05f + 4f
        val sub = paint(14f, fonts.sans, INK2)
        var x = LEFT
        v.specs?.let {
            // The separator only goes in when a VIN follows.
            val shown = if (v.vin != null) "$it · " else it
            c.textTop(shown, x, y, sub)
            x += sub.measureText(shown)
        }
        v.vin?.let { vin ->
            val label = s(R.string.report_vin) + " "
            c.textTop(label, x, y, sub)
            c.textTop(vin, x + sub.measureText(label), y, paint(14f, fonts.monoSemibold, INK2))
        }
        y += 18f
        pill?.let {
            val top = y - 34f
            c.drawRoundRect(RectF(RIGHT - pillWidth, top, RIGHT, y), 10f, 10f, fill(GROUND))
            statusIcon(c, RIGHT - pillWidth + 12f, top + 8f, 18f, m.status)
            c.textMiddle(it, RIGHT - pillWidth + 38f, top + 17f, pillPaint)
        }
        y += 26f

        // Stat tiles
        y += tiles(c, m, y) + 26f

        m.chart?.let { chart ->
            sectionHeading(c, s(R.string.report_chart_title), y)
            legend(c, y)
            y += 24f + 8f
            chart(c, chart, v.unit, LEFT, y)
            y += CHART_HEIGHT + 26f
        }

        if (m.dueNext.isNotEmpty()) {
            sectionHeading(c, s(R.string.report_due_next), y)
            val caption = s(R.string.report_due_next_caption)
            val small = paint(12f, fonts.sans, INK2)
            c.textTop(caption, RIGHT - small.measureText(caption), y + 6f, small)
            y += 24f + 10f
            m.dueNext.forEach { row ->
                val name = paint(14f, fonts.sansSemibold)
                c.groupSquare(RectF(LEFT, y + 5f, LEFT + 10f, y + 15f), row.category)
                c.textMiddle(TextUtils.ellipsize(row.name, name, 196f - 18f, TextUtils.TruncateAt.END).toString(), LEFT + 18f, y + 10f, name)
                val barLeft = LEFT + 196f + 14f
                val barRight = RIGHT - 160f - 14f
                c.drawRoundRect(RectF(barLeft, y + 5f, barRight, y + 15f), 5f, 5f, fill(LINE_SOFT))
                val share = row.used.coerceIn(0.02, 1.0).toFloat()
                val urgent = row.state == DueNextState.OVERDUE || row.state == DueNextState.DUE_SOON
                c.drawRoundRect(RectF(barLeft, y + 5f, barLeft + (barRight - barLeft) * share, y + 15f), 5f, 5f, fill(if (urgent) INK else INK2))
                val at = row.dueAt?.asDistance(v.unit) ?: row.dueBy?.format(longDate) ?: s(R.string.report_none)
                c.textMiddle(at, RIGHT - 160f, y + 10f, paint(12f, fonts.mono))
                val state = s(dueState(row.state))
                val statePaint = paint(12f, if (urgent) fonts.sansBold else fonts.sans)
                c.textMiddle(state, RIGHT - statePaint.measureText(state), y + 10f, statePaint)
                y += 20f + 10f
            }
            y += 16f
        }

        // Recalls and who did the work, side by side
        val half = (CONTENT - 16f) / 2f
        val cards = listOfNotNull(
            m.vehicle.recalls.takeIf { m.status != null || it.isNotEmpty() }?.let { recalls -> { cx: Float, cy: Float, h: Float? -> recallsCard(c, recalls, cx, cy, half, h) } },
            { cx: Float, cy: Float, h: Float? -> whoCard(c, m.diy, m.shop, cx, cy, half, h) },
        )
        val height = cards.maxOf { it(0f, 0f, null) }
        cards.forEachIndexed { i, card -> card(LEFT + i * (half + 16f), y, height) }
    }

    private fun statusText(status: ReportStatus?): String? = when (status) {
        null -> null
        ReportStatus.Current -> s(R.string.report_status_current)
        is ReportStatus.Overdue -> q(R.plurals.report_status_overdue, status.count)
        is ReportStatus.ToLog -> q(R.plurals.report_status_to_log, status.count)
    }

    /** Returns the tiles' height. */
    private fun tiles(c: Canvas, m: ReportModel, top: Float): Float {
        val unit = m.vehicle.unit
        val st = m.stats
        val miles = unit == DistanceUnit.MILES
        val tiles = listOfNotNull(
            (st.odometer?.asNumber() ?: s(R.string.report_none)) to s(if (miles) R.string.report_tile_odometer_mi else R.string.report_tile_odometer_km),
            st.services.asNumber() to s(R.string.report_tile_services),
            (st.sinceFirst?.asNumber() ?: s(R.string.report_none)) to s(if (miles) R.string.report_tile_since_first_mi else R.string.report_tile_since_first_km),
            st.recalls?.let { s(R.string.report_tile_recalls_value, it.completed.asNumber(), it.total.asNumber()) to s(R.string.report_tile_recalls) },
        )
        val w = (CONTENT - 12f * (tiles.size - 1)) / tiles.size
        val label = paint(13f, fonts.sans, INK2)
        val layouts = tiles.map { layout(it.second, label, w - 32f) }
        val h = 14f + 31f + 4f + layouts.maxOf { it.height } + 14f
        val value = paint(28f, fonts.monoSemibold)
        tiles.forEachIndexed { i, (v, _) ->
            val x = LEFT + i * (w + 12f)
            c.drawRoundRect(RectF(x + 0.5f, top + 0.5f, x + w - 0.5f, top + h - 0.5f), 14f, 14f, stroke(LINE, 1f))
            c.textTop(TextUtils.ellipsize(v, value, w - 32f, TextUtils.TruncateAt.END).toString(), x + 16f, top + 14f, value)
            c.withTranslation(x + 16f, top + 14f + 31f + 4f) { layouts[i].draw(this) }
        }
        return h
    }

    private fun legend(c: Canvas, top: Float) {
        val p = paint(12f, fonts.sans, INK2)
        val done = s(R.string.report_legend_done)
        val next = s(R.string.report_legend_next)
        var x = RIGHT - p.measureText(next)
        val mid = top + 12f
        c.textMiddle(next, x, mid, p)
        x -= 6f + 10f
        c.drawCircle(x + 5f, mid, 3.8f, fill(Color.WHITE))
        c.drawCircle(x + 5f, mid, 3.8f, stroke(INK2, 1.5f).apply { pathEffect = DashPathEffect(floatArrayOf(2f, 1.6f), 0f) })
        x -= 14f + p.measureText(done)
        c.textMiddle(done, x, mid, p)
        c.drawCircle(x - 6f - 5f, mid, 4.5f, fill(INK2))
    }

    /**
     * Services by odometer, a lane a group. Labels never sit on each other: a tick label under the "Now" marker
     * is left out, a next-due label close to "Now" moves to the right of its dot, and only as many service dates
     * are labelled along the top as fit, the latest first.
     */
    private fun chart(c: Canvas, chart: ReportChart, unit: DistanceUnit, left: Float, top: Float) {
        val x0 = left + 160f
        val x1 = left + 700f
        fun x(at: Int): Float = x0 + (at - chart.low).toFloat() / (chart.high - chart.low).coerceAtLeast(1) * (x1 - x0)
        val nowX = chart.now?.let(::x)
        val small = chart.high - chart.low < SMALL_AXIS

        // Dates of service along the top, newest kept first when two would touch.
        val dateP = paint(12f, fonts.sans, INK2)
        val labelled = mutableListOf<Pair<Float, String>>()
        chart.visits.sortedByDescending { it.date }.forEach { visit ->
            val vx = x(visit.odometer)
            val text = visit.date.format(monthYear)
            if (labelled.none { abs(it.first - vx) < (dateP.measureText(text) + dateP.measureText(it.second)) / 2f + 8f }) labelled += vx to text
        }
        labelled.forEach { (vx, text) ->
            c.drawLine(vx, top + 30f, vx, top + 222f, stroke(LINE_SOFT, 1f))
            c.textTop(text, vx - dateP.measureText(text) / 2f, top + 2f, dateP)
        }

        val n = chart.lanes.size
        val spacing = if (n <= 1) 0f else minOf(44f, (210f - 56f) / (n - 1))
        val dueP = paint(11f, fonts.mono, INK2)
        chart.lanes.forEachIndexed { i, lane ->
            val y = top + 56f + i * spacing
            val color = categoryColor(lane.category)
            // A long name steps down to 11px before it is cut short.
            val name = categoryName(lane.category)
            val p = paint(13f, fonts.sansSemibold).apply { while (measureText(name) > 130f && textSize > 11f) textSize -= 0.5f }
            c.textMiddle(TextUtils.ellipsize(name, p, 130f, TextUtils.TruncateAt.END).toString(), left, y, p)
            c.groupSquare(RectF(left + 136f, y - 5f, left + 146f, y + 5f), lane.category)
            c.drawLine(left + 150f, y, left + CONTENT, y, stroke(LINE, 1f))
            lane.nextDue?.let { due ->
                val nx = x(due)
                val from = lane.done.lastOrNull()?.let { x(it) + 8f } ?: (left + 150f)
                c.drawLine(from, y, nx, y, stroke(color, 2f).apply { pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f) })
            }
            lane.done.forEach { at ->
                c.drawCircle(x(at), y, 8f, fill(Color.WHITE))
                if (lane.category == ServiceGroup.OTHER) c.drawCircle(x(at), y, 5f, stroke(color, 2f)) else c.drawCircle(x(at), y, 6f, fill(color))
            }
            lane.nextDue?.let { due ->
                val nx = x(due)
                c.drawCircle(nx, y, 6f, fill(Color.WHITE))
                c.drawCircle(nx, y, 6f, stroke(color, 2f).apply { pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f) })
                val label = if (small) due.asNumber() else thousands(due)
                val w = dueP.measureText(label)
                val nearNow = nowX != null && abs(nx - nowX) < 40f
                val (lx, ly) = if (nearNow) (if (nx >= nowX) nx + 10f else nx - 10f - w) to y else nx - w / 2f to y - 17f
                c.drawRect(lx - 2f, ly - 8f, lx + w + 2f, ly + 8f, fill(Color.WHITE))
                c.textMiddle(label, lx, ly, dueP)
            }
        }

        nowX?.let { c.drawLine(it, top + 26f, it, top + 226f, stroke(INK, 1.5f)) }
        c.drawLine(left + 150f, top + 226f, left + CONTENT, top + 226f, stroke(AXIS, 1f))
        val tickP = paint(11f, fonts.mono, MUTED)
        val nowLabel = chart.now?.let { if (small) it.asDistance(unit) else s(R.string.report_now, compact(it)) }
        val nowW = nowLabel?.let { maxOf(76f, paint(11f, fonts.monoSemibold).measureText(it) + 12f) } ?: 0f
        chart.ticks.forEach { t ->
            val tx = x(t)
            if (tx < left + 150f || tx > left + CONTENT) return@forEach
            c.drawLine(tx, top + 226f, tx, top + 231f, stroke(AXIS, 1f))
            val label = if (small) t.asDistance(unit) else s(R.string.report_tick, compact(t), unitShort(unit))
            val lw = tickP.measureText(label)
            if (nowX != null && abs(tx - nowX) < (lw + nowW) / 2f + 4f) return@forEach
            c.textTop(label, tx - lw / 2f, top + 236f, tickP)
        }
        if (nowX != null && nowLabel != null) {
            val box = RectF(nowX - nowW / 2f, top + 234f, nowX + nowW / 2f, top + 254f)
            c.drawRoundRect(box, 4f, 4f, fill(INK))
            val p = paint(11f, fonts.monoSemibold, Color.WHITE)
            c.textMiddle(nowLabel, box.centerX() - p.measureText(nowLabel) / 2f, box.centerY(), p)
        }
    }

    private fun recallsCard(c: Canvas, recalls: List<ExportRecall>, x: Float, y: Float, w: Float, height: Float?): Float {
        val shown = recalls.take(MAX_RECALLS)
        val textW = w - 32f - 30f
        val body = paint(13f, fonts.sans)
        val bold = paint(13f, fonts.sansBold)
        val lines = shown.map { r ->
            val (status, color) = when {
                r.completedOn != null -> s(R.string.report_recall_completed) to OK
                r.archivedOn != null -> s(R.string.report_recall_archived) to INK2
                else -> s(R.string.report_recall_open) to DANGER
            }
            Triple(status, color, " · " + s(R.string.report_recall_line, r.campaign, r.component))
        }
        val more = recalls.size - shown.size
        val h = 14f + 20f + 10f + (if (lines.isEmpty()) 18f else lines.size * 20f + (lines.size - 1) * 8f) + (if (more > 0) 28f else 0f) + 14f
        if (height == null) return h
        card(c, x, y, w, height)
        c.textTop(s(R.string.report_recalls_title), x + 16f, y + 14f, paint(15f, fonts.sansBold))
        var ly = y + 14f + 20f + 10f
        if (lines.isEmpty()) c.textTop(s(R.string.report_recalls_none), x + 16f, ly, paint(13f, fonts.sans, INK2))
        lines.forEach { (status, color, rest) ->
            recallIcon(c, x + 16f, ly, 20f, color, done = color == OK)
            val sx = x + 16f + 30f
            c.textMiddle(status, sx, ly + 10f, bold)
            val restX = sx + bold.measureText(status)
            c.textMiddle(TextUtils.ellipsize(rest, body, textW - bold.measureText(status), TextUtils.TruncateAt.END).toString(), restX, ly + 10f, body)
            ly += 28f
        }
        if (more > 0) c.textTop(q(R.plurals.report_recalls_more, more), x + 16f + 30f, ly, paint(13f, fonts.sans, INK2))
        return h
    }

    private fun whoCard(c: Canvas, diy: Int, shop: Int, x: Float, y: Float, w: Float, height: Float?): Float {
        val h = 14f + 20f + 10f + 40f + 14f
        if (height == null) return h
        card(c, x, y, w, height)
        c.textTop(s(R.string.report_who_title), x + 16f, y + 14f, paint(15f, fonts.sansBold))
        val total = diy + shop
        // At most ten squares; with more work than that they stand for shares, and the words give the counts.
        val squares = minOf(total, MAX_SQUARES)
        val filled = if (total == 0) 0 else (diy.toFloat() / total * squares).roundToInt()
        val top = y + 14f + 20f + 10f + 9f
        repeat(squares) { i ->
            val sx = x + 16f + i * 26f
            val r = RectF(sx + 1f, top + 1f, sx + 21f, top + 21f)
            c.drawRoundRect(r, 5f, 5f, fill(if (i < filled) INK2 else Color.WHITE))
            c.drawRoundRect(r, 5f, 5f, stroke(INK2, 2f))
        }
        val tx = x + 16f + (if (squares > 0) squares * 26f + 12f else 0f)
        val bold = paint(13f, fonts.sansBold)
        val body = paint(13f, fonts.sans)
        listOf(diy to s(R.string.report_who_diy), shop to s(R.string.report_who_shop)).forEachIndexed { i, (n, text) ->
            val ly = y + 14f + 20f + 10f + 2f + i * 19f
            val count = n.asNumber()
            c.textTop(count, tx, ly, bold)
            c.textTop(" $text", tx + bold.measureText(count), ly, body)
        }
        return h
    }

    // ---- Page 2 and on ----

    private fun logPages(m: ReportModel): List<ReportPage.Log> {
        val pages = mutableListOf<MutableList<Placed>>()
        val continued = mutableListOf<Boolean>()
        var y = 0f
        fun newPage() {
            pages.add(mutableListOf())
            continued += pages.size > 1
            y = CONTENT_TOP
        }
        fun place(block: Block, x: Float = LEFT) {
            pages.last() += Placed(x, y, block)
            y += block.height
        }
        fun fits(height: Float) = y + height <= CONTENT_BOTTOM

        newPage()
        // The average distance a month sits beside the log on the first log page.
        val side = m.monthly?.let { monthlyBlock(m) }
        side?.let { pages.last() += Placed(LEFT + LOG_WIDTH + 28f, y, it) }
        val sideBottom = side?.let { y + it.height }

        var width = if (side != null) LOG_WIDTH else CONTENT
        place(heading(s(R.string.report_every_service), width))
        if (m.log.isEmpty()) place(paragraph(s(R.string.report_no_service), paint(13f, fonts.sans, INK2), width))
        m.log.forEach { entry ->
            var block = logEntry(entry, m.vehicle.unit, width)
            if (!fits(block.height)) {
                newPage()
                width = CONTENT
                block = logEntry(entry, m.vehicle.unit, width)
            }
            place(block)
        }
        if (pages.size == 1 && sideBottom != null) y = maxOf(y, sideBottom)

        // Each further section keeps its heading with its first row, and a table repeats its header on a new page.
        fun section(title: String, header: Block?, rows: List<Block>, after: Block? = null) {
            y += SECTION_GAP
            val head = heading(title, CONTENT)
            val first = rows.firstOrNull()?.height ?: 0f
            if (!fits(head.height + (header?.height ?: 0f) + first)) newPage()
            place(head)
            header?.let { place(it) }
            rows.forEach { row ->
                if (!fits(row.height)) {
                    newPage()
                    header?.let { place(it) }
                }
                place(row)
            }
            after?.let {
                if (!fits(it.height + 6f)) newPage() else y += 6f
                place(it)
            }
        }

        if (m.schedule.isNotEmpty()) {
            section(s(R.string.report_schedule_title), scheduleHeader(), m.schedule.map { scheduleRow(it, m.vehicle.unit) })
        }
        val v = m.vehicle
        if (v.whatItTakes.isNotEmpty()) {
            section(words.phrase(ExportPhrase.WHAT_IT_TAKES), null, v.whatItTakes.map { (label, line) -> tableRow(listOf(label, line), listOf(0.3f, 0.7f)) }, small(words.phrase(ExportPhrase.TAKES_NOTE)))
        }
        if (v.documents.isNotEmpty()) {
            val fractions = listOf(0.3f, 0.45f, 0.25f)
            section(
                words.phrase(ExportPhrase.DOCUMENTS_ON_FILE),
                tableRow(listOf(words.phrase(ExportPhrase.TYPE), words.phrase(ExportPhrase.NAME), words.phrase(ExportPhrase.DATE)), fractions, header = true),
                v.documents.map { tableRow(listOf(it.type, it.name, words.paperDate(it)), fractions) },
                small(words.phrase(ExportPhrase.DOCUMENTS_NOTE)),
            )
        }

        val note = noteBox(m)
        y += SECTION_GAP
        if (!fits(note.height)) newPage()
        place(note)

        return pages.mapIndexed { i, placed -> ReportPage.Log(m, continued[i], placed) }
    }

    private fun logHeader(c: Canvas, m: ReportModel, continued: Boolean) {
        val y = TOP
        c.drawRoundRect(RectF(LEFT, y, LEFT + 24f, y + 24f), 7f, 7f, fill(ACCENT))
        wrench(c, LEFT + 5f, y + 5f, 14f)
        val vin = m.vehicle.vin?.let { "${s(R.string.report_vin)} $it" }
        val vinP = paint(12f, fonts.mono, INK2)
        val vinW = vin?.let { vinP.measureText(it) + 16f } ?: 0f
        val titleP = paint(15f, fonts.sansBold)
        val title = s(if (continued) R.string.report_log_continued else R.string.report_log_title, m.vehicle.title)
        c.textMiddle(TextUtils.ellipsize(title, titleP, CONTENT - 34f - vinW, TextUtils.TruncateAt.END).toString(), LEFT + 34f, y + 12f, titleP)
        vin?.let { c.textMiddle(it, RIGHT - vinP.measureText(it), y + 12f, vinP) }
        c.drawLine(LEFT, y + 36f, RIGHT, y + 36f, stroke(LINE, 1f))
    }

    /** One entry of the log: a record, or a visit's records under one date (#307). */
    private fun logEntry(entry: List<ExportRecord>, unit: DistanceUnit, width: Float): Block {
        val first = entry.first()
        val dateP = paint(13f, fonts.sansBold)
        val milesP = paint(12f, fonts.mono, INK2)
        val reading = Visits.readings(entry.map { it.odometer }).singleOrNull()?.asDistance(unit).orEmpty()
        val workX = 96f + 10f + 20f + 10f
        val workW = width - workX
        val parts = entry.map { r ->
            val chips = chips(r, workW)
            val meta = meta(r, entry, unit)
            val notes = r.notes?.let { layout(it, paint(12f, fonts.sans, INK2), workW) }
            val imported = if (r.imported) layout(words.phrase(ExportPhrase.IMPORTED_NOTE), paint(11f, fonts.sans, MUTED), workW) else null
            Triple(chips, meta, listOfNotNull(notes, imported))
        }
        val visit = if (entry.size > 1) layout(words.phrase(ExportPhrase.VISIT, words.records(entry.size)), paint(12f, fonts.sansSemibold, INK2), workW) else null
        val total = words.visitTotal(entry)?.let { layout(words.phrase(ExportPhrase.VISIT_TOTAL, words.money(it)), paint(12f, fonts.sansSemibold), workW) }
        val workH = (visit?.let { it.height + 6f } ?: 0f) +
            parts.sumOf { (chips, _, extra) -> (chips.height + 6f + 16f + extra.sumOf { it.height + 4.0 }).toDouble() }.toFloat() +
            (parts.size - 1) * 10f + (total?.let { it.height + 6f } ?: 0f)
        val h = 10f + maxOf(36f, workH) + 10f
        return Block(h) { x, y ->
            textTop(first.date.format(longDate), x, y + 10f, dateP)
            if (reading.isNotEmpty()) textTop(reading, x, y + 10f + 19f, milesP)
            val dot = RectF(x + 106f + 4f, y + 13f, x + 106f + 16f, y + 25f)
            val diy = entry.all { it.diy && !it.enteredAsLastDone }
            drawOval(dot, fill(if (diy) INK2 else Color.WHITE))
            drawOval(RectF(dot.left + 1f, dot.top + 1f, dot.right - 1f, dot.bottom - 1f), stroke(INK2, 2f))
            var wy = y + 10f
            visit?.let {
                withTranslation(x + workX, wy) { it.draw(this) }
                wy += it.height + 6f
            }
            parts.forEachIndexed { i, (chips, meta, extra) ->
                if (i > 0) wy += 10f
                with(chips) { draw(x + workX, wy) }
                wy += chips.height + 6f
                var mx = x + workX
                meta.forEach { part ->
                    val p = paint(12f, fonts.sans, INK2)
                    textTop(part, mx, wy, p)
                    mx += p.measureText(part) + 14f
                }
                wy += 16f
                extra.forEach { l ->
                    wy += 4f
                    withTranslation(x + workX, wy) { l.draw(this) }
                    wy += l.height
                }
            }
            total?.let {
                wy += 6f
                withTranslation(x + workX, wy) { it.draw(this) }
            }
            drawLine(x, y + h - 0.5f, x + width, y + h - 0.5f, stroke(LINE_SOFT, 1f))
        }
    }

    /** A record's lines as tags, each with its group's colour square, wrapped to [width]. */
    private fun chips(r: ExportRecord, width: Float): Block {
        val p = paint(13f, fonts.sans)
        val chipH = 23f
        val placed = mutableListOf<Triple<Float, Float, Pair<String, ServiceGroup>>>()
        var cx = 0f
        var cy = 0f
        r.items.forEachIndexed { i, item ->
            val category = r.categories.getOrNull(i) ?: ServiceGroup.OTHER
            val text = TextUtils.ellipsize(item, p, width - 30f, TextUtils.TruncateAt.END).toString()
            val w = 8f + 8f + 6f + p.measureText(text) + 8f
            if (cx > 0f && cx + w > width) {
                cx = 0f
                cy += chipH + 6f
            }
            placed += Triple(cx, cy, text to category)
            cx += w + 6f
        }
        val h = if (placed.isEmpty()) 0f else cy + chipH
        return Block(h) { x, y ->
            placed.forEach { (px, py, item) ->
                val (text, category) = item
                val w = 8f + 8f + 6f + p.measureText(text) + 8f
                drawRoundRect(RectF(x + px, y + py, x + px + w, y + py + chipH), 6f, 6f, fill(GROUND))
                groupSquare(RectF(x + px + 8f, y + py + 7.5f, x + px + 16f, y + py + 15.5f), category)
                textMiddle(text, x + px + 22f, y + py + chipH / 2f, p)
            }
        }
    }

    /** Who did the work, its cost and its photos; in a visit, the record's own date and reading where they differ. */
    private fun meta(r: ExportRecord, entry: List<ExportRecord>, unit: DistanceUnit): List<String> = listOfNotNull(
        when {
            r.enteredAsLastDone -> s(R.string.report_last_done_only)
            r.diy -> s(R.string.report_who_owner)
            else -> r.doneBy
        },
        if (entry.size > 1) words.visitOwn(r, entry, unit) else null,
        r.costCents?.let { words.money(it) },
        photoCount(r.photoFiles.size),
    )

    private fun monthlyBlock(m: ReportModel): Block {
        val mo = m.monthly!!
        val unit = m.vehicle.unit
        val w = 260f
        val h = 24f + 10f + 37f + 2f + 17f + 10f + 190f
        return Block(h) { x, y ->
            textTop(s(if (unit == DistanceUnit.MILES) R.string.report_per_month_mi else R.string.report_per_month_km), x, y, paint(18f, fonts.sansBold))
            textTop(s(R.string.report_about, mo.perMonth.asNumber()), x, y + 34f, paint(34f, fonts.monoSemibold))
            textTop(s(R.string.report_average, mo.from.format(monthYear), mo.to.format(monthYear)), x, y + 34f + 39f, paint(13f, fonts.sans, INK2))
            val top = y + h - 190f
            val days = ChronoUnit.DAYS.between(mo.from, mo.to).coerceAtLeast(1)
            fun px(d: LocalDate) = x + 44f + ChronoUnit.DAYS.between(mo.from, d).toFloat() / days * 202f
            fun py(v: Int) = top + 160f - (v - mo.low).toFloat() / (mo.high - mo.low).coerceAtLeast(1) * 146f
            val small = mo.high - mo.low < SMALL_AXIS
            val tickP = paint(11f, fonts.mono, MUTED)
            mo.ticks.forEach { t ->
                val ty = py(t)
                drawLine(x + 34f, ty, x + 256f, ty, stroke(LINE_SOFT, 1f))
                textMiddle(if (small) t.asNumber() else s(R.string.report_thousands, compact(t)), x, ty, tickP)
            }
            val path = Path()
            mo.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(px(p.date), py(p.odometer)) else path.lineTo(px(p.date), py(p.odometer)) }
            drawPath(path, stroke(INK2, 2f).apply { strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND })
            if (mo.points.size <= MAX_POINTS) {
                mo.points.forEach { p ->
                    drawCircle(px(p.date), py(p.odometer), 6f, fill(Color.WHITE))
                    drawCircle(px(p.date), py(p.odometer), 4f, fill(INK2))
                }
            }
            drawLine(x + 34f, top + 160f, x + 256f, top + 160f, stroke(AXIS, 1f))
            val monthP = paint(11f, fonts.sans, MUTED)
            listOf(mo.from, mo.from.plusDays(days / 2), mo.to).distinctBy { it.withDayOfMonth(1) }.forEach { d ->
                val label = d.format(monthYear)
                textTop(label, (px(d) - monthP.measureText(label) / 2f).coerceAtMost(x + w - monthP.measureText(label)), top + 166f, monthP)
            }
            val last = mo.points.last()
            val peak = last.odometer.asNumber()
            val peakP = paint(11f, fonts.monoSemibold)
            textTop(peak, minOf(px(last.date) - peakP.measureText(peak) / 2f, x + w - peakP.measureText(peak)), py(last.odometer) - 22f, peakP)
        }
    }

    private fun scheduleHeader(): Block {
        val p = paint(13f, fonts.sansSemibold, INK2)
        return Block(30f) { x, y ->
            textTop(words.phrase(ExportPhrase.ITEM), x, y + 6f, p)
            textTop(s(R.string.report_interval), x + INTERVAL_X, y + 6f, p)
            val last = words.phrase(ExportPhrase.LAST_DONE)
            textTop(last, x + LAST_RIGHT - p.measureText(last), y + 6f, p)
            val next = s(R.string.report_next_due)
            textTop(next, x + CONTENT - p.measureText(next), y + 6f, p)
            drawLine(x, y + 29.25f, x + CONTENT, y + 29.25f, stroke(INK, 1.5f))
        }
    }

    private fun scheduleRow(row: io.cosgriff.glovebox.domain.ReportScheduleRow, unit: DistanceUnit): Block {
        val p = paint(13f, fonts.sans)
        val name = layout(row.name, p, INTERVAL_X - 16f - 12f)
        val every = layout(row.every, paint(13f, fonts.sans, INK2), LAST_RIGHT - INTERVAL_X - 130f)
        val mono = paint(13f, fonts.mono)
        val last = row.lastDone?.odometer?.asDistance(unit) ?: s(R.string.report_none)
        val next = row.nextDue?.asDistance(unit) ?: row.nextBy?.format(longDate) ?: s(R.string.report_none)
        val h = 7f + maxOf(name.height, every.height).toFloat() + 7f
        return Block(h) { x, y ->
            groupSquare(RectF(x, y + 7f + 5f, x + 8f, y + 7f + 13f), row.category)
            withTranslation(x + 16f, y + 7f) { name.draw(this) }
            withTranslation(x + INTERVAL_X, y + 7f) { every.draw(this) }
            textTop(last, x + LAST_RIGHT - mono.measureText(last), y + 7f, mono)
            textTop(next, x + CONTENT - mono.measureText(next), y + 7f, mono)
            drawLine(x, y + h - 0.5f, x + CONTENT, y + h - 0.5f, stroke(LINE, 1f))
        }
    }

    private fun tableRow(cells: List<String>, fractions: List<Float>, header: Boolean = false): Block {
        val p = if (header) paint(13f, fonts.sansSemibold, INK2) else paint(13f, fonts.sans)
        val layouts = cells.mapIndexed { i, cell -> layout(cell, p, fractions[i] * CONTENT - 12f) }
        val h = 7f + layouts.maxOf { it.height } + 7f
        return Block(h) { x, y ->
            var cx = x
            layouts.forEachIndexed { i, l ->
                withTranslation(cx, y + 7f) { l.draw(this) }
                cx += fractions[i] * CONTENT
            }
            drawLine(x, y + h - 0.5f, x + CONTENT, y + h - 0.5f, if (header) stroke(INK, 1.5f) else stroke(LINE, 1f))
        }
    }

    private fun noteBox(m: ReportModel): Block {
        val text = listOfNotNull(
            s(R.string.report_note),
            words.phrase(ExportPhrase.SCHEDULE_NOTE).takeIf { m.schedule.isNotEmpty() },
            words.phrase(ExportPhrase.RECALL_NOTE).takeIf { m.recalls.isNotEmpty() },
            m.vehicle.clusterSwap?.let { words.clusterNote(it, m.vehicle.unit) },
        ).joinToString(" ")
        val l = layout(text, paint(12f, fonts.sans, INK2), CONTENT - 28f - 30f, spacing = 1.3f)
        val h = 12f + l.height + 12f
        return Block(h) { x, y ->
            drawRoundRect(RectF(x, y, x + CONTENT, y + h), 12f, 12f, fill(GROUND))
            lock(this, x + 14f, y + 13f, 18f)
            withTranslation(x + 14f + 30f, y + 12f) { l.draw(this) }
        }
    }

    private fun heading(text: String, width: Float): Block {
        val l = layout(text, paint(18f, fonts.sansBold), width)
        return Block(l.height + 10f) { x, y -> withTranslation(x, y) { l.draw(this) } }
    }

    private fun paragraph(text: String, p: TextPaint, width: Float): Block {
        val l = layout(text, p, width)
        return Block(l.height + 10f) { x, y -> withTranslation(x, y + 10f) { l.draw(this) } }
    }

    private fun small(text: String): Block = paragraph(text, paint(12f, fonts.sans, MUTED), CONTENT).let { b -> Block(b.height - 10f) { x, y -> with(b) { draw(x, y - 10f) } } }

    // ---- Photo pages ----

    private fun photoPage(c: Canvas, page: ReportPage.Photo, photo: Bitmap?) {
        val r = page.record
        val unit = page.model.vehicle.unit
        logHeader(c, page.model, continued = false)
        val caption = s(R.string.report_caption, r.date.format(longDate), r.odometer.asDistance(unit), r.items.firstOrNull() ?: r.doneBy)
        val captionP = paint(13f, fonts.sans, INK2)
        val area = RectF(PHOTO_MARGIN, CONTENT_TOP, WIDTH - PHOTO_MARGIN, CONTENT_BOTTOM - 40f)
        if (photo == null) {
            c.drawRoundRect(area, 12f, 12f, fill(GROUND))
            val missing = words.phrase(ExportPhrase.PHOTO_UNREADABLE)
            c.textMiddle(missing, area.centerX() - captionP.measureText(missing) / 2f, area.centerY(), captionP)
        } else {
            val scale = minOf(area.width() / photo.width, area.height() / photo.height)
            val w = photo.width * scale
            val h = photo.height * scale
            val dst = RectF(area.centerX() - w / 2f, area.top, area.centerX() + w / 2f, area.top + h)
            c.drawBitmap(photo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            area.bottom = dst.bottom
        }
        val shown = TextUtils.ellipsize(caption, captionP, area.width(), TextUtils.TruncateAt.END).toString()
        c.textTop(shown, area.centerX() - captionP.measureText(shown) / 2f, area.bottom + 14f, captionP)
    }

    // ---- Shared ----

    private fun footerFirst(m: ReportModel, logPage: Int): String =
        s(if (m.vehicle.records.any { it.photoFiles.isNotEmpty() }) R.string.report_footer_first_photos else R.string.report_footer_first, logPage.asNumber())

    private fun footer(c: Canvas, left: String, number: Int, total: Int) {
        val y = BOTTOM - 27f
        c.drawLine(LEFT, y, RIGHT, y, stroke(LINE, 1f))
        val p = paint(12f, fonts.sans, MUTED)
        val page = s(R.string.report_page, number.asNumber(), total.asNumber())
        val pageW = p.measureText(page)
        c.textTop(TextUtils.ellipsize(left, p, CONTENT - pageW - 16f, TextUtils.TruncateAt.END).toString(), LEFT, y + 10f, p)
        c.textTop(page, RIGHT - pageW, y + 10f, p)
    }

    private fun sectionHeading(c: Canvas, text: String, top: Float) = c.textTop(text, LEFT, top, paint(18f, fonts.sansBold))

    private fun card(c: Canvas, x: Float, y: Float, w: Float, h: Float) =
        c.drawRoundRect(RectF(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f), 14f, 14f, stroke(LINE, 1f))

    /** The app's wrench, from the design's icon, in white on the accent square. */
    private fun wrench(c: Canvas, x: Float, y: Float, size: Float) = icon(c, WRENCH, x, y, size, Color.WHITE, 2.4f)

    private fun lock(c: Canvas, x: Float, y: Float, size: Float) {
        icon(c, "M7 11V7a5 5 0 0 1 10 0v4", x, y, size, INK2, 2f)
        c.withTranslation(x, y) {
            scale(size / 24f, size / 24f)
            drawRoundRect(RectF(3f, 11f, 21f, 22f), 2f, 2f, stroke(INK2, 2f))
        }
    }

    private fun statusIcon(c: Canvas, x: Float, y: Float, size: Float, status: ReportStatus?) {
        c.withTranslation(x, y) {
            scale(size / 24f, size / 24f)
            drawCircle(12f, 12f, 10f, stroke(INK, 2.4f))
        }
        if (status == ReportStatus.Current) icon(c, "M8 12.5l2.5 2.5L16 9.5", x, y, size, INK, 2.4f) else icon(c, "M12 7v6M12 17h.01", x, y, size, INK, 2.4f)
    }

    private fun recallIcon(c: Canvas, x: Float, y: Float, size: Float, color: Int, done: Boolean) {
        c.withTranslation(x, y) {
            scale(size / 24f, size / 24f)
            drawCircle(12f, 12f, 10f, stroke(color, 2.4f))
        }
        icon(c, if (done) "M8 12.5l2.5 2.5L16 9.5" else "M12 7v6M12 17h.01", x, y, size, color, 2.4f)
    }

    /** A 24-unit outline icon from its SVG path, drawn [size] wide at [x], [y]. */
    private fun icon(c: Canvas, pathData: String, x: Float, y: Float, size: Float, color: Int, width: Float) {
        val path = PathParser.createPathFromPathData(pathData)
        c.withSave {
            translate(x, y)
            scale(size / 24f, size / 24f)
            drawPath(path, stroke(color, width).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND })
        }
    }

    private fun Canvas.textTop(text: String, x: Float, top: Float, p: TextPaint) = drawText(text, x, top - p.fontMetrics.ascent, p)

    private fun Canvas.textMiddle(text: String, x: Float, middle: Float, p: TextPaint) {
        val m = p.fontMetrics
        drawText(text, x, middle - (m.ascent + m.descent) / 2f, p)
    }

    /** The largest size from [max] down to [min] at which [text] fits in [width]. */
    private fun fitted(text: String, max: Float, min: Float, typeface: Typeface, width: Float): TextPaint {
        val p = paint(max, typeface)
        while (p.textSize > min && p.measureText(text) > width) p.textSize -= 1f
        return p
    }

    private fun layout(text: String, p: TextPaint, width: Float, spacing: Float = 1f): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, spacing)
            .setIncludePad(false)
            .build()

    private fun paint(size: Float, typeface: Typeface, color: Int = INK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.typeface = typeface
        this.color = color
    }

    /** A group's square: filled, or outlined for [ServiceGroup.OTHER], whose grey alone could pass for a hue (#410). */
    private fun Canvas.groupSquare(rect: RectF, group: ServiceGroup) {
        if (group == ServiceGroup.OTHER) {
            drawRoundRect(RectF(rect.left + 0.75f, rect.top + 0.75f, rect.right - 0.75f, rect.bottom - 0.75f), 2f, 2f, stroke(categoryColor(group), 1.5f))
        } else {
            drawRoundRect(rect, 2f, 2f, fill(categoryColor(group)))
        }
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    /** "68.4" for 68,412: thousands, to one decimal place where it isn't whole. */
    private fun compact(value: Int): String = NumberFormat.getNumberInstance(AppText.locale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }.format(value / 1000.0)

    private fun thousands(value: Int): String = s(R.string.report_thousands, compact(value))

    private fun unitShort(unit: DistanceUnit): String = s(if (unit == DistanceUnit.MILES) R.string.unit_mi else R.string.unit_km)

    private fun s(@StringRes id: Int, vararg args: Any): String = AppText.res.getString(id, *args)

    private fun q(id: Int, count: Int): String = AppText.res.getQuantityString(id, count, count.asNumber())

    companion object {
        /** The design's page, in its pixels: US Letter at 96 dpi. */
        const val WIDTH = 816f
        const val HEIGHT = 1056f

        /** US Letter in PDF points (1/72 inch). */
        const val PDF_WIDTH = 612
        const val PDF_HEIGHT = 792

        private const val LEFT = 48f
        private const val RIGHT = WIDTH - 48f
        private const val CONTENT = RIGHT - LEFT
        private const val TOP = 48f
        private const val BOTTOM = HEIGHT - 40f
        private const val CONTENT_TOP = TOP + 36f + 26f
        private const val CONTENT_BOTTOM = BOTTOM - 27f - 26f
        private const val LOG_WIDTH = CONTENT - 28f - 260f
        private const val SECTION_GAP = 26f
        private const val CHART_HEIGHT = 262f
        private const val INTERVAL_X = 287f
        private const val LAST_RIGHT = 580f

        /** Three-quarters of an inch. */
        private const val PHOTO_MARGIN = 72f
        private const val MAX_RECALLS = 4
        private const val MAX_SQUARES = 10
        private const val MAX_POINTS = 24

        /** Below this span an axis is labelled in whole figures, not thousands. */
        private const val SMALL_AXIS = 4_000

        private const val WRENCH = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.8-3.8a6 6 0 0 1-7.9 7.9l-6.9 6.9a2.1 2.1 0 0 1-3-3l6.9-6.9a6 6 0 0 1 7.9-7.9z"

        // Print colours: the light theme's, on white paper whatever theme the app is in.
        private const val INK = 0xFF17191C.toInt()
        private const val INK2 = 0xFF3A3E44.toInt()
        private const val MUTED = 0xFF4F545B.toInt()
        private const val LINE = 0xFFDEDCD5.toInt()
        private const val LINE_SOFT = 0xFFEEECE6.toInt()
        private const val AXIS = 0xFFB9B6AE.toInt()
        private const val GROUND = 0xFFF4F3EF.toInt()
        private const val ACCENT = 0xFFC2410C.toInt()
        private const val OK = 0xFF0F766E.toInt()
        private const val DANGER = 0xFFB42318.toInt()

        /** The app's light colour for the group: the report is always on white paper. Each is drawn beside its name. */
        fun categoryColor(category: ServiceGroup): Int = groupColor(category, dark = false).toArgb()

        fun categoryName(category: ServiceGroup): String = AppText.res.getString(
            when (category) {
                ServiceGroup.OIL_ENGINE -> R.string.report_category_oil
                ServiceGroup.TIRES -> R.string.report_category_tires
                ServiceGroup.BRAKES -> R.string.report_category_brakes
                ServiceGroup.FLUIDS_4WD -> R.string.report_category_fluids
                ServiceGroup.SUSPENSION -> R.string.report_category_suspension
                ServiceGroup.FUEL_EXHAUST -> R.string.report_category_fuel
                ServiceGroup.OTHER -> R.string.report_category_other
            },
        )

        private fun dueState(state: DueNextState): Int = when (state) {
            DueNextState.OVERDUE -> R.string.report_due_overdue
            DueNextState.DUE_SOON -> R.string.report_due_soon
            DueNextState.SNOOZED -> R.string.report_due_snoozed
            DueNextState.OK -> R.string.report_due_ok
            DueNextState.JUST_DONE -> R.string.report_due_just_done
        }
    }
}

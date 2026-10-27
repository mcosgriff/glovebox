package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * One word of a shop's invoice as it was read (#483): from Tesseract, with its [confidence] (0–100), or from a
 * PDF's text layer, which is exact and has none. The box is in pixels or points, from the page's top left; only
 * its proportions matter, since every distance the reader uses is measured in the page's own line height.
 */
data class InvoiceWord(
    val text: String,
    val page: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float? = null,
)

/** One line of the invoice as the reader put it together: its [segments] are the pieces a wide gap separates. */
data class InvoiceLine(val page: Int, val segments: List<String>) {
    val text: String get() = segments.joinToString("  ")
}

/** What a printed figure is, by the invoice's own label or column. */
enum class InvoiceFigure {
    // One service's.
    SUBTOTAL, LABOR, PARTS, FEES, SUBLET, PART_EACH, PART_TOTAL,

    // The totals block's.
    TOTAL_LABOR, TOTAL_PARTS, TOTAL_FEES, TOTAL_SUBLETS, SUPPLIES, TAX, OTHER_CHARGE, DISCOUNT, GRAND_TOTAL,

    // Read but never used: a balance is what is left to pay, not what the work cost.
    BALANCE_DUE, PAID,
}

/** A part of the invoice the reader recognised by its heading or its shape. */
enum class InvoiceSection {
    /** The services: the only place a job comes from. */
    SERVICES,

    /** A table of work with labor and parts columns, read as the services. */
    LINE_TABLE,

    /** The grand total and what it is made of: the services end here. */
    TOTALS,

    /** Recommended, declined, deferred or suggested work: kept as text for notes, never a job. */
    RECOMMENDED,

    /** The shop's terms, an authorization or a signature: nothing in it is read. */
    TERMS,
}

/** One of the sums an invoice prints, by which the reader vouches for its figures. */
enum class InvoiceCheck {
    /** A parts row: quantity × each = its total. */
    PART_ROW,

    /** A service's parts rows add up to its parts figure. */
    JOB_PARTS,

    /** A service's labor, parts, fees and sublet add up to its subtotal (or a table row's total). */
    JOB_SUBTOTAL,

    /** Every service's labor (parts, fees, sublet) adds up to the totals block's. */
    ALL_LABOR, ALL_PARTS, ALL_FEES, ALL_SUBLETS,

    /** The totals block adds up to the grand total. */
    TOTALS_BLOCK,

    /** The services' subtotals, with shop supplies, other charges and tax, add up to the grand total. */
    SUBTOTALS,
}

/** Why a figure that was read was not taken. */
enum class FigureLeftOut {
    /** No sum it is in agrees, or it is in none: it may be a misread, so it is not offered. */
    NO_SUM_AGREES,

    /** The same figure was read twice, differently. */
    READ_TWICE,
}

enum class OdometerRole { IN, OUT, UNLABELLED }

/** Why an odometer figure that was read was not taken. */
sealed interface OdometerLeftOut {
    /** Outside the stretch a reading can be in: from the last reading on or before the invoice's date to [DashReading.FAR_AHEAD] past it. */
    data class OutOfRange(val from: Int, val to: Int) : OdometerLeftOut

    /** "Out" below "in": one of them is misread, and nothing says which. */
    data object OutBelowIn : OdometerLeftOut

    /** "Out" far past "in" (more than [InvoiceReader.SHOP_DRIVE]): one of them is misread. */
    data object OutFarPastIn : OdometerLeftOut

    data object ReadTwice : OdometerLeftOut
}

/** A letter the VIN reader took for the digit it looks like, or the other way round, at [position] (1–17). */
data class VinFold(val position: Int, val read: Char, val taken: Char)

/**
 * One decision the reader made, in order: the debug trace (#484 words it). Lines are indexes into
 * [InvoiceReading.lines]; jobs into [InvoiceReading.jobs].
 */
sealed interface InvoiceStep {
    /** A line every page repeats at its top or bottom (the shop's header), skipped past the first page. */
    data class RepeatedLineSkipped(val line: Int) : InvoiceStep

    data class SectionStarted(val line: Int, val section: InvoiceSection) : InvoiceStep

    data class JobStarted(val line: Int, val job: Int, val number: Int?, val wording: String) : InvoiceStep

    data class FigureRead(val line: Int, val figure: InvoiceFigure, val cents: Long, val job: Int?) : InvoiceStep

    data class PartRow(val line: Int, val job: Int, val quantity: String, val eachCents: Long, val totalCents: Long) : InvoiceStep

    /** A sum the invoice prints: what its parts add up to ([sumCents]) against the figure printed for it. */
    data class SumChecked(val check: InvoiceCheck, val job: Int?, val sumCents: Long, val printedCents: Long, val agrees: Boolean, val lines: List<Int>) : InvoiceStep

    data class FigureTaken(val figure: InvoiceFigure, val job: Int?, val cents: Long) : InvoiceStep

    data class FigureNotTaken(val figure: InvoiceFigure, val job: Int?, val cents: Long, val why: FigureLeftOut) : InvoiceStep

    data class VinTaken(val line: Int, val vin: String, val folds: List<VinFold>) : InvoiceStep

    /** Something labelled VIN that doesn't pass the check digit, even with look-alike letters folded. */
    data class VinNotTaken(val line: Int, val text: String) : InvoiceStep

    data class OdometerTaken(val line: Int, val role: OdometerRole, val value: Int) : InvoiceStep

    data class OdometerNotTaken(val line: Int, val role: OdometerRole, val value: Int, val why: OdometerLeftOut) : InvoiceStep

    data class DateTaken(val line: Int, val date: LocalDate) : InvoiceStep

    /** A date after today: misread, so not offered. */
    data class DateNotTaken(val line: Int, val date: LocalDate) : InvoiceStep

    data class ShopTaken(val line: Int, val name: String) : InvoiceStep

    data class RecommendationKept(val line: Int, val text: String, val cents: Long?) : InvoiceStep

    /** The user's rule files the job under [catalogId]. */
    data class JobFiled(val job: Int, val catalogId: String) : InvoiceStep

    /** [LineGuess]'s guesses for the job, offered and never picked. */
    data class JobGuessed(val job: Int, val catalogIds: List<String>) : InvoiceStep

    /** No services were found: no heading, no table, no subtotal. */
    data object NoServices : InvoiceStep
}

/**
 * One service on the invoice: one job of a record (#481), its wording as the shop wrote it, and only the figures
 * a sum vouched for (null: not printed, or not vouched). [filed] is the user's rule's line; [guesses] are
 * [LineGuess]'s, offered and never picked.
 */
data class InvoiceJob(
    val line: Int,
    val number: Int?,
    val wording: String,
    val laborWording: String?,
    val description: String?,
    val laborCents: Long?,
    val partsCents: Long?,
    val feesCents: Long?,
    val subletCents: Long?,
    val subtotalCents: Long?,
    val filed: String?,
    val guesses: List<String>,
) {
    /** The job's price as a record keeps it: labor and parts only; fees and sublet go to the gap. */
    val price: JobPrice get() = JobPrice(laborCents, partsCents)
}

/** Work the shop recommended, as text offered for the record's notes: never a job, since it wasn't done. */
data class InvoiceRecommendation(val line: Int, val text: String, val cents: Long?)

/**
 * What the reader made of an invoice. It only ever fills the form: every field is an offer, null where nothing was
 * read or a sum didn't vouch for it.
 */
data class InvoiceReading(
    val date: LocalDate?,
    val shop: String?,
    val vin: String?,
    val odometerIn: Int?,
    val odometerOut: Int?,
    val grandTotalCents: Long?,
    val jobs: List<InvoiceJob>,
    val recommendations: List<InvoiceRecommendation>,
    val lines: List<InvoiceLine>,
    val trace: List<InvoiceStep>,
) {
    /** The record's reading: out, or in when out wasn't read. */
    val odometer: Int? get() = odometerOut ?: odometerIn

    /** "In", for the notes, only when the record took "out" and the two differ. */
    val odometerInForNotes: Int? get() = odometerIn?.takeIf { odometerOut != null && it != odometerOut }
}

/**
 * An invoice's words as JSON, so a reading the parser gets wrong can be saved from the debug trace (#484) and kept
 * as a local test (`domain/src/test/private-invoices/`, git-ignored) without the picture.
 */
object InvoiceWords {
    const val VERSION = 1

    fun toJson(words: List<InvoiceWord>): String = buildJsonObject {
        put("version", VERSION)
        put(
            "words",
            buildJsonArray {
                words.forEach { w ->
                    add(
                        buildJsonObject {
                            put("text", w.text)
                            put("page", w.page)
                            put("left", w.left)
                            put("top", w.top)
                            put("right", w.right)
                            put("bottom", w.bottom)
                            w.confidence?.let { put("confidence", it) }
                        },
                    )
                }
            },
        )
    }.toString()

    /** The words in [json]; throws on anything that isn't a version-1 word list. */
    fun fromJson(json: String): List<InvoiceWord> {
        val root = Json.parseToJsonElement(json).jsonObject
        require(root["version"]?.jsonPrimitive?.int == VERSION) { "not a version $VERSION word list" }
        return (root["words"] as JsonArray).map { it.jsonObject.word() }
    }

    private fun JsonObject.word() = InvoiceWord(
        text = getValue("text").jsonPrimitive.content,
        page = getValue("page").jsonPrimitive.int,
        left = getValue("left").jsonPrimitive.float,
        top = getValue("top").jsonPrimitive.float,
        right = getValue("right").jsonPrimitive.float,
        bottom = getValue("bottom").jsonPrimitive.float,
        confidence = (get("confidence") as? JsonPrimitive)?.floatOrNull,
    )
}

package io.cosgriff.glovebox.domain

import java.time.LocalDate

/** A figure the form outlines as "Couldn't read": the invoice printed one, and no sum vouched for it. */
enum class FilledFigure { ODOMETER, TOTAL }

/**
 * One service as Log service takes it (#484): a line of its own, named by the shop's [wording], under the line the
 * user's rule [filed] it under, or a custom line otherwise. [guesses] are only offered. A price is null where the
 * invoice gave none or no sum vouched for it; [laborUnread] / [partsUnread] say it was printed and not taken.
 */
data class FilledJob(
    val wording: String,
    val filed: String?,
    val guesses: List<String>,
    val laborCents: Long?,
    val partsCents: Long?,
    val laborUnread: Boolean,
    val partsUnread: Boolean,
)

/** Work the shop recommended, for the notes. */
data class InvoiceRecommendationNote(val text: String, val cents: Long?)

/** The invoice's VIN isn't the vehicle's: asked, never a block. Each is the VIN's last four characters. */
data class VinMismatch(val invoiceEnd: String, val vehicleEnd: String)

/**
 * What Read an invoice puts on Log service. Every field is an offer for the user to check against the invoice;
 * nothing is saved until they save the form.
 */
data class InvoiceFill(
    val date: LocalDate?,
    val odometer: Int?,
    val shop: String?,
    val totalCents: Long?,
    val jobs: List<FilledJob>,
    val recommendations: List<InvoiceRecommendationNote>,
    val odometerIn: Int?,
    val vinMismatch: VinMismatch?,
    val unread: Set<FilledFigure>,
    /** Services worded as an earlier one, which the form can't hold as a second line: the user adds them. */
    val leftOut: List<String>,
)

/** What one line of the invoice became, for "What was read". A line can be more than one; none is ignored. */
sealed interface LineRole {
    /** The header every page repeats, past the first page. */
    data object Repeated : LineRole

    data class Heading(val section: InvoiceSection) : LineRole

    /** The line starting [job], which became a line of the record. */
    data class Work(val job: Int) : LineRole

    /** [taken] null: a figure only used to check the others (the totals block's). */
    data class Figure(val figure: InvoiceFigure, val cents: Long, val job: Int?, val taken: Boolean?) : LineRole

    /** Recommended, declined or deferred work, offered for the notes. */
    data object Recommended : LineRole

    data object Shop : LineRole

    data object Date : LineRole

    data class Vin(val taken: Boolean) : LineRole

    data class Odometer(val role: OdometerRole, val value: Int, val taken: Boolean) : LineRole
}

object InvoiceFills {
    private const val VIN_END = 4

    fun of(reading: InvoiceReading, vehicleVin: String?): InvoiceFill {
        val notTaken = reading.trace.filterIsInstance<InvoiceStep.FigureNotTaken>()
        fun unread(figure: InvoiceFigure, job: Int?) = notTaken.any { it.figure == figure && it.job == job }

        val seen = mutableSetOf<String>()
        val leftOut = mutableListOf<String>()
        val jobs = reading.jobs.mapIndexedNotNull { i, j ->
            // Nothing to name a line by.
            if (j.wording.isBlank()) return@mapIndexedNotNull null
            if (!seen.add(j.wording.trim().lowercase())) {
                leftOut += j.wording
                return@mapIndexedNotNull null
            }
            FilledJob(
                wording = j.wording,
                filed = j.filed,
                guesses = j.guesses,
                laborCents = j.laborCents,
                partsCents = j.partsCents,
                laborUnread = j.laborCents == null && unread(InvoiceFigure.LABOR, i),
                partsUnread = j.partsCents == null && unread(InvoiceFigure.PARTS, i),
            )
        }
        val odometerUnread = reading.odometer == null && reading.trace.any { it is InvoiceStep.OdometerNotTaken }
        val totalUnread = reading.grandTotalCents == null && unread(InvoiceFigure.GRAND_TOTAL, null)
        val mine = vehicleVin?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val theirs = reading.vin?.uppercase()
        return InvoiceFill(
            date = reading.date,
            odometer = reading.odometer,
            shop = reading.shop,
            totalCents = reading.grandTotalCents,
            jobs = jobs,
            recommendations = reading.recommendations.map { InvoiceRecommendationNote(it.text, it.cents) },
            odometerIn = reading.odometerInForNotes,
            vinMismatch = if (mine != null && theirs != null && mine != theirs) VinMismatch(theirs.takeLast(VIN_END), mine.takeLast(VIN_END)) else null,
            unread = setOfNotNull(FilledFigure.ODOMETER.takeIf { odometerUnread }, FilledFigure.TOTAL.takeIf { totalUnread }),
            leftOut = leftOut,
        )
    }

    /** One list for each of [InvoiceReading.lines], in order: what that line became. */
    fun lineRoles(reading: InvoiceReading): List<List<LineRole>> {
        val taken = reading.trace.filterIsInstance<InvoiceStep.FigureTaken>().map { Triple(it.figure, it.job, it.cents) }.toSet()
        val notTaken = reading.trace.filterIsInstance<InvoiceStep.FigureNotTaken>().map { Triple(it.figure, it.job, it.cents) }.toSet()
        val roles = List(reading.lines.size) { mutableListOf<LineRole>() }
        fun add(line: Int, role: LineRole) {
            if (line in roles.indices && role !in roles[line]) roles[line] += role
        }
        reading.trace.forEach { step ->
            when (step) {
                is InvoiceStep.RepeatedLineSkipped -> add(step.line, LineRole.Repeated)
                is InvoiceStep.SectionStarted -> add(step.line, LineRole.Heading(step.section))
                is InvoiceStep.JobStarted -> add(step.line, LineRole.Work(step.job))
                is InvoiceStep.FigureRead -> {
                    val key = Triple(step.figure, step.job, step.cents)
                    add(step.line, LineRole.Figure(step.figure, step.cents, step.job, if (key in taken) true else if (key in notTaken) false else null))
                }
                is InvoiceStep.RecommendationKept -> add(step.line, LineRole.Recommended)
                is InvoiceStep.ShopTaken -> add(step.line, LineRole.Shop)
                is InvoiceStep.DateTaken -> add(step.line, LineRole.Date)
                is InvoiceStep.VinTaken -> add(step.line, LineRole.Vin(taken = true))
                is InvoiceStep.VinNotTaken -> add(step.line, LineRole.Vin(taken = false))
                is InvoiceStep.OdometerTaken -> add(step.line, LineRole.Odometer(step.role, step.value, taken = true))
                is InvoiceStep.OdometerNotTaken -> add(step.line, LineRole.Odometer(step.role, step.value, taken = false))
                else -> Unit
            }
        }
        return roles
    }
}

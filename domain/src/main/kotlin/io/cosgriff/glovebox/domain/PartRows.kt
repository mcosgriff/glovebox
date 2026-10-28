package io.cosgriff.glovebox.domain

/** One of a part row's three figures (#502). */
enum class PartField { QUANTITY, EACH, TOTAL }

/**
 * A part row's figures as entered (#502): the quantity in thousandths (oil goes by the quart), money in cents.
 * Null is "not entered", which is not zero.
 */
data class PartAmounts(val quantityMilli: Long? = null, val eachCents: Long? = null, val totalCents: Long? = null)

/** What a part row's figures say together. */
sealed interface PartCheck {
    /** [field] comes out exactly from the other two: [value], in thousandths for the quantity, else in cents. */
    data class Fills(val field: PartField, val value: Long) : PartCheck

    /** [field] doesn't come out exactly from the other two, so it stays blank: nothing is rounded for the user. */
    data class LeftBlank(val field: PartField) : PartCheck

    /**
     * All three entered, and the quantity times each isn't the total. [workedCents] is what they make, to the
     * nearest cent, only for saying so: the total as entered is the one that counts.
     */
    data class Mismatch(val workedCents: Long) : PartCheck

    /** All three entered, and they agree. */
    data object Agrees : PartCheck

    /** Fewer than two entered: nothing to work out. */
    data object Incomplete : PartCheck
}

object PartRows {
    /** What [a]'s figures say together; see [PartCheck]. */
    fun check(a: PartAmounts): PartCheck {
        val q = a.quantityMilli
        val each = a.eachCents
        val total = a.totalCents
        return try {
            when {
                q != null && each != null && total != null -> {
                    val made = Math.multiplyExact(q, each)
                    if (made == Math.multiplyExact(total, 1000L)) PartCheck.Agrees else PartCheck.Mismatch((made + 500) / 1000)
                }
                q != null && each != null -> {
                    val made = Math.multiplyExact(q, each)
                    if (made % 1000 == 0L) PartCheck.Fills(PartField.TOTAL, made / 1000) else PartCheck.LeftBlank(PartField.TOTAL)
                }
                q != null && total != null -> {
                    val milli = Math.multiplyExact(total, 1000L)
                    if (q > 0 && milli % q == 0L) PartCheck.Fills(PartField.EACH, milli / q) else PartCheck.LeftBlank(PartField.EACH)
                }
                each != null && total != null -> {
                    val milli = Math.multiplyExact(total, 1000L)
                    if (each > 0 && milli % each == 0L) PartCheck.Fills(PartField.QUANTITY, milli / each) else PartCheck.LeftBlank(PartField.QUANTITY)
                }
                else -> PartCheck.Incomplete
            }
        } catch (_: ArithmeticException) {
            // Figures too big to multiply out can't be checked, so nothing is worked out from them.
            if (q != null && each != null && total != null) PartCheck.Incomplete else PartCheck.LeftBlank(missing(a))
        }
    }

    /**
     * A job's parts figure from its rows: the sum of their totals, worked out on every read and never stored. A
     * row without a total is left out ([missingTotals] counts them); null when no row has one.
     */
    fun partsCents(rows: List<PartAmounts>): Long? = rows.mapNotNull { it.totalCents }.takeIf { it.isNotEmpty() }?.sum()

    /** How many of [rows] have no total, and so aren't in [partsCents]. */
    fun missingTotals(rows: List<PartAmounts>): Int = rows.count { it.totalCents == null }

    private fun missing(a: PartAmounts): PartField = when {
        a.totalCents == null -> PartField.TOTAL
        a.eachCents == null -> PartField.EACH
        else -> PartField.QUANTITY
    }
}

object PartQuantity {
    /** "4" -> 4000, "5.7" -> 5700, "1,000" -> 1000000. Null unless a plain non-negative number with at most three decimals. */
    fun parseMilli(text: String): Long? {
        val cleaned = text.filterNot { it == ',' || it.isWhitespace() }
        val match = Regex("""(\d{1,7})(?:\.(\d{1,3}))?""").matchEntire(cleaned) ?: return null
        return match.groupValues[1].toLong() * 1000 + match.groupValues[2].padEnd(3, '0').toLong()
    }
}

/**
 * One part row on the Log service form (#502): its figures as typed, which of them was [filled] in from the other
 * two (if any), and the job's line it is tagged to ([line], null for the whole job). [id] is the form's own.
 */
data class PartRowDraft<K>(
    val id: Int,
    val wording: String = "",
    val quantity: String = "",
    val each: String = "",
    val total: String = "",
    val filled: PartField? = null,
    val line: K? = null,
) {
    fun text(field: PartField): String = when (field) {
        PartField.QUANTITY -> quantity
        PartField.EACH -> each
        PartField.TOTAL -> total
    }

    fun with(field: PartField, text: String): PartRowDraft<K> = when (field) {
        PartField.QUANTITY -> copy(quantity = text)
        PartField.EACH -> copy(each = text)
        PartField.TOTAL -> copy(total = text)
    }
}

object PartRowDrafts {
    /**
     * [row] with [text] typed into [field]. A figure filled in before is worked out again from what is typed now,
     * and goes when it no longer can be; typing into it makes it the user's own. Only a blank field is ever filled
     * in. [parse] reads a field's text (null when it isn't a figure); [format] writes a figure back as the field
     * shows it.
     */
    fun <K> type(
        row: PartRowDraft<K>,
        field: PartField,
        text: String,
        parse: (PartField, String) -> Long?,
        format: (PartField, Long) -> String,
    ): PartRowDraft<K> {
        var r = row.with(field, text)
        r.filled?.let { r = if (it == field) r.copy(filled = null) else r.with(it, "").copy(filled = null) }
        val check = PartRows.check(amounts(r, parse))
        if (check is PartCheck.Fills && r.text(check.field).isBlank()) r = r.with(check.field, format(check.field, check.value)).copy(filled = check.field)
        return r
    }

    /** What [row]'s fields hold, as figures. */
    fun <K> amounts(row: PartRowDraft<K>, parse: (PartField, String) -> Long?): PartAmounts = PartAmounts(
        parse(PartField.QUANTITY, row.quantity),
        parse(PartField.EACH, row.each),
        parse(PartField.TOTAL, row.total),
    )

    /**
     * "Itemize parts": [job]'s parts become rows, starting with one row [rowId] whose total is the parts figure
     * already typed, so nothing typed is lost.
     */
    fun <K> itemize(job: JobDraft<K>, rowId: Int): JobDraft<K> =
        if (job.rows != null) job else job.copy(parts = "", rows = listOf(PartRowDraft(rowId, total = job.parts)))

    /** [job] with a new empty row [rowId] at the end. */
    fun <K> add(job: JobDraft<K>, rowId: Int): JobDraft<K> = job.copy(rows = job.rows.orEmpty() + PartRowDraft(rowId))

    /** [job] without row [rowId]; without its last row, its parts are one figure again, blank. */
    fun <K> remove(job: JobDraft<K>, rowId: Int): JobDraft<K> {
        val left = job.rows?.filterNot { it.id == rowId } ?: return job
        return job.copy(rows = left.takeIf { it.isNotEmpty() })
    }

    /** [job] with [update] applied to row [rowId]. */
    fun <K> edit(job: JobDraft<K>, rowId: Int, update: (PartRowDraft<K>) -> PartRowDraft<K>): JobDraft<K> =
        job.copy(rows = job.rows?.map { if (it.id == rowId) update(it) else it })
}

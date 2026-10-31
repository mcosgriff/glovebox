package io.cosgriff.glovebox.domain

import java.time.LocalDate

/**
 * How the Log service form's figures are typed (#578). Reading and writing them is the app's, in its locale; the
 * draft keeps each figure as typed, so a half-typed or mistyped one stays on the form, and asks this for its value.
 */
interface TypedFigures {
    /** A cost as typed; null when blank or not an amount. */
    fun cents(text: String): Long?

    /** A quantity as typed, in thousandths; null when blank or not a quantity. */
    fun milli(text: String): Long?

    /** A cost as its field shows it to edit. */
    fun costText(cents: Long): String

    /** A quantity, from thousandths, as its field shows it to edit. */
    fun quantityText(milli: Long): String

    fun part(field: PartField, text: String): Long? = if (field == PartField.QUANTITY) milli(text) else cents(text)

    fun partText(field: PartField, value: Long): String = if (field == PartField.QUANTITY) quantityText(value) else costText(value)
}

/**
 * What a draft is read against: the vehicle's lines by category, what the app calls a line (`:domain` names none),
 * and how the form's figures are typed.
 */
class LogTerms(
    val categories: List<WorkCategory>,
    val nameOf: (WorkOption) -> String,
    val figures: TypedFigures,
) {
    internal val options: List<WorkOption> get() = categories.flatMap { it.options }
}

/**
 * The work on a Log service form (#578): its lines, their jobs and prices as typed, and what an invoice filled.
 * Plain and immutable, so the form holds it in its state and every rule about it is tested without Android. It is
 * made from a [ServiceRecord] ([of]) and saved as one ([toRecord]); it has no record shape of its own.
 *
 * Lines are known by the form's keys: a catalog line's id, or [customKey] for a custom one.
 */
data class LogDraft(
    val selected: Set<String> = emptySet(),
    val customItems: List<String> = emptyList(),
    /**
     * Ticked line id -> the wording it is saved with in place of the line's name: a custom line filed under a line,
     * or a record's line as it was written (an import's "Oil and filter changed"), which an edit keeps.
     */
    val keptLabels: Map<String, String> = emptyMap(),
    /** The jobs as last changed; [jobs] is what the form shows, kept in line with the ticked lines. */
    val storedJobs: List<JobDraft<String>> = emptyList(),
    /** Lines whose labor or parts the invoice printed and no sum vouched for: outlined "Couldn't read" while blank. */
    val unreadLabor: Set<String> = emptySet(),
    val unreadParts: Set<String> = emptySet(),
    /** Set once an invoice has filled it (#484); it fills a draft once. */
    val invoiceRead: Boolean = false,
    /** The id the next part row gets; ids are the draft's own, never a saved row's. */
    val nextRowId: Int = 1,
) {
    val itemCount: Int get() = selected.size + customItems.size

    fun selectedOptions(terms: LogTerms): List<WorkOption> = terms.options.filter { it.id in selected }

    /** The ticked lines as Work and prices lists them: ticked lines in the catalog's order, then the custom ones. */
    fun lineKeys(terms: LogTerms): List<String> = (selectedOptions(terms).map { it.id } + customItems.map(::customKey)).distinct()

    /** Every ticked line in exactly one job (#481): its own until the user prices it together with another. */
    fun jobs(terms: LogTerms): List<JobDraft<String>> = JobGroups.sync(storedJobs, lineKeys(terms))

    fun toggle(optionId: String): LogDraft {
        val ticked = if (optionId in selected) selected - optionId else selected + optionId
        return copy(selected = ticked, keptLabels = if (optionId in ticked) keptLabels else keptLabels - optionId)
    }

    /** What a ticked line is saved as: its wording as written where it has one, otherwise its name. */
    fun labelOf(option: WorkOption, terms: LogTerms): String = keptLabels[option.id] ?: terms.nameOf(option)

    /** What a line is called on the form: as it will be saved. */
    fun lineName(key: String, terms: LogTerms): String =
        selectedOptions(terms).firstOrNull { it.id == key }?.let { labelOf(it, terms) } ?: key.removePrefix(CUSTOM_KEY)

    fun addCustom(label: String): LogDraft {
        val trimmed = label.trim()
        return if (trimmed.isNotEmpty() && trimmed !in customItems) copy(customItems = customItems + trimmed) else this
    }

    fun removeCustom(label: String): LogDraft = copy(customItems = customItems - label)

    /**
     * The custom line [label] goes under [option], keeping its wording: the line decides which reminders it counts
     * for, and nothing else about it changes. Its job, prices and all, goes with it. A line already ticked keeps its
     * own wording.
     */
    fun fileCustom(label: String, option: WorkOption, terms: LogTerms): LogDraft {
        if (option.id in selected) return this
        val from = customKey(label)
        fun Set<String>.moved() = if (from in this) this - from + option.id else this
        return copy(
            storedJobs = jobs(terms).map { j ->
                j.copy(lines = j.lines.map { if (it == from) option.id else it }, rows = j.rows?.map { if (it.line == from) it.copy(line = option.id) else it })
            },
            unreadLabor = unreadLabor.moved(),
            unreadParts = unreadParts.moved(),
            customItems = customItems - label,
            selected = selected + option.id,
            keptLabels = if (label != terms.nameOf(option)) keptLabels + (option.id to label) else keptLabels,
        )
    }

    fun setLabor(jobId: Int, text: String, terms: LogTerms): LogDraft = withJob(jobId, terms) { it.copy(labor = text) }

    fun setParts(jobId: Int, text: String, terms: LogTerms): LogDraft = withJob(jobId, terms) { it.copy(parts = text) }

    /** "Priced together with…": the one-line job [from] goes into [into], which keeps its prices. */
    fun join(from: Int, into: Int, terms: LogTerms): LogDraft = copy(storedJobs = JobGroups.join(jobs(terms), from, into))

    /** "Price separately": each of the job's lines its own job again, unpriced. */
    fun split(jobId: Int, terms: LogTerms): LogDraft = copy(storedJobs = JobGroups.split(jobs(terms), jobId))

    /** "Itemize parts": the job's parts become rows, a typed parts figure the first row's total. The row is [nextRowId]. */
    fun itemize(jobId: Int, terms: LogTerms): LogDraft =
        withJob(jobId, terms) { PartRowDrafts.itemize(it, nextRowId) }.copy(nextRowId = nextRowId + 1)

    /** A new empty row at the end of the job's; the row is [nextRowId]. */
    fun addRow(jobId: Int, terms: LogTerms): LogDraft =
        withJob(jobId, terms) { PartRowDrafts.add(it, nextRowId) }.copy(nextRowId = nextRowId + 1)

    fun removeRow(jobId: Int, rowId: Int, terms: LogTerms): LogDraft = withJob(jobId, terms) { PartRowDrafts.remove(it, rowId) }

    /** [text] typed into one of a row's figures: two typed fill the third, only when it comes out exact. */
    fun typeRow(jobId: Int, rowId: Int, field: PartField, text: String, terms: LogTerms): LogDraft =
        editRow(jobId, rowId, terms) { PartRowDrafts.type(it, field, text, terms.figures::part, terms.figures::partText) }

    /**
     * [text] typed into a row's Part: the part as the invoice prints it, keeping the line it is for. Typing one of the
     * job's lines by its name picks that line; clearing the box clears both.
     */
    fun nameRow(jobId: Int, rowId: Int, text: String, terms: LogTerms): LogDraft {
        val line = jobs(terms).firstOrNull { it.id == jobId }?.lines?.firstOrNull { lineName(it, terms) == text }
        return when {
            line != null -> pickPart(jobId, rowId, line, terms)
            text.isEmpty() -> editRow(jobId, rowId, terms) { it.copy(wording = "", line = null) }
            else -> editRow(jobId, rowId, terms) { it.copy(wording = text) }
        }
    }

    /** One of the job's lines picked as the row's Part: the row is named by it and tagged to it. A label only. */
    fun pickPart(jobId: Int, rowId: Int, line: String, terms: LogTerms): LogDraft =
        editRow(jobId, rowId, terms) { it.copy(wording = "", line = line) }

    /** The job's line [line] is for; null for the whole job. Asked only of a row typed as printed. */
    fun tagRow(jobId: Int, rowId: Int, line: String?, terms: LogTerms): LogDraft = editRow(jobId, rowId, terms) { it.copy(line = line) }

    /** What a row's Part box shows: its wording as printed, or the line picked for it. */
    fun partText(row: PartRowDraft<String>, terms: LogTerms): String = row.wording.ifEmpty { row.line?.let { lineName(it, terms) }.orEmpty() }

    /** A row's figures as typed. */
    fun amounts(row: PartRowDraft<String>, terms: LogTerms): PartAmounts = PartRowDrafts.amounts(row, terms.figures::part)

    /**
     * A job's prices as typed; one that isn't an amount counts as not entered, and [pricesValid] says so. With part
     * rows, its parts are their totals added up.
     */
    fun price(job: JobDraft<String>, terms: LogTerms): JobPrice =
        JobPrice(terms.figures.cents(job.labor), job.rows?.let { rows -> PartRows.partsCents(rows.map { amounts(it, terms) }) } ?: terms.figures.cents(job.parts))

    /** Every job's figures, and its rows', blank or an amount: part of whether the form can be saved. */
    fun pricesValid(terms: LogTerms): Boolean = jobs(terms).all { j ->
        listOf(j.labor, j.parts).all { it.isBlank() || terms.figures.cents(it) != null } &&
            j.rows.orEmpty().all { r -> PartField.entries.all { f -> r.text(f).isBlank() || terms.figures.part(f, r.text(f)) != null } }
    }

    fun laborUnread(job: JobDraft<String>): Boolean = job.labor.isBlank() && job.lines.any { it in unreadLabor }

    fun partsUnread(job: JobDraft<String>): Boolean = job.rows == null && job.parts.isBlank() && job.lines.any { it in unreadParts }

    /**
     * What an invoice was read as merged into this draft (#484), for the user to check against it; nothing is saved
     * until they save. Each service is a line of the shop's wording: under the line the user's rule files it under,
     * or a custom line, and its own job with the prices a sum vouched for. What was ticked stays. Null once an
     * invoice has filled it: a draft fills once.
     */
    fun fill(fill: InvoiceFill, terms: LogTerms): InvoiceMerge? {
        if (invoiceRead) return null
        var draft = copy(invoiceRead = true)
        var stored = jobs(terms)
        var next = (stored.maxOfOrNull { it.id } ?: 0) + 1
        val labor = mutableSetOf<String>()
        val parts = mutableSetOf<String>()
        val filled = mutableSetOf<String>()
        val leftOut = fill.leftOut.toMutableList()
        // A line is free for a service unless another service took it, or the user already typed a price on it.
        fun free(key: String) = key !in filled && stored.none { s -> key in s.lines && (s.lines.size > 1 || s.labor.isNotBlank() || s.parts.isNotBlank() || s.rows != null) }
        fill.jobs.forEach { j ->
            val wording = j.wording.trim()
            val option = j.filed?.let { id -> terms.options.firstOrNull { it.id == id } }?.takeIf { free(it.id) }
            val key = when {
                option != null -> {
                    if (option.id !in draft.selected) {
                        draft = draft.copy(
                            selected = draft.selected + option.id,
                            keptLabels = if (wording != terms.nameOf(option)) draft.keptLabels + (option.id to wording) else draft.keptLabels,
                        )
                    }
                    option.id
                }
                wording.isNotEmpty() && free(customKey(wording)) -> {
                    draft = draft.addCustom(wording)
                    customKey(wording)
                }
                else -> {
                    // Nowhere of its own to go: said so on the form, never dropped quietly.
                    leftOut += j.wording
                    return@forEach
                }
            }
            filled += key
            stored = stored.mapNotNull { s -> s.copy(lines = s.lines - key).takeIf { it.lines.isNotEmpty() } } +
                JobDraft(next++, listOf(key), labor = j.laborCents?.let(terms.figures::costText).orEmpty(), parts = j.partsCents?.let(terms.figures::costText).orEmpty())
            if (j.laborUnread) labor += key
            if (j.partsUnread) parts += key
        }
        return InvoiceMerge(
            draft = draft.copy(storedJobs = stored, unreadLabor = labor, unreadParts = parts),
            leftOut = leftOut,
            opensPrices = fill.jobs.any { it.laborCents != null || it.partsCents != null || it.laborUnread || it.partsUnread },
        )
    }

    /**
     * The draft saved as a record: [visit] (date, odometer, shop, cost, notes, charges) with these lines and jobs, the
     * jobs that say nothing left out ([ServiceRecord.withoutBareJobs]).
     */
    fun toRecord(visit: ServiceRecord, terms: LogTerms): ServiceRecord {
        val catalogIds = terms.categories.filter { it.id != ServiceCatalog.YOUR_ITEMS }.flatMap { it.options }.map { it.id }.toSet()
        val options = selectedOptions(terms)
        val jobs = jobs(terms)
        // Each line by its key, so its job and its part rows can find it.
        val lineIndex = (options.map { it.id } + customItems.map(::customKey)).withIndex().associate { (i, key) -> key to i }
        val jobIndex = jobs.withIndex().flatMap { (i, j) -> j.lines.map { it to i } }.toMap()
        return visit.copy(
            lines = options.map { RecordLine(labelOf(it, terms), it.id.takeIf { id -> id in catalogIds }, it.scheduleItemId, jobIndex[it.id]) } +
                customItems.map { RecordLine(it, job = jobIndex[customKey(it)]) },
            jobs = jobs.map { j ->
                val price = price(j, terms)
                // A job with part rows stores no parts figure: it is their totals, worked out on read.
                RecordJob(
                    wording = j.wording,
                    laborCents = price.laborCents,
                    partsCents = if (j.rows == null) price.partsCents else null,
                    rows = j.rows.orEmpty().map { r ->
                        val a = amounts(r, terms)
                        RecordPartRow(r.wording.trim().ifEmpty { null }, a.quantityMilli, a.eachCents, a.totalCents, r.line?.let(lineIndex::get))
                    },
                    itemized = j.rows != null,
                )
            },
        ).withoutBareJobs()
    }

    private fun withJob(jobId: Int, terms: LogTerms, update: (JobDraft<String>) -> JobDraft<String>): LogDraft =
        copy(storedJobs = jobs(terms).map { if (it.id == jobId) update(it) else it })

    private fun editRow(jobId: Int, rowId: Int, terms: LogTerms, update: (PartRowDraft<String>) -> PartRowDraft<String>): LogDraft =
        withJob(jobId, terms) { PartRowDrafts.edit(it, rowId, update) }

    companion object {
        private const val CUSTOM_KEY = "custom:"

        /** The form's key for a custom line, beside the catalog's line ids. */
        fun customKey(label: String): String = CUSTOM_KEY + label

        /**
         * [record] as a draft to edit: each line ticked as the line it was saved under ([optionFor] finds one by the
         * reminder it resets, for a line with no catalog id), or a custom line; its wording kept where it isn't the
         * line's name; its jobs and part rows with their figures as saved. Nothing is filled in on opening.
         */
        fun of(record: ServiceRecord, optionFor: Map<Long?, WorkOption>, terms: LogTerms): LogDraft {
            // The option each saved line is ticked as; null for a custom line.
            val optionOf = record.lines.map { line -> terms.options.firstOrNull { it.id == line.catalogId } ?: line.scheduleItemId?.let(optionFor::get) }
            val ticked = mutableSetOf<String>()
            val custom = mutableListOf<String>()
            val kept = mutableMapOf<String, String>()
            record.lines.zip(optionOf).forEach { (line, option) ->
                if (option == null) {
                    custom += line.label
                } else if (ticked.add(option.id) && line.label != terms.nameOf(option)) {
                    // Saved as written; an edit doesn't put the line's name in its place.
                    kept[option.id] = line.label
                }
            }
            // Each line under the job it was priced in, by its key.
            val keyOf = record.lines.mapIndexed { i, line -> optionOf[i]?.id ?: customKey(line.label) }
            var nextRow = 1
            val figures = terms.figures
            val jobs = record.jobs.mapIndexed { j, job ->
                val rows = job.rows.map { row ->
                    PartRowDraft(
                        id = nextRow++,
                        wording = row.wording.orEmpty(),
                        quantity = row.quantityMilli?.let(figures::quantityText).orEmpty(),
                        each = row.eachCents?.let(figures::costText).orEmpty(),
                        total = row.totalCents?.let(figures::costText).orEmpty(),
                        line = row.line?.let(keyOf::getOrNull),
                    )
                }
                JobDraft(
                    id = j + 1,
                    lines = record.linesOf(j).map(keyOf::get).distinct(),
                    wording = job.wording,
                    labor = job.laborCents?.let(figures::costText).orEmpty(),
                    parts = job.partsCents?.let(figures::costText).orEmpty(),
                    rows = rows.takeIf { it.isNotEmpty() },
                )
            }.filter { it.lines.isNotEmpty() }
            return LogDraft(selected = ticked, customItems = custom, keptLabels = kept, storedJobs = jobs, nextRowId = nextRow)
        }
    }
}

/**
 * An invoice merged into a draft ([LogDraft.fill]): the draft as it now stands, the services it couldn't take as
 * lines of their own (the user adds them), and whether the prices section opens. The visit's own figures (date,
 * odometer, shop, total) are offered as [InvoiceFill] has them; [InvoiceFill.dateOn] and [InvoiceFill.shopAmong]
 * say how the form takes the date and the shop.
 */
data class InvoiceMerge(
    val draft: LogDraft,
    val leftOut: List<String>,
    val opensPrices: Boolean,
)

/** The invoice's date as the form takes it: never one after [today]. */
fun InvoiceFill.dateOn(today: LocalDate): LocalDate? = date?.takeIf { !it.isAfter(today) }

/** The invoice's shop as the form takes it: a saved shop's spelling where it is one of [shops]. */
fun InvoiceFill.shopAmong(shops: List<ShopUse>): String? = shop?.let { s -> shops.firstOrNull { it.name.equals(s, ignoreCase = true) }?.name ?: s }

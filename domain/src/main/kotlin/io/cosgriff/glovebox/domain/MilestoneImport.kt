package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** One line of work at a milestone in the user's own table. [service] is a [CatalogItem] id; null for anything else. */
data class TableJob(val name: String, val service: String? = null)

/** One milestone from a `milestone-schedule` import file or from the editor ([MilestoneEdits]), already checked. */
data class ImportedMilestone(val atMiles: Int, val work: List<TableJob>, val orMonths: Int? = null)

/** A saved milestone of the user's own table. */
data class SavedMilestone(val id: Long, val atMiles: Int, val work: List<TableJob>, val orMonths: Int? = null)

/**
 * The `milestone-schedule` kind: `docs/import/schema/milestone-schedule.schema.json`. A table the
 * user copied from their manual: mileage points, each with the work due there. It is shown on the
 * Milestones screen in place of the view built from the schedule and is information only; nothing
 * here creates a reminder.
 *
 * A table either ends at its last milestone or repeats: `repeatsEveryMiles`, beside `items`, is the
 * length of the whole cycle, so every milestone at mileage `a` comes round again at `a` plus that,
 * and again, with the same work. It belongs to the table as a whole, never to one milestone.
 */
object MilestoneImport {
    const val MAX_ITEMS = 200
    const val MAX_WORK = 40
    const val MAX_NAME = 80
    const val MAX_MILES = 1_000_000
    const val MAX_MONTHS = 600

    /** The file's fields for a table that repeats: one or the other, in the unit the milestones are in. */
    const val REPEATS = "repeatsEveryMiles"
    const val REPEATS_KM = "repeatsEveryKm"

    /** The same limit for a table given in kilometres: 1,000,000 mi as a schedule figure. */
    const val MAX_KM = 1_600_000

    private val itemKeys = setOf("atMiles", "atKm", "orMonths", "work")

    /**
     * The rules a table keeps however it is made, shared by the file reader here and the editor
     * ([MilestoneEdits]): one milestone per mileage, a name and a service once each per milestone,
     * and a cycle no shorter than the table.
     *
     * Of [keys], each entry that says the same as an earlier one, by index, with the index of the
     * one before it that it repeats. An entry that is null takes no part.
     */
    fun <T : Any> repeated(keys: List<T?>): Map<Int, Int> {
        val seen = mutableMapOf<T, Int>()
        val out = mutableMapOf<Int, Int>()
        keys.forEachIndexed { i, key -> if (key != null) seen.put(key, i)?.let { out[i] = it } }
        return out
    }

    /** What makes two names in one milestone the same name. */
    fun nameKey(name: String): String = Names.key(name)

    /** A cycle shorter than the table would put a repeat of an early milestone among the later ones. */
    fun cycleCovers(everyMiles: Int, lastMiles: Int): Boolean = everyMiles >= lastMiles

    /** Any order in the file; the result is in mileage order. */
    fun read(items: JsonElement, check: ImportCheck): List<ImportedMilestone> {
        val list = check.array(items, "items", 1, MAX_ITEMS) ?: return emptyList()
        val ats = arrayOfNulls<Int>(list.size)
        val read = list.mapIndexedNotNull { i, e ->
            val path = ImportCheck.at("items", i)
            val o = check.obj(e, path, itemKeys, required = setOf("work")) ?: return@mapIndexedNotNull null
            if (!check.has(o, "atMiles", "atKm")) check.fail(path, "needs \"atMiles\" or \"atKm\"")
            val at = check.distance(o, path, "atMiles", "atKm", MAX_MILES, MAX_KM)
            val months = o["orMonths"]?.let { check.int(it, "$path.orMonths", 1, MAX_MONTHS) }
            val work = o["work"]?.let { work(it, "$path.work", check) }
            ats[i] = at
            if (at == null || work == null || ("orMonths" in o && months == null)) return@mapIndexedNotNull null
            ImportedMilestone(at, work, months)
        }
        repeated(ats.asList()).forEach { (i, first) ->
            check.fail("${ImportCheck.at("items", i)}.${if (check.unit == DistanceUnit.KILOMETERS) "atKm" else "atMiles"}", "is the same mileage as items[$first]; put all the work for one mileage in one milestone")
        }
        return read.sortedBy { it.atMiles }
    }

    /**
     * The length of the cycle. It can't be shorter than the table, or a repeat of an early milestone
     * would land among the later ones; that is the rule a schema can't state.
     */
    fun repeats(envelope: JsonObject, items: List<ImportedMilestone>, check: ImportCheck): Int? {
        val every = check.distance(envelope, "", REPEATS, REPEATS_KM, MAX_MILES, MAX_KM) ?: return null
        val last = items.maxOfOrNull { it.atMiles } ?: return every
        if (!cycleCovers(every, last)) {
            val (field, at) = if (check.unit == DistanceUnit.KILOMETERS) REPEATS_KM to "atKm" else REPEATS to "atMiles"
            check.fail(field, "is $every, less than the highest $at in the file ($last); it is the length of the whole cycle, so it can't be shorter than the table")
            return null
        }
        return every
    }

    private fun work(e: JsonElement, path: String, check: ImportCheck): List<TableJob>? {
        val list = check.array(e, path, 1, MAX_WORK) ?: return null
        var ok = true
        val names = arrayOfNulls<String>(list.size)
        val services = arrayOfNulls<String>(list.size)
        val jobs = list.mapIndexedNotNull { i, line ->
            val at = ImportCheck.at(path, i)
            val o = check.obj(line, at, setOf("name", "service"), required = setOf("name"))
            val name = o?.get("name")?.let { check.string(it, "$at.name", MAX_NAME) }
            val service = o?.get("service")?.let { check.choice(it, "$at.service", HistoryImport.services.mapValues { s -> s.key }) }
            if (o == null || name == null || ("service" in o && service == null)) ok = false
            names[i] = name
            services[i] = service
            name?.let { TableJob(it, service) }
        }
        repeated(names.map { it?.let(::nameKey) }).forEach { (i, first) ->
            check.fail("${ImportCheck.at(path, i)}.name", "\"${names[i]}\" is already in this milestone, at work[$first]")
        }
        // Two lines for one service would be one job twice: the log couldn't tell them apart.
        repeated(services.asList()).forEach { (i, first) ->
            check.fail("${ImportCheck.at(path, i)}.service", "\"${services[i]}\" is already used in this milestone, at work[$first]")
        }
        return jobs.takeIf { ok }
    }

    /**
     * What makes a job the same job at another milestone, and what in the log completes it: its
     * service where it names one, otherwise its name.
     */
    fun jobKey(job: TableJob): String = job.service?.let { "service:$it" } ?: "name:${nameKey(job.name)}"

    /** The keys a logged line of work answers to: its catalog line if it has one, and what it is called. */
    fun doneKeys(label: String, catalogId: String?): List<String> =
        listOfNotNull(catalogId?.let { "service:$it" }, "name:${nameKey(label)}") +
            // Work that includes other work completes that job too: a change of oil, the check of it.
            HistoryImport.services[catalogId]?.covers.orEmpty().map { "service:$it" }

    /** The saved table as the Milestones screen takes it, for a table that ends where it ends. */
    fun milestones(table: List<SavedMilestone>): List<Milestone> =
        table.sortedBy { it.atMiles }.map { m -> Milestone(m.atMiles, m.work.map { MilestoneJob(jobKey(it), it.name) }, m.orMonths) }

    /**
     * A table that repeats every [every] miles, laid out as far as the screen shows it: the whole
     * table as written, and after it the same milestones again at their mileage plus [every], plus
     * twice that, and so on. It runs to the [Milestones.AHEAD]th point past [current] or the end of
     * the table as written, whichever is further, and [more] points beyond that.
     *
     * A repeat carries [Milestone.repeatOf] and no `orMonths`: that is the vehicle's age when the
     * milestone first comes due, and the file doesn't say how old it will be the next time round.
     * Only the newest [Milestones.MAX_POINTS] are built.
     */
    fun milestones(table: List<SavedMilestone>, every: Int, current: Int?, more: Int = 0): List<Milestone> {
        val base = milestones(table)
        if (base.isEmpty() || every < base.last().atMiles) return base
        val n = base.size.toLong()
        val now = (current ?: 0).coerceAtLeast(0)
        // Every earlier cycle is wholly behind the odometer, and of this one the points up to where it has got.
        val passed = now / every * n + base.count { it.atMiles <= now % every }
        val end = maxOf(n, passed + Milestones.AHEAD) + more.coerceAtLeast(0)
        return (maxOf(0L, end - Milestones.MAX_POINTS) until end).mapNotNull { index ->
            val cycle = index / n
            val m = base[(index % n).toInt()]
            val at = m.atMiles + cycle * every
            when {
                at > Int.MAX_VALUE -> null
                cycle == 0L -> m
                else -> Milestone(at.toInt(), m.jobs, repeatOf = m.atMiles)
            }
        }
    }

    /**
     * Why a file can't go into a vehicle that already has a table, or null when it can. A table
     * repeats one way or not at all, so a file that says otherwise than the saved table is refused
     * whole, whichever [ImportMode] would have been chosen: nothing here is settled by guessing
     * which of the two the user means. Removing the table and importing again is how it is changed.
     * `app` words it.
     */
    fun repeatConflict(fileRepeats: Int?, saved: List<SavedMilestone>, savedRepeats: Int?): RepeatConflict? =
        if (saved.isEmpty() || fileRepeats == savedRepeats) null else RepeatConflict(savedRepeats, fileRepeats)

    private fun lines(work: List<TableJob>) = work.map { nameKey(it.name) to it.service }.toSet()

    /** A milestone in the file matches a saved one at the same mileage. Same means the months and the work lines, in any order. */
    fun plan(items: List<ImportedMilestone>, existing: List<SavedMilestone>): ImportPlan<ImportedMilestone, SavedMilestone> =
        ImportPlans.plan(
            items = items,
            existing = existing,
            key = { it.atMiles },
            existingKey = { it.atMiles },
            same = { i, e -> i.orMonths == e.orMonths && lines(i.work) == lines(e.work) },
        )
}

/** How the saved table and a file repeat, when they differ; null [saved] or [file] doesn't repeat. */
data class RepeatConflict(val saved: Int?, val file: Int?)

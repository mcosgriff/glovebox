package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import java.util.Locale

/** One line of work at a milestone in the user's own table. [service] is a [CatalogItem] id; null for anything else. */
data class TableJob(val name: String, val service: String? = null)

/** One milestone from a `milestone-schedule` import file, already checked. */
data class ImportedMilestone(val atMiles: Int, val work: List<TableJob>, val orMonths: Int? = null)

/** A saved milestone of the user's own table. */
data class SavedMilestone(val id: Long, val atMiles: Int, val work: List<TableJob>, val orMonths: Int? = null)

/**
 * The `milestone-schedule` kind: `docs/import/schema/milestone-schedule.schema.json`. A table the
 * user copied from their manual: mileage points, each with the work due there. It is shown on the
 * Milestones screen in place of the view built from the schedule and is information only; nothing
 * here creates a reminder.
 */
object MilestoneImport {
    const val MAX_ITEMS = 200
    const val MAX_WORK = 30
    const val MAX_NAME = 80
    const val MAX_MILES = 1_000_000
    const val MAX_MONTHS = 600

    private val itemKeys = setOf("atMiles", "orMonths", "work")

    /** Any order in the file; the result is in mileage order. */
    fun read(items: JsonElement, check: ImportCheck): List<ImportedMilestone> {
        val list = check.array(items, "items", 1, MAX_ITEMS) ?: return emptyList()
        val seen = mutableMapOf<Int, Int>()
        return list.mapIndexedNotNull { i, e ->
            val path = ImportCheck.at("items", i)
            val o = check.obj(e, path, itemKeys, required = setOf("atMiles", "work")) ?: return@mapIndexedNotNull null
            val at = o["atMiles"]?.let { check.int(it, "$path.atMiles", 1, MAX_MILES) }
            val months = o["orMonths"]?.let { check.int(it, "$path.orMonths", 1, MAX_MONTHS) }
            val work = o["work"]?.let { work(it, "$path.work", check) }
            if (at != null) seen.put(at, i)?.let { first -> check.fail("$path.atMiles", "is the same mileage as items[$first]; put all the work for one mileage in one milestone") }
            if (at == null || work == null || ("orMonths" in o && months == null)) return@mapIndexedNotNull null
            ImportedMilestone(at, work, months)
        }.sortedBy { it.atMiles }
    }

    private fun work(e: JsonElement, path: String, check: ImportCheck): List<TableJob>? {
        val list = check.array(e, path, 1, MAX_WORK) ?: return null
        var ok = true
        val names = mutableMapOf<String, Int>()
        val services = mutableMapOf<String, Int>()
        val jobs = list.mapIndexedNotNull { i, line ->
            val at = ImportCheck.at(path, i)
            val o = check.obj(line, at, setOf("name", "service"), required = setOf("name"))
            val name = o?.get("name")?.let { check.string(it, "$at.name", MAX_NAME) }
            val service = o?.get("service")?.let { check.choice(it, "$at.service", HistoryImport.services.mapValues { s -> s.key }) }
            if (o == null || name == null || ("service" in o && service == null)) ok = false
            name?.let { n -> names.put(n.lowercase(Locale.US), i)?.let { first -> check.fail("$at.name", "\"$n\" is already in this milestone, at work[$first]") } }
            // Two lines for one service would be one job twice: the log couldn't tell them apart.
            service?.let { s -> services.put(s, i)?.let { first -> check.fail("$at.service", "\"$s\" is already used in this milestone, at work[$first]") } }
            name?.let { TableJob(it, service) }
        }
        return jobs.takeIf { ok }
    }

    /**
     * What makes a job the same job at another milestone, and what in the log completes it: its
     * service where it names one, otherwise its name.
     */
    fun jobKey(job: TableJob): String = job.service?.let { "service:$it" } ?: "name:${job.name.trim().lowercase(Locale.US)}"

    /** The keys a logged line of work answers to: its catalog line if it has one, and what it is called. */
    fun doneKeys(label: String, catalogId: String?): List<String> =
        listOfNotNull(catalogId?.let { "service:$it" }, "name:${label.trim().lowercase(Locale.US)}")

    /** The saved table as the Milestones screen takes it. */
    fun milestones(table: List<SavedMilestone>): List<Milestone> =
        table.sortedBy { it.atMiles }.map { m -> Milestone(m.atMiles, m.work.map { MilestoneJob(jobKey(it), it.name) }, m.orMonths) }

    private fun lines(work: List<TableJob>) = work.map { it.name.trim().lowercase(Locale.US) to it.service }.toSet()

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

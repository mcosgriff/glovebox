package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The user's own milestone table written back out as a `milestone-schedule` import file, to keep
 * outside the app or pass to another owner of the same model.
 *
 * What it writes is exactly what [ImportFormat] reads, at the format's own version, and a test puts
 * every table through both. Like any import file it never names a vehicle: it holds the mileages,
 * the months and the jobs, and nothing about the vehicle the table was on or what was done to it.
 */
object MilestoneExport {
    /** [table] in any order; the file is in mileage order, with each milestone's jobs in the user's. */
    fun file(table: List<SavedMilestone>, repeatsEveryMiles: Int?, unit: DistanceUnit): String {
        // The file is in miles ([FileDistances]); the table is in the vehicle's unit.
        val (inMiles, repeats) = FileDistances.milestonesOut(table, repeatsEveryMiles, unit)
        return text(inMiles, repeats)
    }

    private fun text(table: List<SavedMilestone>, repeatsEveryMiles: Int?): String = ImportFiles.text(
        kind = ImportKind.MILESTONE_SCHEDULE,
        items = table.sortedBy { it.atMiles }.map { m ->
            buildJsonObject {
                put("atMiles", m.atMiles)
                m.orMonths?.let { put("orMonths", it) }
                put(
                    "work",
                    buildJsonArray {
                        m.work.forEach { job ->
                            add(
                                buildJsonObject {
                                    put("name", job.name)
                                    job.service?.let { put("service", it) }
                                },
                            )
                        }
                    },
                )
            }
        },
        own = { repeatsEveryMiles?.let { put(MilestoneImport.REPEATS, it) } },
    )

    /** The table as the save row takes it. Nothing is ever left out: the editor and the reader keep the same rules. */
    fun written(table: List<SavedMilestone>, repeatsEveryMiles: Int?, unit: DistanceUnit): WrittenFile =
        WrittenFile(if (table.isEmpty()) null else file(table, repeatsEveryMiles, unit), table.size)

    /** "glovebox-milestones-2003-honda-accord.json". [model] is the year, make and model: who else the table would suit. */
    fun fileName(model: String): String = ImportFiles.fileName("milestones", model)
}

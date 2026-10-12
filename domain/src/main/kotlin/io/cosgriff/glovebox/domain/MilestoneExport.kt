package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The user's own milestone table written back out as a `milestone-schedule` import file, to keep
 * outside the app or pass to another owner of the same model.
 *
 * What it writes is exactly what [ImportFormat] reads, at the format's own version, and a test puts
 * every table through both. Like any import file it never names a vehicle: it holds the mileages,
 * the months and the jobs, and nothing about the vehicle the table was on or what was done to it.
 */
object MilestoneExport {
    const val MIME_TYPE = "application/json"

    /** Lets an editor check the file as it is changed; Glovebox ignores it on the way back in. */
    const val SCHEMA = "https://raw.githubusercontent.com/mcosgriff/glovebox/main/docs/import/schema/milestone-schedule.schema.json"

    private val pretty = Json { prettyPrint = true }

    /** [table] in any order; the file is in mileage order, with each milestone's jobs in the user's. */
    fun file(table: List<SavedMilestone>, repeatsEveryMiles: Int?): String {
        val root = buildJsonObject {
            put("\$schema", SCHEMA)
            put("glovebox", "import")
            put("version", ImportFormat.VERSION)
            put("kind", ImportKind.MILESTONE_SCHEDULE.id)
            repeatsEveryMiles?.let { put(MilestoneImport.REPEATS, it) }
            putJsonArray("items") {
                table.sortedBy { it.atMiles }.forEach { m ->
                    add(
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
                        },
                    )
                }
            }
        }
        return pretty.encodeToString(root) + "\n"
    }

    /** "glovebox-milestones-2003-honda-accord.json". [model] is the year, make and model: who else the table would suit. */
    fun fileName(model: String): String = "glovebox-milestones-${Exports.slug(model)}.json"
}

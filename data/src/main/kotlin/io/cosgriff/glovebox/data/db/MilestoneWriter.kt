package io.cosgriff.glovebox.data.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.domain.ImportPlans
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.SavedMilestone
import io.cosgriff.glovebox.domain.TableJob
import javax.inject.Inject
import javax.inject.Singleton

/** A milestone of the owner's table as an import sees it. */
fun MilestoneWithJobs.toSaved() = SavedMilestone(milestone.id, milestone.atMiles, ordered.map { TableJob(it.name, it.service) }, milestone.orMonths)

/**
 * A `milestone-schedule` file written into the owner's table (#645), in one transaction that plans again from the
 * table as it stands inside it. A milestone the file matched (by mileage) gets only what the file says of it, its
 * months and its jobs, never a whole row from a copy read earlier.
 */
@Singleton
class MilestoneWriter @Inject constructor(private val db: GloveboxDatabase) {
    private val milestones = db.milestones()
    private val vehicles = db.vehicles()

    /**
     * Writes a file as its [preview] planned it: nothing is added twice, and no milestone changed since is written over
     * unless the preview listed it ([ImportPlans.recheck]). [repeatsEveryMiles] is the file's, kept on the vehicle.
     * False, with nothing written, when the table now repeats another way than the file: a table repeats one way or
     * not at all ([MilestoneImport.repeatConflict]). Nothing is deleted.
     */
    suspend fun import(vehicleId: Long, repeatsEveryMiles: Int?, preview: ImportPlan<ImportedMilestone, SavedMilestone>, mode: ImportMode): Boolean =
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                val vehicle = vehicles.byId(vehicleId) ?: return@immediateTransaction false
                val table = milestones.tableFor(vehicleId).map { it.toSaved() }
                if (MilestoneImport.repeatConflict(repeatsEveryMiles, table, vehicle.milestonesRepeatEveryMiles) != null) return@immediateTransaction false
                val write = ImportPlans.recheck(preview, MilestoneImport.plan(preview.items, table), mode) { it.id }
                milestones.setRepeat(vehicleId, repeatsEveryMiles)
                write.added.forEach { item -> item.toRows(vehicleId).let { (milestone, jobs) -> milestones.save(milestone, jobs) } }
                write.replaced.forEach { (item, saved) ->
                    milestones.setOrMonths(saved.id, item.orMonths)
                    milestones.setJobs(saved.id, item.jobRows())
                }
                true
            }
        }
}

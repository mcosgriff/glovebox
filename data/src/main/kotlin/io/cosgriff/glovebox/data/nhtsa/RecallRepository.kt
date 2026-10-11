package io.cosgriff.glovebox.data.nhtsa

import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.StoredRecall
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

sealed interface RecallRefresh {
    /** NHTSA answered; [added] campaigns weren't on file before. */
    data class Checked(val added: Int) : RecallRefresh

    /** Offline or NHTSA didn't answer. The stored list is untouched. */
    data object Failed : RecallRefresh

    /** No usable year, make and model to ask NHTSA about. */
    data object CannotCheck : RecallRefresh
}

/** Keeps each vehicle's recall list on the phone, so it reads offline and the done marks last. */
class RecallRepository(
    private val nhtsa: NhtsaClient,
    private val vehicles: VehicleDao,
    private val recalls: RecallDao,
) {
    /** Only the year, make and model are sent. */
    suspend fun refresh(vehicleId: Long, today: LocalDate = LocalDate.now()): RecallRefresh {
        val v = vehicles.byId(vehicleId) ?: return RecallRefresh.CannotCheck
        if (!Recalls.canCheck(v.year, v.make, v.model)) return RecallRefresh.CannotCheck
        val fetched = try {
            nhtsa.recalls(v.year, v.make, v.model)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return RecallRefresh.Failed
        }
        return RecallRefresh.Checked(store(vehicleId, fetched, today))
    }

    /** Merges an already fetched list, e.g. the one shown while adding the vehicle. Returns how many were new. */
    suspend fun store(vehicleId: Long, fetched: List<RecallNotice>, today: LocalDate = LocalDate.now()): Int {
        val stored = recalls.recallsFor(vehicleId)
        val byId = stored.associateBy { it.id }
        val changes = Recalls.merge(stored.map { it.toStored() }, fetched)
        recalls.apply(
            added = changes.added.map { it.toEntity(vehicleId, today) },
            updated = changes.updated.map { (id, n) ->
                byId.getValue(id).copy(
                    component = n.component,
                    summary = n.summary,
                    consequence = n.consequence,
                    remedy = n.remedy,
                    reportedOn = n.reportedOn,
                )
            },
            withdrawn = changes.withdrawn,
        )
        vehicles.setRecallsChecked(vehicleId, today)
        return changes.added.size
    }
}

fun RecallEntity.toNotice() = RecallNotice(campaign, component, summary, consequence, remedy, reportedOn)

fun RecallEntity.toStored() = StoredRecall(id, toNotice(), done = completedOn != null)

private fun RecallNotice.toEntity(vehicleId: Long, firstSeenOn: LocalDate) = RecallEntity(
    vehicleId = vehicleId,
    campaign = campaign,
    component = component,
    summary = summary,
    consequence = consequence,
    remedy = remedy,
    reportedOn = reportedOn,
    firstSeenOn = firstSeenOn,
)

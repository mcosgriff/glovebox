package io.cosgriff.glovebox.data.nhtsa

import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.RecallNames
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.StoredRecall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

sealed interface RecallRefresh {
    /** NHTSA answered; [added] campaigns weren't on file before. */
    data class Checked(val added: List<RecallNotice>) : RecallRefresh

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
    private val settings: SettingsRepository,
) {
    // Two refreshes of one vehicle at once would both try to add the same campaigns.
    private val lock = Mutex()

    /** Asks NHTSA now. Only the year, make and model are sent, once for each name the recalls are checked under. */
    suspend fun refresh(vehicleId: Long, today: LocalDate = LocalDate.now()): RecallRefresh =
        lock.withLock { fetchAndStore(vehicleId, today, onlyIfStale = false) } ?: RecallRefresh.CannotCheck

    /**
     * Asks NHTSA only if the vehicle hasn't been checked for as long as the user's "Check for new
     * recalls" setting says. Null when the list is fresh enough, or when that setting is Never.
     */
    suspend fun refreshIfStale(vehicleId: Long, today: LocalDate = LocalDate.now()): RecallRefresh? =
        lock.withLock { fetchAndStore(vehicleId, today, onlyIfStale = true) }

    /** [refreshIfStale] for every vehicle in the garage. */
    suspend fun refreshStale(today: LocalDate = LocalDate.now()) {
        vehicles.active().forEach { refreshIfStale(it.id, today) }
    }

    /**
     * The other names NHTSA files the vehicle's recalls under, with the ones already ticked (#460). Only the year
     * and make are sent. Throws if NHTSA can't be reached.
     */
    suspend fun offeredNames(vehicleId: Long): List<String> {
        val v = vehicles.byId(vehicleId) ?: return emptyList()
        return RecallNames.offered(v.model, nhtsa.recallModels(v.year, v.make), recalls.namesFor(vehicleId).toSet())
    }

    /** Merges an already fetched list, e.g. the one shown while adding the vehicle. Returns how many were new. */
    suspend fun store(vehicleId: Long, fetched: List<RecallNotice>, today: LocalDate = LocalDate.now()): Int =
        lock.withLock { merge(vehicleId, fetched, today).size }

    private suspend fun fetchAndStore(vehicleId: Long, today: LocalDate, onlyIfStale: Boolean): RecallRefresh? {
        val v = vehicles.byId(vehicleId) ?: return RecallRefresh.CannotCheck
        // Archived vehicles are never sent to NHTSA again.
        if (v.archivedOn != null || !Recalls.canCheck(v.year, v.make, v.model)) return RecallRefresh.CannotCheck
        if (onlyIfStale) {
            val days = settings.settings.first().recallCadence.days ?: return null
            if (!Recalls.isStale(v.recallsCheckedOn, today, days)) return null
        }
        // Every name has to answer: a list missing one name's recalls would withdraw them (#460).
        val fetched = try {
            RecallNames.checkedUnder(v.model, recalls.namesFor(vehicleId)).flatMap { nhtsa.recalls(v.year, v.make, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return RecallRefresh.Failed
        }
        return RecallRefresh.Checked(merge(vehicleId, fetched, today))
    }

    private suspend fun merge(vehicleId: Long, fetched: List<RecallNotice>, today: LocalDate): List<RecallNotice> {
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
                    doNotDrive = n.doNotDrive,
                    parkOutside = n.parkOutside,
                    overTheAir = n.overTheAir,
                )
            },
            withdrawn = changes.withdrawn,
        )
        vehicles.setRecallsChecked(vehicleId, today)
        return changes.added
    }
}

fun RecallEntity.toNotice() = RecallNotice(campaign, component, summary, consequence, remedy, reportedOn, doNotDrive, parkOutside, overTheAir)

fun RecallEntity.toStored() = StoredRecall(id, toNotice(), done = completedOn != null, archived = archivedOn != null)

private fun RecallNotice.toEntity(vehicleId: Long, firstSeenOn: LocalDate) = RecallEntity(
    vehicleId = vehicleId,
    campaign = campaign,
    component = component,
    summary = summary,
    consequence = consequence,
    remedy = remedy,
    reportedOn = reportedOn,
    firstSeenOn = firstSeenOn,
    doNotDrive = doNotDrive,
    parkOutside = parkOutside,
    overTheAir = overTheAir,
)

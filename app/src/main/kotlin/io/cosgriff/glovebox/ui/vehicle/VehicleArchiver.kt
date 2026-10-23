package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.Archive
import io.cosgriff.glovebox.domain.Sale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moves vehicles in and out of the archive. Archiving only sets `vehicles.archivedOn`: nothing is
 * deleted, and every screen that lists the garage already leaves archived vehicles out.
 *
 * A singleton with its own scope because Undo is tapped after the screen that archived the vehicle
 * has closed.
 */
@Singleton
class VehicleArchiver @Inject constructor(private val vehicles: VehicleDao, private val odometer: OdometerDao, @ApplicationScope private val scope: CoroutineScope) {

    /** What [archive] did, so it can be undone exactly. */
    data class Receipt(val vehicleId: Long, val readingId: Long?)

    /** [lastOdometer] is the vehicle's highest reading so far; a higher final odometer is logged as a reading. */
    suspend fun archive(vehicleId: Long, sale: Sale, lastOdometer: Int?): Receipt {
        val readingId = Archive.finalReading(sale, lastOdometer)?.let {
            odometer.insert(OdometerReadingEntity(vehicleId = vehicleId, date = it.date, odometer = it.odometer))
        }
        vehicles.setArchived(vehicleId, sale.soldOn)
        return Receipt(vehicleId, readingId)
    }

    fun undo(receipt: Receipt) {
        scope.launch {
            vehicles.setArchived(receipt.vehicleId, null)
            receipt.readingId?.let { odometer.delete(it) }
        }
    }

    /** "Move back to garage". The final odometer reading stays: it was a real reading. */
    suspend fun moveBack(vehicleId: Long) = vehicles.setArchived(vehicleId, null)

    /** Undo for [moveBack]. */
    fun archiveAgain(vehicleId: Long, soldOn: LocalDate) {
        scope.launch { vehicles.setArchived(vehicleId, soldOn) }
    }
}

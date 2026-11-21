package io.cosgriff.glovebox.data

import android.content.Context
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.VehicleRowCounts
import java.io.File

/** What Delete vehicle takes: [vehicle] and the rows counted in [rows], and the encrypted [files] they point at. */
data class VehicleDeletion(val vehicle: VehicleEntity, val rows: VehicleRowCounts, val files: List<String>)

/** Implements the two destructive flows from the design: delete one vehicle, delete everything. */
class DataWipe(
    private val context: Context,
    private val db: GloveboxDatabase,
    private val keys: DatabaseKeyManager,
    private val photos: PhotoCrypto,
) {
    /**
     * What deleting [vehicleId] takes with it: the rows Delete vehicle counts and every encrypted file the
     * vehicle's rows point at (`DocumentDao.encryptedFiles`, the query the backup and the orphan sweep go by).
     * Null if the vehicle doesn't exist.
     */
    suspend fun whatGoes(vehicleId: Long): VehicleDeletion? {
        val vehicle = db.vehicles().byId(vehicleId) ?: return null
        return VehicleDeletion(vehicle, db.vehicles().rowCounts(vehicleId), db.documents().encryptedFiles(vehicleId))
    }

    /**
     * Deletes the vehicle (ADR-0003): the row, which cascades to everything that names it, then the files
     * [whatGoes] found, read in the same transaction so none is missed. Returns what went, or null if the
     * vehicle was already gone.
     */
    suspend fun deleteVehicle(vehicleId: Long): VehicleDeletion? {
        val gone = db.useWriterConnection { connection ->
            connection.immediateTransaction<VehicleDeletion?> { whatGoes(vehicleId)?.also { db.vehicles().delete(it.vehicle) } }
        } ?: return null
        photos.delete(gone.files)
        return gone
    }

    /**
     * Deletes a vehicle's service records, or only the imported ones when [all] is false, with their
     * photos. Everything else about the vehicle stays: its details, what it takes, torque specs,
     * schedule, recalls, milestones and mileage updates. Reminders count from whatever is left.
     */
    suspend fun clearHistory(vehicleId: Long, all: Boolean): Int {
        val service = db.service()
        val files = service.photoFilesOf(vehicleId, all)
        val deleted = service.clearRecords(vehicleId, all)
        photos.delete(files)
        return deleted
    }

    /**
     * Crypto-erase: destroy the keys first so leftovers are unreadable, then delete files.
     * The caller must restart the app process afterwards (e.g. relaunch to the welcome screen).
     */
    fun deleteEverything() {
        // Written first, so a wipe cut short is finished on the next launch instead of leaving a
        // database behind whose key is gone, which the app could never open again.
        marker(context).createNewFile()
        runCatching { db.close() }
        erase(context, keys, photos)
    }

    companion object {
        private fun marker(context: Context) = File(context.noBackupFilesDir, "wipe.pending")

        /**
         * Finishes a wipe that the process didn't survive. Call it first thing at process start,
         * before anything opens the database or reads the settings.
         */
        fun finishInterrupted(context: Context) {
            if (marker(context).exists()) erase(context, DatabaseKeyManager(context), PhotoCrypto(context))
        }

        private fun erase(context: Context, keys: DatabaseKeyManager, photos: PhotoCrypto) {
            keys.destroy()
            photos.destroy()
            context.deleteDatabase(GloveboxDatabase.FILE_NAME)
            // The empty lock file Room keeps beside the database once it opens through a driver (#220).
            File(context.getDatabasePath(GloveboxDatabase.FILE_NAME).path + ".lck").delete()
            photos.restoreDir.deleteRecursively()
            // Settings and the notification log.
            File(context.filesDir, "datastore").deleteRecursively()
            // A debug build's Read an invoice traces (#484): a real invoice's words, in the clear.
            File(context.filesDir, "invoice-traces").deleteRecursively()
            // Exports, a picked backup file, and photos on their way in or out.
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            marker(context).delete()
        }
    }
}

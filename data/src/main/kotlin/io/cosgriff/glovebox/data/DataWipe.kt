package io.cosgriff.glovebox.data

import android.content.Context
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.VehicleEntity
import java.io.File

/** Implements the two destructive flows from the design: delete one vehicle, delete everything. */
class DataWipe(
    private val context: Context,
    private val db: GloveboxDatabase,
    private val keys: DatabaseKeyManager,
    private val photos: PhotoCrypto,
) {
    /** Removes the vehicle row (cascading to all its data) and its encrypted photo and document files. */
    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        val files = db.vehicles().photoFilesFor(vehicle.id) + listOfNotNull(vehicle.photoFile) + db.documents().documentsFor(vehicle.id).map { it.fileName }
        db.vehicles().delete(vehicle)
        files.forEach { File(photos.photoDir, it).delete() }
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
        files.forEach { File(photos.photoDir, it).delete() }
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
            // Exports, a picked backup file, and photos on their way in or out.
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            marker(context).delete()
        }
    }
}

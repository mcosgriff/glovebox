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
    /** Removes the vehicle row (cascading to all its data) and its encrypted photo files. */
    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        val files = db.vehicles().photoFilesFor(vehicle.id) + listOfNotNull(vehicle.photoFile)
        db.vehicles().delete(vehicle)
        files.forEach { File(photos.photoDir, it).delete() }
    }

    /**
     * Crypto-erase: destroy the keys first so leftovers are unreadable, then delete files.
     * The caller must restart the app process afterwards (e.g. relaunch to the welcome screen).
     */
    fun deleteEverything() {
        db.close()
        keys.destroy()
        photos.destroy()
        context.deleteDatabase(GloveboxDatabase.FILE_NAME)
        File(context.filesDir, "datastore").deleteRecursively()
    }
}

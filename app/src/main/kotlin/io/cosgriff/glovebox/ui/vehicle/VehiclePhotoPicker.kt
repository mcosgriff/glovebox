package io.cosgriff.glovebox.ui.vehicle

import android.net.Uri
import io.cosgriff.glovebox.data.photo.VehiclePhotoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks a vehicle's photo with the system photo picker, which needs no storage permission.
 *
 * Lives outside the screens on purpose: leaving for the picker can lock the app, and the lock
 * screen replaces the whole UI, so a launcher owned by Vehicle details would never hear back.
 * The activity owns the launcher and this remembers which vehicle the answer is for.
 */
@Singleton
class VehiclePhotoPicker @Inject constructor(private val photos: VehiclePhotoStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var launcher: (() -> Unit)? = null
    private var vehicleId: Long? = null

    private val _failedFor = MutableStateFlow<Long?>(null)

    /** The vehicle whose last picked image couldn't be read; cleared by the next attempt. */
    val failedFor: StateFlow<Long?> = _failedFor.asStateFlow()

    fun attach(launch: () -> Unit) {
        launcher = launch
    }

    /** Only [launch]'s own activity lets go, in case a new one attached first. */
    fun detach(launch: () -> Unit) {
        if (launcher === launch) launcher = null
    }

    fun pick(forVehicleId: Long) {
        val launch = launcher ?: return
        vehicleId = forVehicleId
        _failedFor.value = null
        launch()
    }

    /** [uri] is null when the user backed out of the picker. */
    fun onPicked(uri: Uri?) {
        val id = vehicleId ?: return
        vehicleId = null
        if (uri == null) return
        scope.launch { if (!photos.set(id, uri)) _failedFor.value = id }
    }
}

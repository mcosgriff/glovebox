package io.cosgriff.glovebox.ui.photo

import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.RecordPhotos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Where newly attached photos go. */
sealed interface PhotoTarget {
    /** A Log service form that hasn't been saved; [key] is the form's own. */
    data class Draft(val key: String) : PhotoTarget

    /** A record that already exists: the photo is added to it at once. */
    data class Record(val recordId: Long) : PhotoTarget
}

/**
 * Takes or picks receipt and parts photos and encrypts them.
 *
 * Lives outside the screens for the same reason as [io.cosgriff.glovebox.ui.vehicle.VehiclePhotoPicker]:
 * leaving for the camera or the picker can lock the app, and the lock screen replaces the whole UI.
 * The activity owns the launchers; this remembers where the answer goes and holds a form's photos
 * until the form is saved or abandoned.
 */
@Singleton
class RecordPhotoPicker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: RecordPhotoStore,
    private val service: ServiceDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val picker = LauncherSlot<() -> Unit>()
    private val camera = LauncherSlot<(Uri) -> Unit>()

    private var target: PhotoTarget? = null
    private var room = 0
    private var captureFile: File? = null

    private val _drafts = MutableStateFlow(emptyMap<String, List<String>>())

    /** File names attached to each unsaved form, by its key. */
    val drafts: StateFlow<Map<String, List<String>>> = _drafts.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)

    /** Why the last attempt added less than was asked for; cleared by the next attempt. */
    val problem: StateFlow<String?> = _problem.asStateFlow()

    fun attach(picker: () -> Unit, camera: (Uri) -> Unit) {
        this.picker.attach(picker)
        this.camera.attach(camera)
    }

    /** Only [picker]'s own activity lets go, in case a new one attached first. */
    fun detach(picker: () -> Unit) {
        // The camera's launcher came with the picker's, from the same activity.
        if (this.picker.detach(picker)) camera.launcher?.let(camera::detach)
    }

    /** [room] is how many more photos the form or record can take. */
    fun pick(to: PhotoTarget, room: Int) {
        val launch = picker.launcher ?: return
        if (!begin(to, room)) return
        launch()
    }

    /** The caller must already hold the camera permission: the app declares it, so Android insists. */
    fun capture(to: PhotoTarget, room: Int) {
        val launch = camera.launcher ?: return
        if (!begin(to, room)) return
        val file = File(store.captureDir, "${UUID.randomUUID()}.jpg")
        captureFile = file
        launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
    }

    private fun begin(to: PhotoTarget, room: Int): Boolean {
        _problem.value = null
        if (room <= 0) {
            _problem.value = AppText.res.getString(R.string.photos_full, RecordPhotos.MAX_PER_RECORD.asNumber())
            return false
        }
        target = to
        this.room = room
        return true
    }

    /** Empty when the user backed out of the picker. */
    fun onPicked(uris: List<Uri>) {
        val to = target ?: return
        target = null
        if (uris.isEmpty()) return
        val wanted = uris.take(room)
        scope.launch {
            val names = wanted.mapNotNull { store.import(it) }
            deliver(to, names)
            _problem.value = when {
                names.size < wanted.size -> AppText.res.getString(R.string.photos_unreadable, (wanted.size - names.size).asNumber())
                uris.size > wanted.size -> AppText.res.getString(R.string.photos_too_many, wanted.size.asNumber(), RecordPhotos.MAX_PER_RECORD.asNumber())
                else -> null
            }
        }
    }

    fun onCaptured(saved: Boolean) {
        val to = target
        val file = captureFile
        target = null
        captureFile = null
        if (to == null || file == null) return
        scope.launch {
            val name = if (saved) store.import(Uri.fromFile(file)) else null
            // The camera's copy is in the clear; it goes whether or not it could be used.
            file.delete()
            if (name != null) deliver(to, listOf(name)) else if (saved) _problem.value = AppText.res.getString(R.string.photo_unreadable)
        }
    }

    private suspend fun deliver(to: PhotoTarget, names: List<String>) {
        if (names.isEmpty()) return
        when (to) {
            is PhotoTarget.Draft -> _drafts.update { it + (to.key to it[to.key].orEmpty() + names) }
            is PhotoTarget.Record -> {
                // The record may have been deleted, or filled up, while the camera was open.
                val fits = if (service.record(to.recordId) == null) 0 else RecordPhotos.room(service.photoCount(to.recordId))
                service.insertPhotos(names.take(fits).map { PhotoEntity(recordId = to.recordId, fileName = it) })
                store.delete(names.drop(fits))
            }
        }
    }

    /** Takes one photo off an unsaved form and deletes its file. */
    fun removeDraft(key: String, fileName: String) {
        _drafts.update { it + (key to it[key].orEmpty() - fileName) }
        scope.launch { store.delete(listOf(fileName)) }
    }

    /** The form was saved: hands over its photos, whose files now belong to the record. */
    fun takeDraft(key: String): List<String> {
        val names = _drafts.value[key].orEmpty()
        _drafts.update { it - key }
        return names
    }

    /** The form was abandoned: its photos go. Does nothing after [takeDraft]. */
    fun discardDraft(key: String) {
        val names = takeDraft(key)
        if (names.isNotEmpty()) scope.launch { store.delete(names) }
    }
}

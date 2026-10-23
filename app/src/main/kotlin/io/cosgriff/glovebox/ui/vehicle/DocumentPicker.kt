package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.newCapture
import io.cosgriff.glovebox.data.displayName
import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.DocumentProblem
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.data.photo.AddedDocument
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A document just added, for the Documents screen to open its details on. */
data class NewDocument(val vehicleId: Long, val id: Long)

/**
 * Picks a PDF or a picture, or takes a photo, and files it as one of a vehicle's documents.
 *
 * Lives outside the screens for the same reason as [RecordPhotoPicker][io.cosgriff.glovebox.ui.photo.RecordPhotoPicker]:
 * leaving for the file picker or the camera can lock the app, and the lock screen replaces the whole UI.
 * The activity owns the launchers; this remembers which vehicle the answer is for.
 */
@Singleton
class DocumentPicker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: DocumentStore,
    private val photos: RecordPhotoStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val picker = LauncherSlot<() -> Unit>()
    private val camera = LauncherSlot<(Uri) -> Unit>()
    private var vehicleId: Long? = null
    private var captureFile: File? = null

    private val _added = MutableStateFlow<NewDocument?>(null)

    /** The last document added, until the screen has opened it ([consumeAdded]). */
    val added: StateFlow<NewDocument?> = _added.asStateFlow()

    private val _problem = MutableStateFlow<DocumentProblem?>(null)

    /** Why the last attempt added nothing; cleared by the next attempt. */
    val problem: StateFlow<DocumentProblem?> = _problem.asStateFlow()

    private val _working = MutableStateFlow(false)

    /** A file is being read and encrypted. */
    val working: StateFlow<Boolean> = _working.asStateFlow()

    fun attach(picker: () -> Unit, camera: (Uri) -> Unit) {
        this.picker.attach(picker)
        this.camera.attach(camera)
    }

    /** Only [picker]'s own activity lets go, in case a new one attached first. */
    fun detach(picker: () -> Unit) {
        // The camera's launcher came with the picker's, from the same activity.
        if (this.picker.detach(picker)) camera.launcher?.let(camera::detach)
    }

    fun pick(vehicleId: Long) {
        val launch = picker.launcher ?: return
        _problem.value = null
        this.vehicleId = vehicleId
        launch()
    }

    /** The caller must already hold the camera permission: the app declares it, so Android insists. */
    fun capture(vehicleId: Long) {
        val launch = camera.launcher ?: return
        _problem.value = null
        this.vehicleId = vehicleId
        val (file, uri) = context.newCapture(photos.captureDir)
        captureFile = file
        launch(uri)
    }

    /** Null when the user backed out of the picker. */
    fun onPicked(uri: Uri?) {
        val to = vehicleId ?: return
        vehicleId = null
        if (uri == null) return
        scope.launch { add(to, uri, context.contentResolver.displayName(uri)) }
    }

    fun onCaptured(saved: Boolean) {
        val to = vehicleId
        val file = captureFile
        vehicleId = null
        captureFile = null
        if (to == null || file == null) return
        scope.launch {
            if (saved) add(to, Uri.fromFile(file), null)
            // The camera's copy is in the clear; it goes whether or not it could be used.
            file.delete()
        }
    }

    private suspend fun add(vehicleId: Long, uri: Uri, name: String?) {
        _working.value = true
        val result = try {
            store.add(vehicleId, uri, name, DocumentType.OTHER.label, LocalDate.now())
        } catch (_: Exception) {
            // The vehicle was deleted while the picker was open.
            AddedDocument.Refused(DocumentProblem.FAILED)
        } finally {
            _working.value = false
        }
        when (result) {
            is AddedDocument.Added -> _added.value = NewDocument(vehicleId, result.id)
            is AddedDocument.Refused -> _problem.value = result.problem
        }
    }

    fun consumeAdded() {
        _added.value = null
    }

    fun clearProblem() {
        _problem.value = null
    }
}

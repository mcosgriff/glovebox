package io.cosgriff.glovebox.ui.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.PageLight
import io.cosgriff.glovebox.domain.PhotoPages
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ui.newCapture
import io.cosgriff.glovebox.ui.scan.TextReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A page made ready for Tesseract: greyscale, [width] x [height], one byte per pixel. */
private class PageImage(val luminance: ByteArray, val width: Int, val height: Int)

/**
 * Photographs or picks the pages of a paper invoice for Log service (#513) and reads each one as it comes, entirely
 * on the phone. A singleton with its launchers in the activity, like [io.cosgriff.glovebox.ui.photo.RecordPhotoPicker]:
 * leaving for the camera or the gallery can lock the app.
 *
 * Each page is encrypted the moment it arrives, as a record photo; the camera's copy is in the clear only in
 * `cache/capture/` until then. What is read is held in memory only. Pages are read one at a time, in the order they
 * came, since a page takes seconds (#482 guessed 5–8 on a phone).
 */
@Singleton
class InvoicePhotos @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: RecordPhotoStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val picker = LauncherSlot<() -> Unit>()
    private val camera = LauncherSlot<(Uri) -> Unit>()

    /** The form the next pages go to, and the page they take the place of when retaken. */
    private var target: String? = null
    private var retaking: String? = null
    private var room = 0
    private var captureFile: File? = null

    private val _pages = MutableStateFlow(emptyMap<String, PhotoPages>())

    /** Each open form's pages, by its key. */
    val pages: StateFlow<Map<String, PhotoPages>> = _pages.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)

    /** Why the last pages added fewer than were asked for; cleared by the next attempt. */
    val problem: StateFlow<String?> = _problem.asStateFlow()

    /** One page read at a time, and one engine for them all while there are pages to read. */
    private val reading = Mutex()
    private var reader: TextReader? = null
    private var waiting = 0

    fun attach(picker: () -> Unit, camera: (Uri) -> Unit) {
        this.picker.attach(picker)
        this.camera.attach(camera)
    }

    /** Only [picker]'s own activity lets go, in case a new one attached first. */
    fun detach(picker: () -> Unit) {
        if (this.picker.detach(picker)) camera.launcher?.let(camera::detach)
    }

    /** The caller must already hold the camera permission. */
    fun capture(key: String) {
        val launch = camera.launcher ?: return
        if (!begin(key, null)) return
        val (file, uri) = context.newCapture(store.captureDir)
        captureFile = file
        launch(uri)
    }

    /** [file] photographed again: the new photo takes its place. */
    fun retake(key: String, file: String) {
        val launch = camera.launcher ?: return
        if (!begin(key, file)) return
        val (capture, uri) = context.newCapture(store.captureDir)
        captureFile = capture
        launch(uri)
    }

    fun pick(key: String) {
        val launch = picker.launcher ?: return
        if (!begin(key, null)) return
        launch()
    }

    private fun begin(key: String, retake: String?): Boolean {
        _problem.value = null
        // A retake replaces a page, so it needs no room.
        room = if (retake != null) 1 else RecordPhotos.room(pagesOf(key).pages.size)
        if (room <= 0) {
            _problem.value = AppText.res.getString(R.string.invoice_pages_full, RecordPhotos.MAX_PER_RECORD.asNumber())
            return false
        }
        target = key
        retaking = retake
        return true
    }

    fun onCaptured(saved: Boolean) {
        val key = target
        val file = captureFile
        val retake = retaking
        target = null
        retaking = null
        captureFile = null
        if (key == null || file == null) return
        scope.launch {
            var image: PageImage? = null
            val name = if (saved) {
                // Read into memory before the camera's copy goes: it is the only full-size one.
                image = decode(Uri.fromFile(file))
                store.import(Uri.fromFile(file))
            } else {
                null
            }
            file.delete()
            if (name == null || image == null) {
                name?.let { store.delete(listOf(it)) }
                if (saved) _problem.value = AppText.res.getString(R.string.photo_unreadable)
                return@launch
            }
            if (arrived(key, name, retake)) readPage(key, name) { image }
        }
    }

    /** Empty when the user backed out of the gallery. */
    fun onPicked(uris: List<Uri>) {
        val key = target ?: return
        target = null
        if (uris.isEmpty()) return
        val wanted = uris.take(room)
        scope.launch {
            var unreadable = 0
            for (uri in wanted) {
                val name = store.import(uri)
                if (name == null) {
                    unreadable++
                    continue
                }
                // The gallery keeps its copy, so it is read from there when its turn comes, not held in memory.
                if (arrived(key, name, null)) launch { readPage(key, name) { decode(uri) } }
            }
            _problem.value = when {
                unreadable > 0 -> AppText.res.getString(R.string.photos_unreadable, unreadable.asNumber())
                uris.size > wanted.size -> AppText.res.getString(R.string.invoice_pages_too_many, wanted.size.asNumber(), RecordPhotos.MAX_PER_RECORD.asNumber())
                else -> null
            }
        }
    }

    /**
     * Page [name] goes on form [key]'s strip: in [retake]'s place, or at the end if there is none (or it was taken off
     * while the camera was open). False, and the file deleted, when the form has gone.
     */
    private suspend fun arrived(key: String, name: String, retake: String?): Boolean {
        val pages = _pages.value[key]
        if (pages == null) {
            store.delete(listOf(name))
            return false
        }
        val replaces = retake?.takeIf { r -> pages.pages.any { it.file == r } }
        _pages.update { it + (key to if (replaces != null) pages.retake(replaces, name) else pages.add(name)) }
        replaces?.let { store.delete(listOf(it)) }
        return true
    }

    /** Reads page [name] of form [key] when its turn comes, unless it has been taken off or retaken by then. */
    private suspend fun readPage(key: String, name: String, image: suspend () -> PageImage?) {
        waiting++
        try {
            reading.withLock {
                if (!has(key, name)) return
                val page = image()
                val started = SystemClock.elapsedRealtime()
                val words = page?.let { p ->
                    withContext(Dispatchers.Default) {
                        val engine = reader ?: TextReader.page(context).also { reader = it }
                        engine.boxedWords(PageLight.flatten(p.luminance, p.width, p.height), p.width, p.height)
                    }
                }.orEmpty()
                val millis = SystemClock.elapsedRealtime() - started
                _pages.update { all -> all[key]?.let { all + (key to it.read(name, words, millis)) } ?: all }
            }
        } finally {
            // The engine holds tens of megabytes; it goes once nothing is left to read.
            if (--waiting == 0) reading.withLock { if (waiting == 0) reader?.close().also { reader = null } }
        }
    }

    private fun has(key: String, name: String) = _pages.value[key]?.pages?.any { it.file == name } == true

    private fun pagesOf(key: String) = _pages.value[key] ?: PhotoPages()

    /** A form begins taking pages: until then, pages arriving for it go nowhere. */
    fun open(key: String) {
        _pages.update { if (key in it) it else it + (key to PhotoPages()) }
    }

    fun move(key: String, file: String, by: Int) = _pages.update { all -> all[key]?.let { all + (key to it.move(file, by)) } ?: all }

    fun remove(key: String, file: String) {
        _pages.update { all -> all[key]?.let { all + (key to it.remove(file)) } ?: all }
        scope.launch { store.delete(listOf(file)) }
    }

    /** The form was saved: its pages' files, in order, now the caller's to keep or delete. */
    fun take(key: String): List<String> {
        val files = _pages.value[key]?.pages.orEmpty().map { it.file }
        _pages.update { it - key }
        return files
    }

    /** Pages taken from the form with [take] and not kept. */
    fun drop(files: List<String>) {
        if (files.isNotEmpty()) scope.launch { store.delete(files) }
    }

    /** The pages are put aside, or the form was left: their files go, and pages arriving later go nowhere. Does nothing after [take]. */
    fun discard(key: String) {
        _problem.value = null
        drop(take(key))
    }

    /** The page at [uri] [PageLight.READ_WIDTH] wide in greyscale, or null if it can't be decoded. */
    private suspend fun decode(uri: Uri): PageImage? = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = PageLight.readSize(info.size.width, info.size.height)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                // 16 bits a pixel is plenty for a page about to be made grey, and halves a 3400 px page's memory.
                decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
            }
            // Turned upright from the photo's own orientation, it may be the other way round from what was asked.
            val (w, h) = PageLight.readSize(decoded.width, decoded.height)
            val bitmap = if (decoded.width == w) decoded else decoded.scale(w, h).also { decoded.recycle() }
            try {
                PageImage(grey(bitmap), bitmap.width, bitmap.height)
            } finally {
                bitmap.recycle()
            }
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /** The bitmap's brightness, a row at a time, so the full-colour pixels are never all held at once. */
    private fun grey(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val out = ByteArray(width * bitmap.height)
        val row = IntArray(width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                val c = row[x]
                // Rec. 601 luma, as the camera's own greyscale plane is.
                out[y * width + x] = ((299 * (c shr 16 and 0xFF) + 587 * (c shr 8 and 0xFF) + 114 * (c and 0xFF)) / 1000).toByte()
            }
        }
        return out
    }
}

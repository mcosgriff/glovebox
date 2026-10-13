package io.cosgriff.glovebox.ui.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.ImportFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands a finished export or backup to Android's "save as" screen and copies it to wherever the user picks.
 * The app itself needs no storage permission.
 *
 * A singleton, with its launchers in the activity, for the same reason as the photo pickers:
 * choosing where to save leaves the app, which can lock it, and the answer has to land somewhere
 * that outlives the screen that asked.
 */
@Singleton
class ExportSaver @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var launch: ((mimeType: String, fileName: String) -> Unit)? = null
    private var pending: Pending? = null

    private class Pending(val file: File, val name: String, val done: String, val onSaved: suspend () -> Unit)

    private val _results = Channel<String>(Channel.BUFFERED)

    /** What to tell the user once a save has finished or failed. */
    val results = _results.receiveAsFlow()

    /** Exports are written here, in the clear, only until they have been copied out. */
    private val dir: File get() = File(context.cacheDir, "export").apply { mkdirs() }

    fun attach(launcher: (mimeType: String, fileName: String) -> Unit) {
        launch = launcher
    }

    fun detach(launcher: (mimeType: String, fileName: String) -> Unit) {
        if (launch === launcher) launch = null
    }

    /** An empty file to write the export into; anything left from an earlier export is removed first. */
    fun newFile(): File {
        clear()
        return File(dir, "export.tmp")
    }

    /** Asks where to put [file], suggesting [fileName]. */
    fun save(file: File, fileName: String, format: ExportFormat) = save(file, fileName, format.mimeType, "Exported $fileName") {}

    /**
     * Asks where to put [file], suggesting [fileName]. [onSaved] runs only once the copy has
     * succeeded, and [done] is then shown to the user.
     */
    fun save(file: File, fileName: String, mimeType: String, done: String, onSaved: suspend () -> Unit) {
        val launcher = launch
        if (launcher == null) {
            file.delete()
            return
        }
        pending = Pending(file, fileName, done, onSaved)
        launcher(mimeType, fileName)
    }

    /**
     * Writes [text] as an import file (`ImportFiles` in :domain) and asks where to put it. Like every
     * export it is in the clear, and the working copy goes once it is copied out or the user backs out.
     */
    suspend fun saveImportFile(text: String, fileName: String) {
        val file = newFile()
        withContext(Dispatchers.IO) { file.writeText(text) }
        save(file, fileName, ImportFiles.MIME_TYPE, "Saved $fileName") {}
    }

    /** [destination] is null when the user backed out. Either way the working copy goes. */
    fun onDestination(destination: Uri?) {
        val p = pending ?: return
        pending = null
        scope.launch {
            val saved = destination != null && withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(destination)?.use { out -> p.file.inputStream().use { it.copyTo(out) } } != null
                } catch (_: IOException) {
                    false
                } finally {
                    p.file.delete()
                }
            }
            when {
                destination == null -> p.file.delete()
                saved -> {
                    p.onSaved()
                    _results.send(p.done)
                }
                else -> _results.send("Couldn't save ${p.name}")
            }
        }
    }

    /** Run on launch: an export interrupted before it was copied out leaves nothing behind. */
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }
}

/** Lets the nav host announce a finished export. */
@dagger.hilt.android.lifecycle.HiltViewModel
class ExportResultsViewModel @Inject constructor(saver: ExportSaver) : androidx.lifecycle.ViewModel() {
    val results = saver.results
}

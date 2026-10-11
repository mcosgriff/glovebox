package io.cosgriff.glovebox.ui.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.domain.ExportFormat
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
 * Hands a finished export to Android's "save as" screen and copies it to wherever the user picks.
 * The app itself needs no storage permission.
 *
 * A singleton, with its launchers in the activity, for the same reason as the photo pickers:
 * choosing where to save leaves the app, which can lock it, and the answer has to land somewhere
 * that outlives the screen that asked.
 */
@Singleton
class ExportSaver @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var launch: ((format: ExportFormat, fileName: String) -> Unit)? = null
    private var pending: Pair<File, String>? = null

    private val _results = Channel<String>(Channel.BUFFERED)

    /** What to tell the user once a save has finished or failed. */
    val results = _results.receiveAsFlow()

    /** Exports are written here, in the clear, only until they have been copied out. */
    private val dir: File get() = File(context.cacheDir, "export").apply { mkdirs() }

    fun attach(launcher: (ExportFormat, String) -> Unit) {
        launch = launcher
    }

    fun detach(launcher: (ExportFormat, String) -> Unit) {
        if (launch === launcher) launch = null
    }

    /** An empty file to write the export into; anything left from an earlier export is removed first. */
    fun newFile(): File {
        clear()
        return File(dir, "export.tmp")
    }

    /** Asks where to put [file], suggesting [fileName]. */
    fun save(file: File, fileName: String, format: ExportFormat) {
        val launcher = launch
        if (launcher == null) {
            file.delete()
            return
        }
        pending = file to fileName
        launcher(format, fileName)
    }

    /** [destination] is null when the user backed out. Either way the working copy goes. */
    fun onDestination(destination: Uri?) {
        val (file, name) = pending ?: return
        pending = null
        scope.launch {
            val saved = destination != null && withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(destination)?.use { out -> file.inputStream().use { it.copyTo(out) } } != null
                } catch (_: IOException) {
                    false
                } finally {
                    file.delete()
                }
            }
            if (destination == null) file.delete() else _results.send(if (saved) "Exported $name" else "Couldn't save the export")
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

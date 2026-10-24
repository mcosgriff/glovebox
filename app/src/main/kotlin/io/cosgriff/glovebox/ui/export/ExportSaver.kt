package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.ImportFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
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
class ExportSaver @Inject constructor(@ApplicationContext private val context: Context, @ApplicationScope private val scope: CoroutineScope) {
    private val slot = LauncherSlot<(mimeType: String, fileName: String) -> Unit>()
    private var pending: Pending? = null

    /** [write] fills the chosen document; [cleanUp] runs however it ends. */
    private class Pending(
        val name: String,
        val done: String,
        val write: suspend (OutputStream) -> Unit,
        val cleanUp: () -> Unit,
        val onSaved: suspend () -> Unit,
    )

    private val _results = Channel<String>(Channel.BUFFERED)

    /** What to tell the user once a save has finished or failed. */
    val results = _results.receiveAsFlow()

    /** Exports are written here, in the clear, only until they have been copied out. */
    private val dir: File get() = File(context.cacheDir, "export").apply { mkdirs() }

    fun attach(launcher: (mimeType: String, fileName: String) -> Unit) {
        slot.attach(launcher)
    }

    fun detach(launcher: (mimeType: String, fileName: String) -> Unit) {
        slot.detach(launcher)
    }

    /** An empty file to write the export into; anything left from an earlier export is removed first. */
    fun newFile(): File {
        clear()
        return File(dir, "export.tmp")
    }

    /**
     * Asks where to save an export, suggesting [fileName], and only then has [write] put it straight into the
     * document Android hands back: nothing is written in the clear inside the app, not even for a moment.
     */
    fun saveExport(fileName: String, format: ExportFormat, write: suspend (OutputStream) -> Unit) {
        val launcher = slot.launcher ?: return
        pending = Pending(fileName, AppText.res.getString(R.string.export_saved, fileName), write, cleanUp = {}, onSaved = {})
        launcher(format.mimeType, fileName)
    }

    /**
     * Asks where to put [file], suggesting [fileName]. [onSaved] runs only once the copy has
     * succeeded, and [done] is then shown to the user.
     */
    fun save(file: File, fileName: String, mimeType: String, done: String, onSaved: suspend () -> Unit) {
        val launcher = slot.launcher
        if (launcher == null) {
            file.delete()
            return
        }
        pending = Pending(fileName, done, write = { out -> file.inputStream().use { it.copyTo(out) } }, cleanUp = { file.delete() }, onSaved = onSaved)
        launcher(mimeType, fileName)
    }

    /**
     * Writes [text] as an import file (`ImportFiles` in :domain) and asks where to put it. Like every
     * export it is in the clear, and the working copy goes once it is copied out or the user backs out.
     */
    suspend fun saveImportFile(text: String, fileName: String) {
        val file = newFile()
        withContext(Dispatchers.IO) { file.writeText(text) }
        save(file, fileName, ImportFiles.MIME_TYPE, AppText.res.getString(R.string.export_saved_file, fileName)) {}
    }

    /** [destination] is null when the user backed out. Either way the working copy, if there is one, goes. */
    fun onDestination(destination: Uri?) {
        val p = pending ?: return
        pending = null
        if (destination == null) {
            p.cleanUp()
            return
        }
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(destination)?.use { p.write(it) } != null
                } catch (_: IOException) {
                    false
                } catch (_: RuntimeException) {
                    // A page that couldn't be drawn: the save fails and says so, rather than taking the app down.
                    false
                } finally {
                    p.cleanUp()
                }
            }
            if (saved) {
                p.onSaved()
                _results.send(p.done)
            } else {
                // Don't leave a half-written file behind where the user chose to save it.
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, destination) }
                _results.send(AppText.res.getString(R.string.export_couldnt_save, p.name))
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

/**
 * Android's "save as" with the file's type given on each call. `CreateDocument` fixes the type when its
 * launcher is made, so one launcher per type was registered and any other type took the backup's.
 */
class SaveAs : ActivityResultContract<SaveAs.Request, Uri?>() {
    data class Request(val mimeType: String, val fileName: String)

    override fun createIntent(context: Context, input: Request): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).setType(input.mimeType).putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}

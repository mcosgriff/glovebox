package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.data.readCapped
import io.cosgriff.glovebox.data.displayName
import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.torque.text
import io.cosgriff.glovebox.ui.schedule.text
import java.time.LocalDate
import io.cosgriff.glovebox.domain.CarfaxReport
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportFormat
import io.cosgriff.glovebox.ui.service.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** An import file the user picked: its name and what it says. Held in memory only, never written anywhere. */
/**
 * [file] is set when the pick was read already, as a Carfax report PDF is; otherwise [text] is parsed as an import file.
 * [pdf] is the report itself, held in memory only so that it can be kept in the vehicle's documents if the user asks.
 */
data class PickedImport(val name: String, val text: String, val file: ImportFile? = null, val pdf: ByteArray? = null)

/**
 * Lets the user pick an import file with Android's file picker. A singleton with its launcher in
 * the activity, like the other pickers: choosing a file leaves the app, which can lock it.
 */
@Singleton
class ImportPicker @Inject constructor(@ApplicationContext private val context: Context, @ApplicationScope private val scope: CoroutineScope) {
    private val slot = LauncherSlot<() -> Unit>()

    private val _picked = MutableStateFlow<PickedImport?>(null)
    val picked: StateFlow<PickedImport?> = _picked.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    fun attach(launcher: () -> Unit) {
        slot.attach(launcher)
    }

    fun detach(launcher: () -> Unit) {
        slot.detach(launcher)
    }

    fun pick() {
        _problem.value = null
        slot.launcher?.invoke()
    }

    /** Null when the user backed out. */
    fun onPicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            // A PDF is read as a Carfax report: its service visits become a service-history import.
            val bytes = withContext(Dispatchers.IO) { context.contentResolver.readCapped(uri, PDF_MAX_BYTES.toLong()) }
            if (bytes != null && bytes.size >= 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF") {
                if (bytes.size > PDF_MAX_BYTES) {
                    _problem.value = AppText.res.getString(R.string.import_pdf_too_big)
                    return@launch
                }
                val file = withContext(Dispatchers.Default) {
                    PdfWords.read(context, bytes)?.let { CarfaxReport.read(it, LocalDate.now()) }
                }
                if (file == null) _problem.value = AppText.res.getString(R.string.import_pdf_unreadable)
                else _picked.value = PickedImport(context.contentResolver.displayName(uri) ?: AppText.res.getString(R.string.import_carfax_report), "", file, pdf = bytes)
                return@launch
            }
            val text = withContext(Dispatchers.IO) { read(uri) }
            when (text) {
                null -> _problem.value = AppText.res.getString(R.string.document_problem_unreadable)
                TOO_BIG -> _problem.value = AppText.res.getString(R.string.import_too_big)
                else -> _picked.value = PickedImport(context.contentResolver.displayName(uri) ?: AppText.res.getString(R.string.import_file), text)
            }
        }
    }

    fun clear() {
        _picked.value = null
        _problem.value = null
    }

    /** Stops reading as soon as the file is past the size cap, so a huge file is never held in memory. */
    private fun read(uri: Uri): String? = context.contentResolver.readCapped(uri, ImportFormat.MAX_BYTES.toLong())
        ?.let { if (it.size > ImportFormat.MAX_BYTES) TOO_BIG else it.toString(Charsets.UTF_8) }

    private companion object {
        // Not valid JSON, so it can never be mistaken for a file's contents.
        const val TOO_BIG = "\u0000too-big"

        /** A Carfax report is a few MB at most; this leaves room without holding something huge in memory. */
        const val PDF_MAX_BYTES = 20 * 1024 * 1024
    }
}

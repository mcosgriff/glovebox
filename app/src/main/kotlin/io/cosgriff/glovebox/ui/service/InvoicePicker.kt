package io.cosgriff.glovebox.ui.service

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.BuildConfig
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.data.readCapped
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.InvoiceReading
import io.cosgriff.glovebox.domain.InvoiceWord
import io.cosgriff.glovebox.domain.InvoiceWords
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ui.settings.PdfWords
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** What came back from the picker for the form [key]: the PDF's bytes, or why there are none. */
sealed interface PickedInvoice {
    val key: String

    class Pdf(override val key: String, val bytes: ByteArray) : PickedInvoice

    /** Not a PDF, too big to keep, or it couldn't be read from where it was picked. */
    data class Refused(override val key: String) : PickedInvoice
}

/**
 * Lets Log service pick a shop's emailed invoice (#484) with Android's file picker. A singleton with its launcher in
 * the activity, like the other pickers: choosing a file leaves the app, which can lock it. The PDF is read into
 * memory and never written anywhere unless the user keeps it, encrypted, in the vehicle's documents.
 */
@Singleton
class InvoicePicker @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val documents: DocumentStore,
) {
    private val slot = LauncherSlot<() -> Unit>()
    private var target: String? = null

    private val _picked = MutableStateFlow<PickedInvoice?>(null)
    val picked: StateFlow<PickedInvoice?> = _picked.asStateFlow()

    fun attach(launcher: () -> Unit) = slot.attach(launcher)

    fun detach(launcher: () -> Unit) {
        slot.detach(launcher)
    }

    /** [key] names the form the PDF goes to. */
    fun pick(key: String) {
        val launch = slot.launcher ?: return
        target = key
        launch()
    }

    /** Null when the user backed out. */
    fun onPicked(uri: Uri?) {
        val key = target ?: return
        target = null
        if (uri == null) return
        scope.launch {
            // Kept as a document if the user asks, so no bigger than one may be.
            val bytes = withContext(Dispatchers.IO) { context.contentResolver.readCapped(uri, VehicleDocuments.MAX_BYTES) }
            _picked.value = if (bytes != null && bytes.size <= VehicleDocuments.MAX_BYTES && VehicleDocuments.isPdf(bytes)) PickedInvoice.Pdf(key, bytes) else PickedInvoice.Refused(key)
        }
    }

    /** The form [key] took what was picked for it. */
    fun take(key: String): PickedInvoice? = _picked.value?.takeIf { it.key == key }?.also { _picked.value = null }

    /** The PDF's words with their boxes, off the main thread; null if it can't be read, empty for a scan. */
    suspend fun words(bytes: ByteArray): List<InvoiceWord>? = withContext(Dispatchers.Default) { PdfWords.readInvoice(context, bytes) }

    /** The invoice kept, encrypted, among the vehicle's documents as the user asked on saving. */
    suspend fun keep(vehicleId: Long, bytes: ByteArray, name: String, date: LocalDate?) {
        documents.addPdf(vehicleId, bytes, DocumentType.OTHER, name, date, LocalDate.now())
    }

    /**
     * Debug builds only: the words, their boxes and the reader's trace as JSON in `files/invoice-traces/`, so an
     * invoice it reads wrong can become a private test (`domain/src/test/private-invoices/`). A release build never
     * writes it; the PDF itself is never written.
     */
    suspend fun writeTrace(words: List<InvoiceWord>, reading: InvoiceReading) {
        if (!BuildConfig.DEBUG) return
        withContext(Dispatchers.IO) {
            val json = JsonObject(
                mapOf(
                    "words" to Json.parseToJsonElement(InvoiceWords.toJson(words)),
                    "lines" to JsonArray(reading.lines.map { JsonPrimitive(it.text) }),
                    "trace" to JsonArray(reading.trace.map { JsonPrimitive(it.toString()) }),
                ),
            )
            val dir = File(context.filesDir, TRACES).apply { mkdirs() }
            File(dir, "invoice-${LocalDateTime.now().format(STAMP)}.json").writeText(json.toString())
        }
    }

    private companion object {
        const val TRACES = "invoice-traces"
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
    }
}

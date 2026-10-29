package io.cosgriff.glovebox.ui.scan

import android.content.Context
import com.googlecode.tesseract.android.ResultIterator
import com.googlecode.tesseract.android.TessBaseAPI
import io.cosgriff.glovebox.domain.InvoiceWord
import java.io.File
import java.io.IOException

/**
 * Reads the text in one camera frame with Tesseract, entirely on the phone, for the things the
 * camera is pointed at to read: the odometer ([digits]), a printed VIN ([vin]) and a page of a paper
 * invoice ([page]). Tesseract is told which characters there can be ([characters], null for any) and
 * how the text is laid out ([pageMode]), and [read] keeps only the words it is at least
 * [minConfidence] (of 100) sure of.
 *
 * It runs entirely on the phone: the engine is a native
 * library and its language file ships in the app, so nothing is fetched and nothing is sent. Only
 * the greyscale (luminance) plane is needed. Not thread-safe: use one per analyzer, on its thread.
 *
 * Tesseract opens its language file by path, so the copy in `assets/tessdata` is written to the
 * cache the first time and reused; Android may clear it, and `DataWipe` does, and it is simply
 * copied again. It holds nothing of the user's.
 */
class TextReader private constructor(
    private val context: Context,
    private val characters: String?,
    private val minConfidence: Float,
    // The numbers on a cluster, and a VIN among the other print on a plate or a card, are scattered about, not laid out as a page.
    private val pageMode: Int = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT,
    private val wordLists: Map<String, String> = NO_WORD_LISTS,
) {
    private var api: TessBaseAPI? = null

    /** Set when the engine couldn't be started; nothing more is tried after that. */
    var failed = false
        private set

    /**
     * What Tesseract makes of the frame, in the reader's own characters only, or null when it read
     * nothing or can't run. [luminance] is [width] x [height], one byte per pixel, rows top to bottom.
     */
    fun read(luminance: ByteArray, width: Int, height: Int): String? {
        val text = StringBuilder()
        for (word in words(luminance, width, height)) {
            if (word.confidence < minConfidence) continue
            if (text.isNotEmpty()) text.append(if (word.startsLine) '\n' else ' ')
            text.append(word.text)
        }
        return text.toString().takeIf { it.isNotEmpty() }
    }

    /** One word as Tesseract read it. [confidence] is its own figure, 0 to 100; [startsLine] is true for the first word of a line. */
    data class Word(val text: String, val confidence: Float, val startsLine: Boolean)

    /** Everything Tesseract found, sure of it or not; [read] keeps the words it is sure of. */
    fun words(luminance: ByteArray, width: Int, height: Int): List<Word> =
        recognise(luminance, width, height) { text, iterator -> Word(text, iterator.confidence(WORD), iterator.isAtBeginningOf(LINE)) }

    /**
     * Every word with its box in the picture's pixels and its confidence, sure of it or not, for
     * [io.cosgriff.glovebox.domain.InvoiceReader] (#513): the sums on the invoice vouch for its figures, not
     * Tesseract. The words are on page 0; [io.cosgriff.glovebox.domain.PhotoPages] numbers them.
     */
    fun boxedWords(luminance: ByteArray, width: Int, height: Int): List<InvoiceWord> =
        recognise(luminance, width, height) { text, iterator ->
            val box = iterator.getBoundingBox(WORD)
            InvoiceWord(text, 0, box[0].toFloat(), box[1].toFloat(), box[2].toFloat(), box[3].toFloat(), iterator.confidence(WORD))
        }

    private fun <T> recognise(luminance: ByteArray, width: Int, height: Int, word: (String, ResultIterator) -> T): List<T> {
        val engine = api ?: start() ?: return emptyList()
        engine.setImage(darkOnLight(luminance), width, height, 1, width)
        try {
            // Recognition runs here; the iterator below only walks what it found.
            if (engine.utF8Text.isNullOrBlank()) return emptyList()
            val words = mutableListOf<T>()
            val iterator = engine.resultIterator ?: return emptyList()
            try {
                iterator.begin()
                do {
                    val text = iterator.getUTF8Text(WORD)?.trim().orEmpty()
                    if (text.isNotEmpty()) words += word(text, iterator)
                } while (iterator.next(WORD))
            } finally {
                iterator.delete()
            }
            return words
        } finally {
            engine.clear()
        }
    }

    fun close() {
        api?.recycle()
        api = null
    }

    private fun start(): TessBaseAPI? {
        if (failed) return null
        val engine = try {
            val data = languageFile()
            TessBaseAPI().takeIf { it.init(data.parentFile!!.parentFile!!.absolutePath, LANGUAGE, TessBaseAPI.OEM_LSTM_ONLY, wordLists) }
        } catch (_: IOException) {
            null
        } catch (_: LinkageError) {
            // No native library for this phone's processor: UnsatisfiedLinkError the first time, and
            // NoClassDefFoundError for every reader after that, since TessBaseAPI never finished loading.
            null
        }
        if (engine == null) {
            failed = true
            return null
        }
        characters?.let { engine.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, it) }
        engine.pageSegMode = pageMode
        api = engine
        return engine
    }

    /** The language file where Tesseract can open it: `<cache>/tesseract/tessdata/eng.traineddata`. */
    private fun languageFile(): File {
        val file = File(context.cacheDir, "tesseract/tessdata/$LANGUAGE.traineddata")
        val length = context.assets.openFd(ASSET).use { it.length }
        if (!file.isFile || file.length() != length) {
            file.parentFile!!.mkdirs()
            val part = File(file.parentFile, "${file.name}.part")
            context.assets.open(ASSET).use { input -> part.outputStream().use { input.copyTo(it) } }
            if (!part.renameTo(file)) throw IOException("couldn't put the language file in place")
        }
        return file
    }

    companion object {
        private const val LANGUAGE = "eng"
        private const val ASSET = "tessdata/$LANGUAGE.traineddata"
        private const val WORD = TessBaseAPI.PageIteratorLevel.RIL_WORD
        private const val LINE = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE

        /**
         * Tesseract's English word lists pull a reading towards words. Neither an odometer nor a VIN is
         * one, so they are left out; they can only be switched off as the engine starts.
         */
        private val NO_WORD_LISTS = mapOf("load_system_dawg" to "0", "load_freq_dawg" to "0")

        /**
         * For the odometer. An odometer is digits; the point and comma are kept so a trip meter's
         * tenths can be told apart. Tesseract will make a digit out of a bookshelf if asked for digits
         * only, but it isn't sure of it, and nothing else vouches for a reading, so a word it is less
         * than 70 sure of is dropped.
         */
        fun digits(context: Context) = TextReader(context, "0123456789.,", minConfidence = 70f)

        /**
         * For a printed VIN: capitals and digits. I, O and Q are let through although a VIN has none,
         * so that a 1 or a 0 read as one of them can be put right (`VinScan.printed`) instead of coming
         * out as some other letter. The space is listed too: left out, Tesseract runs the words of a line
         * together ("VIN1M8GDM9AX…"), and a VIN beside a label is lost in a longer run. Every word is kept, however unsure: a VIN vouches for itself with
         * its check digit, and has to be read the same twice.
         */
        fun vin(context: Context) = TextReader(context, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ", minConfidence = 0f)

        /**
         * For a page of a paper invoice (#513), already flattened (`PageLight`): any character, with the
         * English word lists, since most of an invoice is words. Read as a page (PSM 3): #482 measured it
         * best on real photos, 55 of 56 figures, since the layout analysis tells the description column from
         * the subtotals beside it.
         */
        fun page(context: Context) =
            TextReader(context, characters = null, minConfidence = 0f, pageMode = TessBaseAPI.PageSegMode.PSM_AUTO, wordLists = emptyMap())

        /** Darker than this on average and the picture is taken to be lit digits on a dark display. */
        private const val DARK = 110

        /**
         * Tesseract is trained on dark print on a light page, and most odometers are the reverse:
         * lit digits on a dark display. A frame that is dark overall is turned into its negative.
         */
        fun darkOnLight(luminance: ByteArray): ByteArray {
            if (luminance.isEmpty()) return luminance
            var sum = 0L
            for (b in luminance) sum += b.toInt() and 0xFF
            if (sum / luminance.size >= DARK) return luminance
            return ByteArray(luminance.size) { (255 - (luminance[it].toInt() and 0xFF)).toByte() }
        }
    }
}

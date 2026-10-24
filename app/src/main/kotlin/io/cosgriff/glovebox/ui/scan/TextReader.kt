package io.cosgriff.glovebox.ui.scan

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.IOException

/**
 * Reads the text in one camera frame with Tesseract, entirely on the phone, for the two things the
 * camera is pointed at to read: the odometer ([digits]) and a printed VIN ([vin]). Tesseract is told
 * which characters there can be ([characters]), and keeps only the words it is at least
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
class TextReader private constructor(private val context: Context, private val characters: String, private val minConfidence: Float) {
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
    fun words(luminance: ByteArray, width: Int, height: Int): List<Word> {
        val engine = api ?: start() ?: return emptyList()
        engine.setImage(darkOnLight(luminance), width, height, 1, width)
        try {
            // Recognition runs here; the iterator below only walks what it found.
            if (engine.utF8Text.isNullOrBlank()) return emptyList()
            val words = mutableListOf<Word>()
            val iterator = engine.resultIterator ?: return emptyList()
            try {
                iterator.begin()
                do {
                    val word = iterator.getUTF8Text(WORD)?.trim().orEmpty()
                    if (word.isNotEmpty()) words += Word(word, iterator.confidence(WORD), iterator.isAtBeginningOf(LINE))
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
            TessBaseAPI().takeIf { it.init(data.parentFile!!.parentFile!!.absolutePath, LANGUAGE, TessBaseAPI.OEM_LSTM_ONLY, NO_WORD_LISTS) }
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
        engine.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characters)
        // The numbers on a cluster, and a VIN among the other print on a plate or a card, are scattered about, not laid out as a page.
        engine.pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
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

package io.cosgriff.glovebox.ui.scan

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.IOException

/**
 * Reads the text in one camera frame with Tesseract, entirely on the phone: the engine is a native
 * library and its language file ships in the app, so nothing is fetched and nothing is sent. Only
 * the greyscale (luminance) plane is needed. Not thread-safe: use one per analyzer, on its thread.
 *
 * Tesseract opens its language file by path, so the copy in `assets/tessdata` is written to the
 * cache the first time and reused; Android may clear it, and `DataWipe` does, and it is simply
 * copied again. It holds nothing of the user's.
 */
class DashReader(private val context: Context) {
    private var api: TessBaseAPI? = null

    /** Set when the engine couldn't be started; nothing more is tried after that. */
    var failed = false
        private set

    /**
     * What Tesseract makes of the frame, digits and their punctuation only, or null when it read
     * nothing or can't run. [luminance] is [width] x [height], one byte per pixel, rows top to bottom.
     */
    fun read(luminance: ByteArray, width: Int, height: Int): String? {
        val text = StringBuilder()
        for (word in words(luminance, width, height)) {
            if (word.confidence < MIN_CONFIDENCE) continue
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
            TessBaseAPI().takeIf { it.init(data.parentFile!!.parentFile!!.absolutePath, LANGUAGE, TessBaseAPI.OEM_LSTM_ONLY) }
        } catch (_: IOException) {
            null
        } catch (_: UnsatisfiedLinkError) {
            // No native library for this phone's processor.
            null
        }
        if (engine == null) {
            failed = true
            return null
        }
        // An odometer is digits; the point and comma are kept so a trip meter's tenths can be told apart.
        engine.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789.,")
        // The numbers on a cluster are scattered about, not laid out as a page.
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
         * Tesseract will make a digit out of a bookshelf if asked for digits only, but it isn't sure
         * of it. A word below this (of 100) is dropped before anything looks for a reading in it.
         */
        const val MIN_CONFIDENCE = 70f

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

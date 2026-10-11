package io.cosgriff.glovebox.data.backup

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The encrypted backup file. docs/backup-format.md is the written spec; keep the two in step.
 *
 * Plain JVM code with no Android in it, so the format is covered by ordinary unit tests.
 */
object BackupFormat {
    /** Raised whenever the layout below changes in a way an older app couldn't read. */
    const val VERSION = 1
    val MAGIC: ByteArray = "GLOVEBOX".toByteArray(Charsets.US_ASCII)
    const val FILE_EXTENSION = "glovebox"

    const val KDF_ARGON2ID = 1
    const val SALT_BYTES = 16
    const val NONCE_PREFIX_BYTES = 4
    const val HEADER_BYTES = 8 + 1 + 1 + 4 + 4 + 1 + SALT_BYTES + NONCE_PREFIX_BYTES
    const val KEY_BYTES = 32
    const val TAG_BYTES = 16

    /** Plaintext per chunk. Small enough to hold in memory, large enough that the framing costs nothing. */
    const val CHUNK_BYTES = 64 * 1024

    // What a reader will accept from a file, so a hostile one can't make the phone allocate gigabytes.
    const val MAX_MEMORY_KIB = 1024 * 1024
    const val MAX_ITERATIONS = 16
    const val MAX_PARALLELISM = 8
}

/**
 * Argon2id cost. The defaults are what new backups use: 64 MiB and three passes, which takes a
 * phone a second or two and makes guessing passwords expensive. They are written into each file,
 * so they can be raised later without breaking old backups.
 */
data class BackupKdf(val memoryKiB: Int = 64 * 1024, val iterations: Int = 3, val parallelism: Int = 1) {
    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val key = ByteArray(BackupFormat.KEY_BYTES)
        Argon2BytesGenerator().apply {
            init(
                Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(salt)
                    .withMemoryAsKB(memoryKiB)
                    .withIterations(iterations)
                    .withParallelism(parallelism)
                    .build(),
            )
        }.generateBytes(password, key)
        return key
    }
}

class BackupException(val reason: Reason, cause: Throwable? = null) : IOException(reason.name, cause) {
    enum class Reason {
        /** Doesn't start with the backup marker. */
        NOT_A_BACKUP,

        /** Written by a newer version of the app than this one. */
        NEWER_VERSION,

        /**
         * The first block didn't open. Almost always the wrong password; a file damaged in its first
         * few bytes looks the same, because nothing can be told apart until a key has worked.
         */
        WRONG_PASSWORD,

        /** The password worked but a later part has been changed or damaged. */
        DAMAGED,

        /** The file ends before its final block: an interrupted copy or download. */
        INCOMPLETE,
    }
}

/**
 * Encrypts everything written to it into a backup file on [out]. Closing it writes the final
 * block; a file that was never closed properly is detected as incomplete when read.
 */
class BackupOutputStream(
    private val out: OutputStream,
    password: CharArray,
    kdf: BackupKdf = BackupKdf(),
    random: SecureRandom = SecureRandom(),
) : OutputStream() {
    private val header: ByteArray
    private val noncePrefix = ByteArray(BackupFormat.NONCE_PREFIX_BYTES).also(random::nextBytes)
    private val key: SecretKeySpec
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val buffer = ByteArray(BackupFormat.CHUNK_BYTES)
    private var filled = 0
    private var counter = 0L
    private var closed = false

    init {
        val salt = ByteArray(BackupFormat.SALT_BYTES).also(random::nextBytes)
        header = ByteBuffer.allocate(BackupFormat.HEADER_BYTES)
            .put(BackupFormat.MAGIC)
            .put(BackupFormat.VERSION.toByte())
            .put(BackupFormat.KDF_ARGON2ID.toByte())
            .putInt(kdf.memoryKiB)
            .putInt(kdf.iterations)
            .put(kdf.parallelism.toByte())
            .put(salt)
            .put(noncePrefix)
            .array()
        key = SecretKeySpec(kdf.derive(password, salt), "AES")
        out.write(header)
        // Block 0 is empty. It exists so a reader can tell a wrong password from later damage.
        writeChunk(ByteArray(0), 0, final = false)
    }

    override fun write(b: Int) {
        buffer[filled++] = b.toByte()
        if (filled == buffer.size) flushChunk(final = false)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var from = off
        var left = len
        while (left > 0) {
            val n = minOf(left, buffer.size - filled)
            System.arraycopy(b, from, buffer, filled, n)
            filled += n
            from += n
            left -= n
            if (filled == buffer.size) flushChunk(final = false)
        }
    }

    private fun flushChunk(final: Boolean) {
        writeChunk(buffer, filled, final)
        filled = 0
    }

    private fun writeChunk(plain: ByteArray, length: Int, final: Boolean) {
        val flag = if (final) 1 else 0
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(BackupFormat.TAG_BYTES * 8, nonce(noncePrefix, counter)))
        cipher.updateAAD(aad(header, counter, flag))
        val sealed = cipher.doFinal(plain, 0, length)
        out.write(flag)
        out.write(ByteBuffer.allocate(4).putInt(sealed.size).array())
        out.write(sealed)
        counter++
    }

    override fun close() {
        if (closed) return
        closed = true
        flushChunk(final = true)
        out.close()
    }
}

/** Reads a backup file back. Every failure is a [BackupException] saying which kind. */
class BackupInputStream(input: InputStream, password: CharArray) : InputStream() {
    private val data = DataInputStream(input)
    private val header = ByteArray(BackupFormat.HEADER_BYTES)
    private val noncePrefix = ByteArray(BackupFormat.NONCE_PREFIX_BYTES)
    private val key: SecretKeySpec
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private var chunk = ByteArray(0)
    private var position = 0
    private var counter = 0L
    private var finished = false

    init {
        try {
            data.readFully(header)
        } catch (e: EOFException) {
            throw BackupException(BackupException.Reason.NOT_A_BACKUP, e)
        }
        val fields = ByteBuffer.wrap(header)
        val magic = ByteArray(BackupFormat.MAGIC.size).also { fields.get(it) }
        if (!magic.contentEquals(BackupFormat.MAGIC)) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        val version = fields.get().toInt() and 0xFF
        if (version > BackupFormat.VERSION) throw BackupException(BackupException.Reason.NEWER_VERSION)
        if (version < 1) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        val kdfId = fields.get().toInt()
        val kdf = BackupKdf(memoryKiB = fields.getInt(), iterations = fields.getInt(), parallelism = fields.get().toInt())
        val sane = kdfId == BackupFormat.KDF_ARGON2ID &&
            kdf.memoryKiB in 8..BackupFormat.MAX_MEMORY_KIB &&
            kdf.iterations in 1..BackupFormat.MAX_ITERATIONS &&
            kdf.parallelism in 1..BackupFormat.MAX_PARALLELISM
        // The header isn't authenticated until a key exists, so nothing in it is trusted further than this.
        if (!sane) throw BackupException(BackupException.Reason.DAMAGED)
        val salt = ByteArray(BackupFormat.SALT_BYTES).also { fields.get(it) }
        fields.get(noncePrefix)
        key = SecretKeySpec(kdf.derive(password, salt), "AES")
        // Block 0 proves the key before anything else is read.
        if (!readChunk(first = true)) throw BackupException(BackupException.Reason.DAMAGED)
    }

    /** False once the final block has been read. */
    private fun readChunk(first: Boolean = false): Boolean {
        if (finished) return false
        val flag: Int
        val sealed: ByteArray
        try {
            flag = data.readUnsignedByte()
            val length = data.readInt()
            if (flag > 1 || length < BackupFormat.TAG_BYTES || length > BackupFormat.CHUNK_BYTES + BackupFormat.TAG_BYTES) {
                throw BackupException(if (first) BackupException.Reason.WRONG_PASSWORD else BackupException.Reason.DAMAGED)
            }
            sealed = ByteArray(length).also { data.readFully(it) }
        } catch (e: EOFException) {
            throw BackupException(BackupException.Reason.INCOMPLETE, e)
        }
        chunk = try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(BackupFormat.TAG_BYTES * 8, nonce(noncePrefix, counter)))
            cipher.updateAAD(aad(header, counter, flag))
            cipher.doFinal(sealed)
        } catch (e: GeneralSecurityException) {
            throw BackupException(if (first) BackupException.Reason.WRONG_PASSWORD else BackupException.Reason.DAMAGED, e)
        }
        position = 0
        counter++
        if (flag == 1) finished = true
        return true
    }

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        while (position == chunk.size) {
            if (!readChunk()) return -1
        }
        val n = minOf(len, chunk.size - position)
        System.arraycopy(chunk, position, b, off, n)
        position += n
        return n
    }

    override fun close() = data.close()
}

/** 12 bytes: the file's random prefix, then the block number. A (key, nonce) pair is never reused. */
private fun nonce(prefix: ByteArray, counter: Long): ByteArray = ByteBuffer.allocate(12).put(prefix).putLong(counter).array()

/**
 * What each block is bound to: the whole header, its own number and whether it is the last. So a
 * block can't be moved, dropped, taken from another file or passed off as the end.
 */
private fun aad(header: ByteArray, counter: Long, flag: Int): ByteArray =
    ByteBuffer.allocate(header.size + 9).put(header).putLong(counter).put(flag.toByte()).array()

/** What the encrypted part of a backup holds, in the order written. */
enum class BackupEntryKind(val code: Int) {
    /** JSON: when it was made, by which schema, and counts for the restore summary. Always first. */
    MANIFEST(1),

    /** JSON array of one table's rows; the entry name is the table's. */
    TABLE(2),

    /** JSON object of the settings worth carrying to another phone. */
    SETTINGS(3),

    /** One photo as plain JPEG bytes; the entry name is its file name in the photo folder. */
    PHOTO(4),
    ;

    companion object {
        fun of(code: Int): BackupEntryKind? = entries.firstOrNull { it.code == code }
    }
}

class BackupEntry(val kind: BackupEntryKind, val name: String, val bytes: ByteArray)

/** Writes entries into a [BackupOutputStream]. Each is: kind, name, length, bytes. */
class BackupEntryWriter(out: OutputStream) : AutoCloseable {
    private val data = DataOutputStream(out)

    fun write(kind: BackupEntryKind, name: String, bytes: ByteArray) {
        data.writeByte(kind.code)
        data.writeUTF(name)
        data.writeInt(bytes.size)
        data.write(bytes)
    }

    /** A zero marks the end, so a reader knows nothing was cut off inside the encrypted part either. */
    override fun close() {
        data.writeByte(0)
        data.close()
    }
}

class BackupEntryReader(input: InputStream) : AutoCloseable {
    private val data = DataInputStream(input)

    /** The next entry, or null at the end. Entries of a kind this version doesn't know are skipped. */
    fun next(): BackupEntry? {
        while (true) {
            val code = try {
                data.readUnsignedByte()
            } catch (e: EOFException) {
                throw BackupException(BackupException.Reason.DAMAGED, e)
            }
            if (code == 0) return null
            try {
                val name = data.readUTF()
                val length = data.readInt()
                if (length < 0) throw BackupException(BackupException.Reason.DAMAGED)
                val bytes = ByteArray(length).also { data.readFully(it) }
                BackupEntryKind.of(code)?.let { return BackupEntry(it, name, bytes) }
            } catch (e: EOFException) {
                throw BackupException(BackupException.Reason.DAMAGED, e)
            }
        }
    }

    override fun close() = data.close()
}

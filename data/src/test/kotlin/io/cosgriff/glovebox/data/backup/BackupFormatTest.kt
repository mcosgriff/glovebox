package io.cosgriff.glovebox.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class BackupFormatTest {
    // Cheap settings so the tests are quick; the real cost is checked once below.
    private val quick = BackupKdf(memoryKiB = 64, iterations = 1, parallelism = 1)
    private val password = "correct horse battery".toCharArray()

    private fun seal(plain: ByteArray, pass: CharArray = password, kdf: BackupKdf = quick): ByteArray {
        val out = ByteArrayOutputStream()
        BackupOutputStream(out, pass, kdf).use { it.write(plain) }
        return out.toByteArray()
    }

    private fun open(file: ByteArray, pass: CharArray = password): ByteArray = BackupInputStream(ByteArrayInputStream(file), pass).use { it.readBytes() }

    private fun reasonOf(block: () -> Unit): BackupException.Reason = try {
        block()
        fail("expected a BackupException")
        throw AssertionError()
    } catch (e: BackupException) {
        e.reason
    }

    private fun noise(size: Int) = ByteArray(size).also { Random(7).nextBytes(it) }

    @Test
    fun `what goes in comes out, across block boundaries`() {
        for (size in listOf(0, 1, BackupFormat.CHUNK_BYTES - 1, BackupFormat.CHUNK_BYTES, BackupFormat.CHUNK_BYTES + 1, 3 * BackupFormat.CHUNK_BYTES + 123)) {
            val plain = noise(size)
            assertArrayEquals("size $size", plain, open(seal(plain)))
        }
    }

    @Test
    fun `the file starts with the marker and version and hides its contents`() {
        val plain = "service records for a made-up vehicle".toByteArray()
        val file = seal(plain)
        assertEquals("GLOVEBOX", String(file, 0, 8, Charsets.US_ASCII))
        assertEquals(BackupFormat.VERSION, file[8].toInt())
        assertFalse(String(file, Charsets.ISO_8859_1).contains("made-up vehicle"))
    }

    @Test
    fun `two backups of the same data share nothing after the header fields`() {
        val plain = noise(1000)
        val a = seal(plain)
        val b = seal(plain)
        // Different salt and nonce prefix, so different keys and different ciphertext.
        assertFalse(a.copyOfRange(19, a.size).contentEquals(b.copyOfRange(19, b.size)))
    }

    @Test
    fun `the wrong password is reported as such`() {
        val file = seal(noise(5000))
        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf { open(file, "correct horse batterx".toCharArray()) })
        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf { open(file, CharArray(0)) })
    }

    @Test
    fun `a changed byte anywhere is caught`() {
        val file = seal(noise(2 * BackupFormat.CHUNK_BYTES))
        // Salt, nonce prefix and the first block all look like a wrong password: no key has worked yet.
        for (at in listOf(20, 36, BackupFormat.HEADER_BYTES + 6)) {
            val bad = file.copyOf().also { it[at] = (it[at] + 1).toByte() }
            assertEquals("byte $at", BackupException.Reason.WRONG_PASSWORD, reasonOf { open(bad) })
        }
        // Anywhere after that, the password worked and the damage is named.
        for (at in listOf(file.size / 2, file.size - 1)) {
            val bad = file.copyOf().also { it[at] = (it[at] + 1).toByte() }
            assertEquals("byte $at", BackupException.Reason.DAMAGED, reasonOf { open(bad) })
        }
    }

    @Test
    fun `a file cut short is incomplete, never silently shorter`() {
        val file = seal(noise(3 * BackupFormat.CHUNK_BYTES))
        for (keep in listOf(file.size - 1, file.size - 20, file.size / 2, BackupFormat.HEADER_BYTES + 21)) {
            assertEquals("kept $keep", BackupException.Reason.INCOMPLETE, reasonOf { open(file.copyOf(keep)) })
        }
        // Cut exactly between two blocks: every block present is genuine, but the final one is missing.
        val firstData = BackupFormat.HEADER_BYTES + 5 + BackupFormat.TAG_BYTES
        val afterOneBlock = firstData + 5 + BackupFormat.CHUNK_BYTES + BackupFormat.TAG_BYTES
        assertEquals(BackupException.Reason.INCOMPLETE, reasonOf { open(file.copyOf(afterOneBlock)) })
    }

    @Test
    fun `blocks can't be reordered or dropped`() {
        val file = seal(noise(3 * BackupFormat.CHUNK_BYTES))
        val start = BackupFormat.HEADER_BYTES + 5 + BackupFormat.TAG_BYTES
        val block = 5 + BackupFormat.CHUNK_BYTES + BackupFormat.TAG_BYTES
        val swapped = file.copyOf()
        System.arraycopy(file, start + block, swapped, start, block)
        System.arraycopy(file, start, swapped, start + block, block)
        assertEquals(BackupException.Reason.DAMAGED, reasonOf { open(swapped) })
        val dropped = file.copyOfRange(0, start) + file.copyOfRange(start + block, file.size)
        assertEquals(BackupException.Reason.DAMAGED, reasonOf { open(dropped) })
    }

    @Test
    fun `other files and newer backups are told apart`() {
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reasonOf { open("%PDF-1.4 not a backup at all, just a document".toByteArray() + ByteArray(40)) })
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reasonOf { open(ByteArray(5)) })
        val newer = seal(noise(10)).also { it[8] = (BackupFormat.VERSION + 1).toByte() }
        assertEquals(BackupException.Reason.NEWER_VERSION, reasonOf { open(newer) })
    }

    @Test
    fun `a file asking for absurd key-derivation cost is refused before any work is done`() {
        val file = seal(noise(10))
        // Memory field (4 bytes at offset 10) set to about 2 GiB.
        val hungry = file.copyOf().also { it[10] = 0x7F }
        assertEquals(BackupException.Reason.DAMAGED, reasonOf { open(hungry) })
    }

    @Test
    fun `a file asking for more key-derivation memory than a phone can spare is refused`() {
        val file = seal(noise(10))
        // Memory field (4 bytes at offset 10) set to 256 MiB.
        val hungry = file.copyOf().also { it[10] = 0; it[11] = 0x04; it[12] = 0; it[13] = 0 }
        assertEquals(BackupException.Reason.DAMAGED, reasonOf { open(hungry) })
    }

    @Test
    fun `an entry claiming to be bigger than any backup holds is damage, not a crash`() {
        val out = ByteArrayOutputStream()
        BackupOutputStream(out, password, quick).use { s ->
            val d = java.io.DataOutputStream(s)
            d.writeByte(BackupEntryKind.PHOTO.code); d.writeUTF("record-a.jpg"); d.writeInt(Int.MAX_VALUE)
            d.writeByte(0)
            d.flush()
        }
        BackupEntryReader(BackupInputStream(ByteArrayInputStream(out.toByteArray()), password)).use { r ->
            assertEquals(BackupException.Reason.DAMAGED, reasonOf { r.next() })
        }
    }

    @Test
    fun `a version 1 file written today stays readable`() {
        // The format's promise to older backups: this exact layout keeps opening. If VERSION goes up,
        // add a reader for the new layout and keep this test passing unchanged.
        val file = seal("still here".toByteArray())
        assertEquals(1, file[8].toInt())
        assertEquals(39, BackupFormat.HEADER_BYTES)
        assertEquals("still here", String(open(file)))
    }

    @Test
    fun `the cost new backups use works and is what the spec says`() {
        val kdf = BackupKdf()
        assertEquals(64 * 1024, kdf.memoryKiB)
        assertEquals(3, kdf.iterations)
        val file = seal("x".toByteArray(), kdf = kdf)
        assertEquals("x", String(open(file)))
    }

    @Test
    fun `the Argon2id implementation matches the RFC 9106 test vector`() {
        // RFC 9106 section 5.3: Argon2id, version 0x13, 32 KiB, 3 passes, 4 lanes, 32-byte tag.
        val tag = ByteArray(32)
        org.bouncycastle.crypto.generators.Argon2BytesGenerator().apply {
            init(
                org.bouncycastle.crypto.params.Argon2Parameters.Builder(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id)
                    .withVersion(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_VERSION_13)
                    .withMemoryAsKB(32)
                    .withIterations(3)
                    .withParallelism(4)
                    .withSalt(ByteArray(16) { 2 })
                    .withSecret(ByteArray(8) { 3 })
                    .withAdditional(ByteArray(12) { 4 })
                    .build(),
            )
        }.generateBytes(ByteArray(32) { 1 }, tag)
        assertEquals("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659", tag.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun `the key depends on the password, the salt and the cost`() {
        val salt = "somesalt12345678".toByteArray()
        val kdf = BackupKdf(memoryKiB = 64, iterations = 2, parallelism = 1)
        val key = kdf.derive("password".toCharArray(), salt)
        assertEquals(32, key.size)
        assertArrayEquals(key, kdf.derive("password".toCharArray(), salt))
        assertFalse(key.contentEquals(kdf.derive("passwore".toCharArray(), salt)))
        assertFalse(key.contentEquals(kdf.derive("password".toCharArray(), "somesalt12345679".toByteArray())))
        assertFalse(key.contentEquals(kdf.copy(iterations = 3).derive("password".toCharArray(), salt)))
        assertFalse(key.contentEquals(kdf.copy(memoryKiB = 128).derive("password".toCharArray(), salt)))
    }

    @Test
    fun `entries round trip in order and end cleanly`() {
        val photo = noise(200_000)
        val out = ByteArrayOutputStream()
        BackupEntryWriter(BackupOutputStream(out, password, quick)).use { w ->
            w.write(BackupEntryKind.MANIFEST, "", """{"format":1}""".toByteArray())
            w.write(BackupEntryKind.TABLE, "vehicles", "[]".toByteArray())
            w.write(BackupEntryKind.PHOTO, "record-abc.jpg", photo)
        }
        BackupEntryReader(BackupInputStream(ByteArrayInputStream(out.toByteArray()), password)).use { r ->
            val manifest = r.next()!!
            assertEquals(BackupEntryKind.MANIFEST, manifest.kind)
            assertEquals("""{"format":1}""", String(manifest.bytes))
            val table = r.next()!!
            assertEquals("vehicles", table.name)
            val p = r.next()!!
            assertEquals("record-abc.jpg", p.name)
            assertArrayEquals(photo, p.bytes)
            assertNull(r.next())
        }
    }

    @Test
    fun `an entry kind from a later version is skipped, not fatal`() {
        val out = ByteArrayOutputStream()
        BackupOutputStream(out, password, quick).use { s ->
            val d = java.io.DataOutputStream(s)
            d.writeByte(99); d.writeUTF("future"); d.writeInt(3); d.write(byteArrayOf(1, 2, 3))
            d.writeByte(BackupEntryKind.SETTINGS.code); d.writeUTF(""); d.writeInt(2); d.write("{}".toByteArray())
            d.writeByte(0)
            d.flush()
        }
        BackupEntryReader(BackupInputStream(ByteArrayInputStream(out.toByteArray()), password)).use { r ->
            assertEquals(BackupEntryKind.SETTINGS, r.next()!!.kind)
            assertNull(r.next())
        }
    }

    @Test
    fun `entries that stop without an end marker are damaged`() {
        val out = ByteArrayOutputStream()
        BackupOutputStream(out, password, quick).use { s ->
            val d = java.io.DataOutputStream(s)
            d.writeByte(BackupEntryKind.TABLE.code); d.writeUTF("vehicles"); d.writeInt(2); d.write("[]".toByteArray())
            d.flush()
        }
        BackupEntryReader(BackupInputStream(ByteArrayInputStream(out.toByteArray()), password)).use { r ->
            assertTrue(r.next() != null)
            assertEquals(BackupException.Reason.DAMAGED, reasonOf { r.next() })
        }
    }

    @Test
    fun `settings come back as they went, each with its type`() {
        val prefs = mapOf<String, Any>("theme" to "DARK", "lockAfterSeconds" to 30, "lastBackupOn" to 20_400L, "biometrics" to true)
        assertEquals(prefs, BackupFormat.settingsFromJson(BackupFormat.settingsToJson(prefs)))
        // One this version can't read is left out, and that setting keeps its default.
        val odd = kotlinx.serialization.json.Json.parseToJsonElement("""{ "x": { "type": "colour", "value": "red" }, "theme": { "type": "string", "value": "LIGHT" } }""")
        assertEquals(mapOf<String, Any>("theme" to "LIGHT"), BackupFormat.settingsFromJson(odd as kotlinx.serialization.json.JsonObject))
    }
}

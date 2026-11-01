package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Real invoices, kept only on the owner's machine (#483): `domain/src/test/private-invoices/` is git-ignored and
 * never committed. Each invoice is a folder with `words.json` (its words, as [InvoiceWords.toJson] writes them,
 * from the debug trace) and a hand-written `expected.json`. Skipped where the folder is empty, as in CI.
 *
 * `expected.json` compares only the keys it has, and `null` means "left out":
 * ```
 * { "today": "2026-10-26", "unit": "miles", "readings": [{ "date": "2025-09-02", "odometer": 121400 }],
 *   "date": "2026-03-18", "shop": "…", "vin": "…", "odometerIn": 129562, "odometerOut": 129565,
 *   "grandTotalCents": 103997,
 *   "jobs": [{ "wording": "…", "laborCents": 26000, "partsCents": 47996, "guesses": ["spark-plugs"] }],
 *   "recommendations": ["Synthetic Oil Change"] }
 * ```
 * The first four are the reader's inputs (today defaults to the day the test runs); "jobs" must match in number and
 * order, each job by the keys it has.
 */
class PrivateInvoicesTest {
    private val folder = File("src/test/private-invoices")

    @Test
    fun `each private invoice reads as its expected file says`() {
        val cases = folder.listFiles()?.filter { File(it, "words.json").isFile && File(it, "expected.json").isFile }.orEmpty().sortedBy { it.name }
        assumeTrue("no private invoices in $folder", cases.isNotEmpty())
        val failures = cases.flatMap { case ->
            val expected = Json.parseToJsonElement(File(case, "expected.json").readText()).jsonObject
            val json = File(case, "words.json").readText()
            val words = InvoiceWords.fromJson(json)
            val r = InvoiceReader.read(
                words,
                today = expected["today"]?.let { LocalDate.parse(it.jsonPrimitive.content) } ?: LocalDate.now(),
                readings = (expected["readings"] as? JsonArray).orEmpty().map {
                    OdometerReading(LocalDate.parse(it.jsonObject.getValue("date").jsonPrimitive.content), it.jsonObject.getValue("odometer").jsonPrimitive.int)
                },
                unit = if (expected["unit"]?.jsonPrimitive?.content == "km") DistanceUnit.KILOMETERS else DistanceUnit.MILES,
                marks = InvoiceWords.marksFromJson(json),
            )
            mismatches(expected, r).map { "${case.name}: $it" }.also { if (it.isNotEmpty()) println(r.trace.joinToString("\n") { s -> "  ${case.name} | $s" }) }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun mismatches(e: JsonObject, r: InvoiceReading): List<String> {
        val out = mutableListOf<String>()
        fun check(key: String, actual: Any?, read: (JsonElement) -> Any?) {
            val want = e[key] ?: return
            val wanted = if (want is JsonNull) null else read(want)
            if (wanted != actual) out += "$key: expected $wanted, read $actual"
        }
        check("date", r.date) { LocalDate.parse(it.jsonPrimitive.content) }
        check("shop", r.shop) { it.jsonPrimitive.content }
        check("vin", r.vin) { it.jsonPrimitive.content }
        check("odometerIn", r.odometerIn) { it.jsonPrimitive.int }
        check("odometerOut", r.odometerOut) { it.jsonPrimitive.int }
        check("grandTotalCents", r.grandTotalCents) { it.jsonPrimitive.long }
        check("recommendations", r.recommendations.map { it.text }) { j -> (j as JsonArray).map { it.jsonPrimitive.content } }
        // Each as "Area: what was found [COLOUR]", the colour only where the report marked it (#572).
        check("findings", r.findings.map { f -> listOfNotNull("${f.area}: ${f.text.orEmpty()}".trimEnd(':', ' '), f.colour?.let { "[$it]" }).joinToString(" ") }) { j ->
            (j as JsonArray).map { it.jsonPrimitive.content }
        }
        (e["jobs"] as? JsonArray)?.let { jobs ->
            if (jobs.size != r.jobs.size) out += "jobs: expected ${jobs.size}, read ${r.jobs.size} (${r.jobs.map { it.wording }})"
            jobs.zip(r.jobs).forEachIndexed { i, (want, job) ->
                val w = want.jsonObject
                fun job(key: String, actual: Any?, read: (JsonElement) -> Any?) {
                    val v = w[key] ?: return
                    val wanted = if (v is JsonNull) null else read(v)
                    if (wanted != actual) out += "jobs[$i].$key: expected $wanted, read $actual"
                }
                job("wording", job.wording) { it.jsonPrimitive.content }
                job("laborCents", job.laborCents) { it.jsonPrimitive.long }
                job("partsCents", job.partsCents) { it.jsonPrimitive.long }
                job("guesses", job.guesses) { j -> (j as JsonArray).map { it.jsonPrimitive.content } }
            }
        }
        return out
    }
}

package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import java.util.Locale
import kotlin.math.abs

/** One fastener from a `torque-specs` import file, already checked and with its torque in N·m. */
data class ImportedTorqueSpec(
    val name: String,
    val category: TorqueCategory,
    val socket: String? = null,
    val torque: Torque? = null,
    val source: TorqueSource = TorqueSource.USER,
    val note: String? = null,
)

/** A saved row, as far as an import needs to know it. */
data class SavedTorqueSpec(
    val id: Long,
    val name: String,
    val category: TorqueCategory,
    val socket: String?,
    val torque: Torque?,
    val source: TorqueSource,
    val note: String?,
)

/** The `torque-specs` kind: `docs/import/schema/torque-specs.schema.json`. */
object TorqueImport {
    const val MAX_ITEMS = 500
    const val MAX_SOCKET = 30
    const val MAX_NOTE = 140

    /** The file's spellings. ASCII on purpose: "N·m" is awkward to type. */
    val units: Map<String, TorqueUnit> = mapOf("Nm" to TorqueUnit.NEWTON_METER, "ft-lb" to TorqueUnit.FOOT_POUND, "in-lb" to TorqueUnit.INCH_POUND)
    val categories: Map<String, TorqueCategory> = TorqueCategory.entries.associateBy { it.name.lowercase(Locale.US) }
    val sources: Map<String, TorqueSource> = mapOf("factory-manual" to TorqueSource.FACTORY, "own" to TorqueSource.USER)

    // The same, the other way round: what a file calls each, for the writers and the messages.
    val unitNames: Map<TorqueUnit, String> = units.entries.associate { (name, unit) -> unit to name }
    val categoryNames: Map<TorqueCategory, String> = categories.entries.associate { (name, category) -> category to name }
    val sourceNames: Map<TorqueSource, String> = sources.entries.associate { (name, source) -> source to name }

    private val itemKeys = setOf("name", "category", "socket", "torque", "source", "note")

    fun read(items: JsonElement, check: ImportCheck): List<ImportedTorqueSpec> {
        val list = check.array(items, "items", 1, MAX_ITEMS) ?: return emptyList()
        val seen = Repeats<String>()
        return list.mapIndexedNotNull { i, e ->
            val path = ImportCheck.at("items", i)
            val o = check.obj(e, path, itemKeys, required = setOf("name", "category")) ?: return@mapIndexedNotNull null
            val name = o["name"]?.let { check.string(it, "$path.name", VehicleText.MAX_SPEC) }
            val category = o["category"]?.let { check.choice(it, "$path.category", categories) }
            val socket = o["socket"]?.let { check.string(it, "$path.socket", MAX_SOCKET) }
            val torque = o["torque"]?.let { torque(it, "$path.torque", check) }
            val source = o["source"]?.let { check.choice(it, "$path.source", sources) } ?: TorqueSource.USER
            val note = o["note"]?.let { check.string(it, "$path.note", MAX_NOTE) }
            if (source == TorqueSource.FACTORY && "torque" !in o) {
                check.fail("$path.source", "\"factory-manual\" needs a torque value; it says where the value came from")
            }
            if (name == null || category == null) return@mapIndexedNotNull null
            seen.seen(key(name, category), i)?.let { first ->
                check.fail("$path.name", "\"$name\" is already in this file under the same category, at items[$first]")
            }
            ImportedTorqueSpec(name, category, socket, torque, source, note)
        }
    }

    private fun torque(e: JsonElement, path: String, check: ImportCheck): Torque? {
        val o = check.obj(e, path, setOf("value", "unit"), required = setOf("value", "unit")) ?: return null
        val value = o["value"]?.let { check.number(it, "$path.value") }
        val unit = o["unit"]?.let { check.choice(it, "$path.unit", units) }
        if (value == null || unit == null) return null
        if (value <= 0) return null.also { check.fail("$path.value", "must be above zero") }
        // The cap is stated per unit, in round figures, so the schema can say exactly the same thing.
        if (value > maxIn(unit)) {
            return null.also { check.fail("$path.value", "is more than Glovebox accepts (${maxIn(unit)} ${unitNames.getValue(unit)}); check the number and the unit") }
        }
        return Torque.of(value, unit)
    }

    /** The most a file may give in [unit]: 1,000 N·m, rounded down to a whole figure in the others. */
    fun maxIn(unit: TorqueUnit): Int = when (unit) {
        TorqueUnit.NEWTON_METER -> 1000
        TorqueUnit.FOOT_POUND -> 737
        TorqueUnit.INCH_POUND -> 8850
    }

    /** Two rows are the same fastener when the category and the name agree, ignoring case. */
    fun key(name: String, category: TorqueCategory): String = "${category.name}|${Names.key(name)}"

    /** Close enough to be the same setting on a wrench, whichever unit each was written in. */
    private fun sameTorque(a: Torque?, b: Torque?): Boolean =
        if (a == null || b == null) a == b else abs(a.newtonMeters - b.newtonMeters) < 0.05

    /**
     * The file against the vehicle's rows. A row that is only a name (from "Start with common
     * fasteners", say) is filled in whatever the mode, since nothing is lost by it.
     */
    fun plan(items: List<ImportedTorqueSpec>, existing: List<SavedTorqueSpec>): ImportPlan<ImportedTorqueSpec, SavedTorqueSpec> =
        ImportPlans.plan(
            items = items,
            existing = existing,
            key = { key(it.name, it.category) },
            existingKey = { key(it.name, it.category) },
            same = { i, e -> i.socket == e.socket && sameTorque(i.torque, e.torque) && i.note == e.note && (i.torque == null || i.source == e.source) },
            blank = { _, e -> e.socket == null && e.torque == null && e.note == null },
        )
}

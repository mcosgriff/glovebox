package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The part of a [KnownTakesList] that is only for some of the model: a drive or a gearbox. It is used
 * only for a vehicle **known** to have it; one with nothing on record gets none of it, because a fluid
 * for the wrong gearbox is worse than an empty row.
 */
data class KnownTakesVariant(
    val drive: DriveKind? = null,
    /** True for a manual gearbox, false for one that shifts itself ([Transmissions.isAutomatic]). */
    val manual: Boolean? = null,
    val takes: WhatItTakes,
) {
    fun fits(drivetrain: String?, transmission: String?): Boolean =
        (drive == null || Drivetrains.kind(drivetrain) == drive) &&
            (manual == null || if (manual) Transmissions.isManual(transmission) else Transmissions.isAutomatic(transmission))
}

/**
 * What a maker's own documents say a model takes (oil, fluids, tires, plugs, bulbs), shipped with the
 * app for one model, years and, where needed, engine. [id] is kept on the vehicle once anything is
 * filled from it (`vehicles.takesKnownList`).
 */
data class KnownTakesList(
    val id: String,
    /** "2017–2024 4Runner" */
    val title: String,
    val make: String,
    val model: String,
    val years: IntRange,
    val source: KnownSpecSource,
    /** What the whole model takes, whatever its drive or gearbox. */
    val takes: WhatItTakes,
    /** The tire sizes the documents list for the model, by trim or option. Offered to pick from, never filled when there are several. */
    val tireSizes: List<String> = emptyList(),
    val variants: List<KnownTakesVariant> = emptyList(),
    /** Null for a list that is for every engine the model had in those years. */
    val engine: KnownEngine? = null,
)

sealed interface KnownTakesFile {
    data class Ok(val list: KnownTakesList) : KnownTakesFile
    data class Invalid(val errors: List<ImportError>) : KnownTakesFile
}

/** What filling a vehicle's rows from a list would do. Nothing the owner has entered is ever written over. */
data class KnownTakesPlan(
    /** The vehicle's rows with [filled] taken from the list. */
    val result: WhatItTakes,
    /** Rows with nothing entered that the list has something for. */
    val filled: List<TakesItem>,
    /** Rows the list has something for that the owner has already entered. They stay exactly as they are. */
    val kept: List<TakesItem>,
)

/**
 * "What it takes" from the maker's own documents.
 *
 * **Glovebox has no data of its own on what a vehicle takes.** A bundled list is what a named
 * manufacturer document prints for one model and run of years ([KnownSpecSource]), read and typed in
 * by the maintainer, and is offered only for that model. Filling from it writes only rows the owner
 * left empty, and every row it fills is marked to double-check until the owner has checked it against
 * their own manual or changed it ([stillToCheck]).
 *
 * A list is a file in `assets/known-takes/`. Every value in it has to be written exactly as the
 * row's own editor would store it ([VehicleSpecEntry]), so what is filled reads back as if typed.
 */
object KnownTakes {
    const val ASSET_DIR = "known-takes"

    private val required = KnownLists.headerKeys + "takes"
    private val keys = required + setOf("variants", "engine")

    private val drives = mapOf("2WD" to DriveKind.TWO_WHEEL, "4WD" to DriveKind.FOUR_WHEEL, "AWD" to DriveKind.ALL_WHEEL)
    private val gearboxes = mapOf("manual" to true, "automatic" to false)

    fun parse(text: String): KnownTakesFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return KnownTakesFile.Invalid(listOf(ImportError("", "This isn't a JSON file.")))
        }
        val check = ImportCheck()
        val o = check.obj(root, "", keys, required = required) ?: return KnownTakesFile.Invalid(check.invalid().errors)
        val header = KnownLists.header(o, check)
        val sizes = mutableListOf<String>()
        val takes = o["takes"]?.let { takes(it, "takes", check, sizes) }
        val variants = o["variants"]?.let { v ->
            check.array(v, "variants", 1, 10)?.mapIndexedNotNull { i, e ->
                val path = ImportCheck.at("variants", i)
                check.obj(e, path, setOf("drive", "gearbox", "takes"), required = setOf("takes"))?.let { vo ->
                    if ("drive" !in vo && "gearbox" !in vo) check.fail(path, "needs a \"drive\" or a \"gearbox\"; what is for the whole model goes in \"takes\"")
                    val drive = vo["drive"]?.let { check.choice(it, ImportCheck.at(path, "drive"), drives) }
                    val manual = vo["gearbox"]?.let { check.choice(it, ImportCheck.at(path, "gearbox"), gearboxes) }
                    val t = vo["takes"]?.let { takes(it, ImportCheck.at(path, "takes"), check, sizes) }
                    if (t != null) KnownTakesVariant(drive, manual, t) else null
                }
            }
        }.orEmpty()
        val engine = o["engine"]?.let { KnownLists.engine(it, check) }
        if (takes != null) {
            variants.forEachIndexed { i, v ->
                TakesItem.entries.filter { takes.summary(it) != null && v.takes.summary(it) != null }.forEach {
                    check.fail("variants[$i].takes", "gives ${it.key}, which \"takes\" already gives for the whole model")
                }
                // Two variants that could both fit one vehicle mustn't both say the same row.
                variants.forEachIndexed { j, w ->
                    val exclusive = (v.drive != null && w.drive != null && v.drive != w.drive) || (v.manual != null && w.manual != null && v.manual != w.manual)
                    if (j > i && !exclusive) {
                        TakesItem.entries.filter { v.takes.summary(it) != null && w.takes.summary(it) != null }.forEach {
                            check.fail("variants[$j].takes", "gives ${it.key}, which variants[$i] also gives, and one vehicle could fit both")
                        }
                    }
                }
            }
        }
        if (check.failed || header == null || takes == null) return KnownTakesFile.Invalid(check.invalid().errors)
        return KnownTakesFile.Ok(KnownTakesList(header.id, header.title, header.make, header.model, header.years, header.source, takes, sizes.distinct(), variants, engine))
    }

    private val TakesItem.key: String
        get() = when (this) {
            TakesItem.OIL -> "oil"
            TakesItem.TIRES -> "tires"
            TakesItem.WIPERS -> "wipers"
            TakesItem.TRANSMISSION -> "transmission"
            TakesItem.DIFFERENTIAL -> "differential"
            TakesItem.TRANSFER_CASE -> "transferCase"
            TakesItem.COOLANT -> "coolant"
            TakesItem.BRAKE_FLUID -> "brakeFluid"
            TakesItem.AIR_FILTERS -> "airFilters"
            TakesItem.SPARK_PLUGS -> "sparkPlugs"
            TakesItem.BATTERY -> "battery"
            TakesItem.BULBS -> "bulbs"
        }

    /** A `takes` block. A tire size is collected into [sizes]; it fills the row only when it is the only one. */
    private fun takes(e: JsonElement, path: String, check: ImportCheck, sizes: MutableList<String>): WhatItTakes? {
        val o = check.obj(e, path, TakesItem.entries.map { it.key }.toSet()) ?: return null
        if (o.isEmpty()) check.fail(path, "must give at least one row")
        fun at(vararg p: String) = p.fold(path) { acc, f -> ImportCheck.at(acc, f) }

        /** Text that the row's editor would store exactly as it is written here. */
        fun stored(e: JsonElement?, p: String, form: (String) -> String?): String? {
            val s = e?.let { check.string(it, p, 60) } ?: return null
            val f = form(s)
            return when {
                f == null -> null.also { check.fail(p, "\"$s\" isn't something this row takes") }
                f != s -> null.also { check.fail(p, "write it as Glovebox stores it: \"$f\"") }
                else -> s
            }
        }
        var w = WhatItTakes()
        o["oil"]?.let { oe ->
            check.obj(oe, at("oil"), setOf("grade", "approval", "capacity", "capacityUnit", "filter"))?.let { oil ->
                if ("capacity" in oil != "capacityUnit" in oil) check.fail(at("oil"), "gives \"capacity\" and \"capacityUnit\" together")
                val unit = oil["capacityUnit"]?.let { check.choice(it, at("oil", "capacityUnit"), VehicleSpecEntry.Volume.entries.associateBy { u -> u.symbol }) }
                val capacity = oil["capacity"]?.let { c ->
                    check.number(c, at("oil", "capacity"))?.also { n ->
                        if (unit != null && VehicleSpecEntry.oilCapacity(n.toString(), unit) != n) check.fail(at("oil", "capacity"), "must be from 1 to ${unit.max} ${unit.symbol}, to one decimal place")
                    }
                }
                w = w.copy(
                    oilGrade = stored(oil["grade"], at("oil", "grade"), VehicleSpecEntry::oilGrade),
                    oilApproval = stored(oil["approval"], at("oil", "approval"), VehicleSpecEntry::oilApproval),
                    oilCapacity = capacity.takeIf { unit != null },
                    oilCapacityUnit = unit.takeIf { capacity != null },
                    oilFilter = stored(oil["filter"], at("oil", "filter"), VehicleSpecEntry::partNumbers),
                )
            }
        }
        o["tires"]?.let { te ->
            check.obj(te, at("tires"), setOf("sizes", "pressure"))?.let { tires ->
                val listed = tires["sizes"]?.let { s ->
                    check.array(s, at("tires", "sizes"), 1, 12)?.mapIndexedNotNull { i, size -> stored(size, ImportCheck.at(at("tires", "sizes"), i), VehicleSpecEntry::tireSize) }
                }.orEmpty()
                sizes += listed
                // Given only where the document gives one pressure for every size it lists; one that differs by size can't be filled.
                val pressure = tires["pressure"]?.let { check.obj(it, at("tires", "pressure"), setOf("front", "rear", "spare", "unit"), required = setOf("unit")) }
                val unit = pressure?.get("unit")?.let { check.choice(it, at("tires", "pressure", "unit"), VehicleSpecEntry.Pressure.entries.associateBy { u -> u.symbol }) }
                fun psi(key: String) = pressure?.get(key)?.let { v -> unit?.let { u -> check.int(v, at("tires", "pressure", key), u.range.first, u.range.last) } }
                w = w.copy(
                    tireSize = listed.singleOrNull(),
                    tirePressureFront = psi("front"),
                    tirePressureRear = psi("rear"),
                    tirePressureSpare = psi("spare"),
                    tirePressureUnit = unit,
                )
            }
        }
        o["wipers"]?.let { we ->
            check.obj(we, at("wipers"), setOf("driver", "passenger", "rear"))?.let { wipers ->
                fun inches(key: String) = wipers[key]?.let { check.int(it, at("wipers", key), 10, 34) }
                w = w.copy(wiperDriver = inches("driver"), wiperPassenger = inches("passenger"), wiperRear = inches("rear"))
            }
        }
        w = w.copy(
            transmissionFluid = stored(o["transmission"], at("transmission"), VehicleSpecEntry::fluidName),
            differentialOil = stored(o["differential"], at("differential"), VehicleSpecEntry::gearOil),
            transferCaseOil = stored(o["transferCase"], at("transferCase"), VehicleSpecEntry::gearOil),
            coolant = stored(o["coolant"], at("coolant"), VehicleSpecEntry::fluidName),
            brakeFluid = stored(o["brakeFluid"], at("brakeFluid"), VehicleSpecEntry::brakeFluid),
        )
        o["airFilters"]?.let { ae ->
            check.obj(ae, at("airFilters"), setOf("engine", "cabin"))?.let { a ->
                w = w.copy(
                    engineAirFilter = stored(a["engine"], at("airFilters", "engine"), VehicleSpecEntry::partNumbers),
                    cabinAirFilter = stored(a["cabin"], at("airFilters", "cabin"), VehicleSpecEntry::partNumbers),
                )
            }
        }
        o["sparkPlugs"]?.let { se ->
            check.obj(se, at("sparkPlugs"), setOf("part", "gap"))?.let { sp ->
                w = w.copy(
                    sparkPlug = stored(sp["part"], at("sparkPlugs", "part"), VehicleSpecEntry::partNumbers),
                    sparkPlugGap = stored(sp["gap"], at("sparkPlugs", "gap"), VehicleSpecEntry::plugGap),
                )
            }
        }
        o["battery"]?.let { be ->
            check.obj(be, at("battery"), setOf("group", "cca"))?.let { b ->
                w = w.copy(
                    batteryGroup = stored(b["group"], at("battery", "group"), VehicleSpecEntry::batteryGroup),
                    batteryCca = b["cca"]?.let { check.int(it, at("battery", "cca"), 100, 1500) },
                )
            }
        }
        o["bulbs"]?.let { be ->
            check.obj(be, at("bulbs"), setOf("low", "high", "fog"))?.let { b ->
                w = w.copy(
                    bulbLow = stored(b["low"], at("bulbs", "low"), VehicleSpecEntry::bulbCode),
                    bulbHigh = stored(b["high"], at("bulbs", "high"), VehicleSpecEntry::bulbCode),
                    bulbFog = stored(b["fog"], at("bulbs", "fog"), VehicleSpecEntry::bulbCode),
                )
            }
        }
        // An object that is there but says nothing (`"bulbs": {}`) is a mistake, not an empty row.
        TakesItem.entries.filter { it.key in o && w.summary(it) == null && (o[it.key] as? JsonObject)?.isEmpty() == true }
            .forEach { check.fail(at(it.key), "must give at least one value") }
        // Several sizes and nothing else make no row of their own: they are offered to pick from.
        val onlySizes = o["tires"] is JsonObject && w.summary(TakesItem.TIRES) == null && sizes.size > 1
        if (!check.failed && TakesItem.entries.none { w.summary(it) != null } && !onlySizes) check.fail(path, "must give at least one row")
        return w
    }

    private fun forModel(lists: List<KnownTakesList>, year: Int, make: String, model: String) =
        lists.filter { year in it.years && KnownLists.same(it.make, make) && KnownLists.same(it.model, model) }

    /**
     * The list for a vehicle, or null: its make and model, a year whose documents were read, and its
     * engine where the model has a list per engine (one that isn't on record matches none). No near
     * misses: an oil for the wrong engine is worse than an empty row.
     */
    fun match(lists: List<KnownTakesList>, year: Int, make: String, model: String, engine: String? = null, fuel: String? = null): KnownTakesList? =
        KnownLists.forEngine(forModel(lists, year, make, model), KnownTakesList::engine, engine, fuel)

    /** What [list] says for a vehicle with this drive and gearbox: the whole model's rows and the variants it is known to fit. */
    fun forVehicle(list: KnownTakesList, drivetrain: String?, transmission: String?): WhatItTakes =
        list.variants.filter { it.fits(drivetrain, transmission) }.fold(list.takes) { acc, v ->
            TakesItem.entries.filter { v.takes.summary(it) != null }.fold(acc) { a, item -> a.with(item, v.takes) }
        }

    /** What filling [current] from [known] would do: each row the owner left empty takes the list's, whole; nothing else changes. */
    fun plan(known: WhatItTakes, current: WhatItTakes): KnownTakesPlan {
        val offered = TakesItem.entries.filter { known.summary(it) != null }
        val (filled, kept) = offered.partition { current.summary(it) == null }
        return KnownTakesPlan(filled.fold(current) { acc, item -> acc.with(item, known) }, filled, kept)
    }

    /**
     * Whether a row filled from a list is still to be checked once its editor is saved: it is until
     * the owner says they have checked it or changes it, and an emptied row has nothing to check.
     * [before] and [after] are the row's line before and after.
     */
    fun stillToCheck(before: List<TakesPart>?, after: List<TakesPart>?, checked: Boolean): Boolean =
        !checked && after != null && after == before
}

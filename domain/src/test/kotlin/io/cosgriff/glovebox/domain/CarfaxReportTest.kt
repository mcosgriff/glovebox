package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A made-up report laid out as Carfax's are: date at x 55, mileage at 129, source at 182, comments at 356,
 * lines 13 points apart. Nothing here is a real vehicle's history.
 */
class CarfaxReportTest {
    private val today = LocalDate.of(2026, 10, 15)

    private class Page(val page: Int) {
        val words = mutableListOf<PdfWord>()
        var y = 100f
        fun at(x: Float, text: String) = text.split(" ").fold(x) { left, w -> words += PdfWord(page, left, y, w); left + w.length * 5.5f + 4f }
        /** One history entry: the first line has the date, mileage, the source's first line and the first comment. */
        fun entry(date: String, miles: String?, source: List<String>, comments: List<String>) {
            val rows = maxOf(source.size, comments.size)
            for (r in 0 until rows) {
                if (r == 0) { at(55f, date); miles?.let { at(129f, it) } }
                source.getOrNull(r)?.let { at(182f, it) }
                comments.getOrNull(r)?.let { at(356f, it) }
                y += 13f
            }
            y += 26f
        }
    }

    private fun report(build: Page.() -> Unit): List<PdfWord> = Page(1).apply {
        at(55f, "CARFAX Vehicle History Report")
        y += 40f
        build()
    }.words

    private fun records(words: List<PdfWord>) = (CarfaxReport.read(words, today) as ImportFile.ServiceHistory).items

    @Test
    fun `service visits come out as records, and titles and registrations don't`() {
        val words = report {
            entry("04/02/2019", null, listOf("Ohio", "Motor Vehicle Dept."), listOf("Title issued or updated", "New owner reported"))
            entry("06/14/2019", "12,480", listOf("Acme Motors", "Springfield, OH", "555-010-0000"), listOf("Oil and �lter changed", "Tires rotated", "Brakes checked"))
            entry("01/09/2020", "18,002", listOf("Corner Garage", "Springfield, OH"), listOf("Belts and hoses checked"))
            entry("03/03/2020", "19,250", listOf("Ohio", "Motor Vehicle Dept."), listOf("Registration issued or renewed"))
        }
        val r = records(words)
        assertEquals(2, r.size)
        val first = r[0]
        assertEquals(LocalDate.of(2019, 6, 14), first.date)
        assertEquals(12_480, first.odometer)
        assertEquals("Acme Motors", first.shopName)
        assertEquals(false, first.diy)
        assertEquals(
            listOf(ImportedWork("Oil and filter changed", "oil"), ImportedWork("Tires rotated", "rotation"), ImportedWork("Brakes checked", "brake-inspect")),
            first.work,
        )
        // Work the wording doesn't name exactly is kept as written.
        assertEquals(listOf(ImportedWork("Belts and hoses checked", null)), r[1].work)
    }

    @Test
    fun `a comment that wraps is one item, and boxes after an entry aren't part of it`() {
        val words = report {
            entry("05/05/2021", "30,100", listOf("Acme Motors"), listOf("Transmission fluid", "changed"))
            // An owner summary further down, with numbers in the comment column.
            at(80f, "Purchased: 2021")
            at(480f, "12,000 mi/yr")
            y += 40f
            entry("06/06/2021", "31,000", listOf("Acme Motors"), listOf("Wheels balanced"))
        }
        val r = records(words)
        assertEquals(listOf(ImportedWork("Transmission fluid changed", "trans-fluid")), r[0].work)
        assertEquals(listOf(ImportedWork("Wheels balanced", "balance")), r[1].work)
    }

    @Test
    fun `a visit without a mileage, or in the future, isn't taken`() {
        val words = report {
            entry("07/07/2022", null, listOf("Acme Motors"), listOf("Vehicle serviced"))
            entry("08/08/2022", "40,000", listOf("Acme Motors"), listOf("Vehicle serviced"))
            entry("01/01/2030", "90,000", listOf("Acme Motors"), listOf("Vehicle serviced"))
        }
        assertEquals(listOf(40_000), records(words).map { it.odometer })
        val onlyWithout = report { entry("07/07/2022", null, listOf("Acme Motors"), listOf("Vehicle serviced")) }
        val refused = CarfaxReport.read(onlyWithout, today) as ImportFile.Invalid
        assertEquals(CarfaxRefusal.NoVisits(withoutMileage = 1), refused.carfax)
    }

    @Test
    fun `a PDF that isn't a Carfax report, or has no visits, is refused with a reason`() {
        val notCarfax = Page(1).apply { at(55f, "Quarterly statement"); y += 20f; entry("06/14/2019", "12,480", listOf("Bank"), listOf("Payment received")) }.words
        assertEquals(CarfaxRefusal.NotAReport, (CarfaxReport.read(notCarfax, today) as ImportFile.Invalid).carfax)
        val noVisits = report { entry("04/02/2019", null, listOf("Ohio", "Motor Vehicle Dept."), listOf("Title issued or updated")) }
        assertTrue((CarfaxReport.read(noVisits, today) as ImportFile.Invalid).carfax is CarfaxRefusal.NoVisits)
    }

    @Test
    fun `wording that used to be custom is a service line`() {
        mapOf(
            "Water pump replaced" to "water-pump",
            "Water pump gasket replaced" to "water-pump",
            "Thermostat replaced" to "thermostat",
            "Radiator hose(s) replaced" to "coolant-hoses",
            "Radiator replaced" to "radiator",
            "Radiator cap replaced" to "radiator-cap",
            "Coolant reservoir replaced" to "coolant-reservoir",
            "Overflow tank replaced" to "coolant-reservoir",
            "Cooling fan motor replaced" to "cooling-fan",
            "Radiator fan replaced" to "cooling-fan",
            "Fan clutch replaced" to "fan-clutch",
            "Heater core replaced" to "heater-core",
            "Heater hose(s) replaced" to "coolant-hoses",
            "Cooling system checked" to "cooling-check",
            "Coolant level checked" to "cooling-check",
            "Brakes serviced" to "brake-service",
            "Brake caliper(s) replaced" to "calipers",
            "Brake line(s) replaced" to "brake-lines",
            "Rear brake shoes replaced" to "brake-shoes",
            "Brake drum(s) resurfaced" to "brake-drums",
            "Wheel cylinder(s) replaced" to "wheel-cylinders",
            "Brake master cylinder replaced" to "master-cylinder",
            "Master cylinder replaced" to "master-cylinder",
            "Brake booster replaced" to "brake-booster",
            "Parking brake adjusted" to "parking-brake",
            "Parking brake shoes replaced" to "parking-brake",
            "ABS sensor(s) replaced" to "abs-sensor",
            "Wheel speed sensor replaced" to "abs-sensor",
            "Tire condition and pressure checked" to "tire-check",
            "Tire pressure checked" to "tire-check",
            "Tire(s) inspected" to "tire-check",
            "Tire pressure monitoring system checked" to "tpms",
            "TPMS sensor(s) replaced" to "tpms",
            "90,000 mile service performed" to "scheduled-service",
            "30K service performed" to "scheduled-service",
            "Recommended maintenance performed" to "scheduled-service",
            "Window(s) checked" to "windows-check",
            "Windshield replaced" to "body",
            "Fluids checked" to "fluid-check",
            "Fluid levels checked" to "fluid-check",
            "All fluids topped off" to "fluid-check",
            "Washer fluid topped off" to "washer",
            "Ignition coil(s) replaced" to "ignition-coils",
            "Oxygen sensor(s) replaced" to "oxygen-sensor",
            "Valve cover gasket replaced" to "valve-cover-gasket",
            "Engine mount(s) replaced" to "engine-mounts",
            "Muffler replaced" to "muffler",
            "Muffler repaired" to "exhaust",
            "Catalytic converter(s) replaced" to "catalytic-converter",
            "Exhaust system repaired" to "exhaust",
            "Exhaust pipe replaced" to "exhaust",
            "Spark plug wires replaced" to "plug-wires",
            "Ignition wire set replaced" to "plug-wires",
            "Glow plug(s) replaced" to "glow-plugs",
            "Timing chain replaced" to "timing-chain",
            "Throttle body cleaned" to "throttle-body",
            "Fuel injector(s) replaced" to "fuel-injectors",
            "Fuel pump replaced" to "fuel-pump",
            // Cleaning them is the fuel system service; replacing them is the part.
            "Fuel injectors cleaned" to "fuel-system",
            "EGR valve replaced" to "egr-valve",
            "EVAP canister replaced" to "evap-system",
            "Evaporative emissions system repaired" to "evap-system",
            "Charcoal canister replaced" to "evap-system",
            "Mass air flow sensor replaced" to "engine-sensors",
            "Crankshaft position sensor replaced" to "engine-sensors",
            "Knock sensor(s) replaced" to "engine-sensors",
            "Turbocharger replaced" to "turbo",
            "Head gasket replaced" to "head-gasket",
            "Cylinder head gasket repaired" to "head-gasket",
            "Intake manifold gasket replaced" to "intake-gasket",
            "Oil pan gasket replaced" to "oil-pan-gasket",
            "Rear main seal replaced" to "rear-main-seal",
            "Starter replaced" to "starter",
            "Alternator replaced" to "alternator",
            "Air conditioning system recharged" to "air-conditioning",
            "A/C system checked" to "air-conditioning",
            "Clutch replaced" to "clutch",
            "U-joint(s) replaced" to "u-joints",
            "Suspension checked" to "suspension-check",
            "Steering and suspension inspected" to "suspension-check",
            "Lights checked" to "lights-check",
            "Exterior lights inspected" to "lights-check",
        ).forEach { (wording, id) -> assertEquals(wording, id, CarfaxReport.service(wording)) }
    }

    @Test
    fun `wording that doesn't say plainly what was done stays as written`() {
        listOf(
            // Both axles: two jobs.
            "Front and rear brake pads replaced",
            // A specific fluid with no line of its own is not "fluids".
            "Transmission fluid checked",
            "Check engine light checked",
            "Belts and hoses checked",
            "Vehicle serviced",
            "Emissions or safety inspection performed",
        ).forEach { assertNull(it, CarfaxReport.service(it)) }
        assertEquals("front-pads", CarfaxReport.service("Front brake pads replaced"))
        assertEquals("rear-pads", CarfaxReport.service("Rear brake pads replaced"))
        // Already its own line, before the general one.
        assertEquals("brake-fluid-check", CarfaxReport.service("Brake fluid checked"))
        assertEquals("wipers", CarfaxReport.service("Windshield wiper(s) replaced"))
    }

    @Test
    fun `a job and its gasket on one visit are one line`() {
        val work = listOf("Water pump replaced", "Water pump gasket replaced", "Thermostat replaced")
            .map { ImportedWork(it, CarfaxReport.service(it)) }
        assertEquals(
            listOf("Water pump gasket replaced" to "water-pump", "Thermostat replaced" to "thermostat"),
            CarfaxReport.oneLinePerService(work).map { it.label to it.service },
        )
    }

    @Test
    fun `a comment is work when something was done, and a service line only when the wording is plain`() {
        listOf("Oil and filter changed", "Brakes checked", "Maintenance inspection completed", "Emissions inspection performed", "Starter replaced")
            .forEach { assertTrue(it, CarfaxReport.isWork(it)) }
        listOf("Title issued or updated", "Registration issued or renewed", "New owner reported", "Odometer reading reported", "Vehicle color noted as White", "Damage reported")
            .forEach { assertTrue(it, !CarfaxReport.isWork(it)) }
        assertEquals("oil", CarfaxReport.service("Oil and filter changed"))
        assertEquals("cabin-air", CarfaxReport.service("Cabin air filter replaced"))
        assertEquals("engine-air", CarfaxReport.service("Air filter replaced"))
        assertEquals("front-pads", CarfaxReport.service("Front brake pads replaced"))
        assertEquals("multi-point", CarfaxReport.service("Maintenance inspection completed"))
        assertEquals("brake-inspect", CarfaxReport.service("Brakes checked"))
        assertEquals("brake-inspect", CarfaxReport.service("Brake system inspected"))
        assertEquals("brake-fluid-check", CarfaxReport.service("Brake fluid checked"))
        assertEquals("brake-fluid", CarfaxReport.service("Brake fluid flushed"))
        assertEquals("belt-check", CarfaxReport.service("Serpentine belt checked"))
        assertEquals("belt", CarfaxReport.service("Drive belt(s) replaced"))
        assertEquals("alignment", CarfaxReport.service("Four wheel alignment performed"))
        assertEquals("rear-diff", CarfaxReport.service("Rear differential serviced"))
        assertEquals("front-diff", CarfaxReport.service("Front differential fluid flushed/changed"))
        assertEquals("front-diff-check", CarfaxReport.service("Front differential fluid checked"))
        assertEquals("diff-fluid", CarfaxReport.service("Differential serviced"))
        assertEquals("diff-fluid", CarfaxReport.service("Differential fluid flushed/changed"))
        assertEquals("transfer-case", CarfaxReport.service("Transfer case fluid flushed/changed"))
        assertEquals("timing-belt", CarfaxReport.service("Timing belt replaced"))
        assertEquals("fuel-filter", CarfaxReport.service("Fuel filter replaced"))
        assertEquals("fuel-system", CarfaxReport.service("Fuel injection system cleaned"))
        assertEquals("battery-check", CarfaxReport.service("Battery/charging system checked"))
        assertEquals("battery", CarfaxReport.service("Battery replaced"))
        assertEquals("shocks", CarfaxReport.service("Shock absorber(s)/strut(s) replaced"))
        assertEquals("wheel-bearing", CarfaxReport.service("Wheel bearing(s) replaced"))
        assertEquals("steering-parts", CarfaxReport.service("Tie rod end(s) replaced"))
        assertEquals("safety-inspection", CarfaxReport.service("Safety inspection performed"))
        assertNull(CarfaxReport.service("Emissions or safety inspection performed"))
        // Which axle? Its own line, never taken for front or rear.
        assertEquals("brake-pads", CarfaxReport.service("Brake pads replaced"))
        assertNull(CarfaxReport.service("Battery cable(s) replaced"))
        assertNull(CarfaxReport.service("Vehicle serviced"))
    }

    @Test
    fun `one job named twice in a visit is one line, worded as the longer`() {
        assertEquals(
            listOf(ImportedWork("Four wheel alignment performed", "alignment"), ImportedWork("Starter replaced", null), ImportedWork("Oil changed", "oil")),
            CarfaxReport.oneLinePerService(
                listOf(
                    ImportedWork("Alignment performed", "alignment"),
                    ImportedWork("Starter replaced", null),
                    ImportedWork("Four wheel alignment performed", "alignment"),
                    ImportedWork("Oil changed", "oil"),
                ),
            ),
        )
    }

    private fun repaired(vararg words: String) = CarfaxReport.repairLigatures(words.toList())

    @Test
    fun `fi and fl drawn as one glyph are put back`() {
        assertEquals(
            listOf("filter", "fluid", "flushed", "Verified", "filter", "plain"),
            repaired("\uFFFDlter", "\uFFFDuid", "\uFFFDushed", "Veri\uFFFDed", "\uFB01lter", "plain"),
        )
    }

    @Test
    fun `ff, ffi and ffl are put back however the PDF names them`() {
        assertEquals(
            listOf("Differential", "Differential", "muffler", "muffler,", "efficiency", "traffic"),
            repaired("Di\uFB00erential", "Di\uFFFDerential", "mu\uFB04er", "mu\uFFFDer,", "e\uFFFDciency", "tra\uFB03c"),
        )
        // Named as letters: here "c" is "ff", "q" is "ffi" and "i" is "fl", each shown by two words.
        assertEquals(
            listOf("Differential", "buffed", "Officer", "efficiency", "Flywheel", "fluid/differential"),
            repaired("Dicerential", "buced", "Oqcer", "eqciency", "Iywheel", "iuid/dicerential"),
        )
        // "g" for "fi", in capitals and in a town's name.
        assertEquals(listOf("FILTER", "first", "Westfield,"), repaired("GLTER", "grst", "Westgeld,"))
    }

    @Test
    fun `a letter is only read as a ligature when the rest of the report shows it is one`() {
        // One word a letter away from a known one proves nothing.
        listOf("Dicerential", "Door", "Low", "did", "Wipers/washers", "Lex").forEach { assertEquals(listOf(it), repaired(it)) }
        // With "c" shown to be "fi", "D" in "Door" still isn't "fl", and ordinary words stay as they are.
        val ordinary = listOf(
            "Door", "Low", "did", "Wipers/washers", "Brakes", "changed", "inspected", "Toyota", "Oil", "and", "liter", "bulbs",
            "Volkswagen", "Tire", "Center", "Ocean", "120,000", "A/C", "Pre-delivery", "Coolant", "yield", "windshield",
        )
        assertEquals(listOf("filter", "first") + ordinary, repaired(*(listOf("clter", "crst") + ordinary).toTypedArray()))
        // "i" for "fi" doesn't make "yield" a field.
        assertEquals(listOf("filter", "first", "yield", "Springield"), repaired("ilter", "irst", "yield", "Springield"))
    }

    @Test
    fun `a report printed to PDF on a Mac reads the same`() {
        // There "fi" comes out as "c" and "fl" as "g", bullets stick to the first letter, and the page's
        // small print sits right under the last entry.
        val words = report {
            entry(
                "02/02/2023", "52,000", listOf("Acme Auto of Springceld"),
                listOf("V- ehicle serviced", "- Oil and clter changed", "Brake guid gushed/changed", "Spark plug(s) replaced", "Air clters replaced", "Four tires balanced", "Tire(s) balanced"),
            )
            at(182f, "Get the full picture at www.carfax.com.")
        }
        val r = records(words).single()
        assertEquals("Acme Auto of Springfield", r.shopName)
        assertEquals(
            listOf(
                ImportedWork("Vehicle serviced", null),
                ImportedWork("Oil and filter changed", "oil"),
                ImportedWork("Brake fluid flushed/changed", "brake-fluid"),
                ImportedWork("Spark plug(s) replaced", "spark-plugs"),
                ImportedWork("Air filters replaced", "engine-air"),
                // The same job named twice is one service line, in the wordier of the two.
                ImportedWork("Four tires balanced", "balance"),
            ),
            r.work,
        )
    }
}

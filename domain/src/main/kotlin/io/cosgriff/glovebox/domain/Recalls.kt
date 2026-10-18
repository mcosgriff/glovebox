package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** A safety recall as NHTSA lists it for a year, make and model. */
data class RecallNotice(
    /** NHTSA campaign number as sent, e.g. "20V682000". Identifies the recall. */
    val campaign: String,
    val component: String,
    val summary: String,
    val consequence: String?,
    val remedy: String?,
    val reportedOn: LocalDate?,
)

/**
 * A recall already kept for a vehicle. [done] is the user's own mark; NHTSA can't tell us. [archived]
 * is the user saying it doesn't apply to this vehicle; it isn't open, and can be moved back.
 */
data class StoredRecall(val id: Long, val notice: RecallNotice, val done: Boolean, val archived: Boolean = false) {
    val open: Boolean get() = !done && !archived
}

/** What a fresh list from NHTSA changes in the stored one. */
data class RecallChanges(
    val added: List<RecallNotice>,
    /** Stored id to the new wording. The user's done mark is untouched. */
    val updated: List<Pair<Long, RecallNotice>>,
    /** Ids of open recalls NHTSA no longer lists for this vehicle. */
    val withdrawn: List<Long>,
)

/** Open recalls for a banner or chip; [only] names the component when there is exactly one. */
data class OpenRecalls(val count: Int, val only: String?)

/** Where a make's owners look a VIN up. [name] is the make as people write it. */
data class ManufacturerSite(val name: String, val recallUrl: String)

object Recalls {
    /** The list is re-fetched when it is this old. */
    const val STALE_AFTER_DAYS = 7L

    /** NHTSA's own VIN lookup, for makes without a known manufacturer page. */
    const val NHTSA_LOOKUP_URL = "https://www.nhtsa.gov/recalls"

    private val reportDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Works out what to write after a refresh. Campaigns are matched by number, so the user's done
     * marks survive. A campaign NHTSA has dropped is removed only while it is still open: one the
     * user marked done stays, as a record of work on the vehicle.
     */
    fun merge(stored: List<StoredRecall>, fetched: List<RecallNotice>): RecallChanges {
        val listed = fetched.filter { it.campaign.isNotBlank() }.distinctBy { it.campaign }
        val known = stored.associateBy { it.notice.campaign }
        val campaigns = listed.map { it.campaign }.toSet()
        return RecallChanges(
            added = listed.filter { it.campaign !in known },
            updated = listed.mapNotNull { n -> known[n.campaign]?.takeIf { it.notice != n }?.let { it.id to n } },
            withdrawn = stored.filter { !it.done && it.notice.campaign !in campaigns }.map { it.id },
        )
    }

    /** NHTSA sends report dates as day/month/year ("25/10/2018"). Null if it isn't one. */
    fun parseReportDate(text: String?): LocalDate? = try {
        text?.trim()?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, reportDate) }
    } catch (_: DateTimeParseException) {
        null
    }

    /** "20V682000" reads "20V-682", the way NHTSA and dealers write it. */
    fun campaignLabel(campaign: String): String {
        val c = campaign.trim().uppercase()
        val wellFormed = c.length == 9 && c.take(2).all(Char::isDigit) && c[2].isLetter() && c.drop(3).all(Char::isDigit)
        return if (wellFormed && c.endsWith("000")) "${c.take(3)}-${c.substring(3, 6)}" else c
    }

    /**
     * NHTSA components are paths like "FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP"; the last part
     * that says something ("Fuel pump") is the name people recognise. NHTSA's words, in English as it
     * sends them; null when it sent none, and `app` says "Recall".
     */
    fun componentLabel(component: String): String? {
        val parts = component.split(':').map { it.trim() }.filter { it.isNotEmpty() }
        val part = parts.lastOrNull { !it.equals("OTHER", ignoreCase = true) } ?: parts.lastOrNull() ?: return null
        return part.lowercase().replaceFirstChar(Char::uppercase)
    }

    /** NHTSA looks recalls up by year, make and model, so all three have to be real. */
    fun canCheck(year: Int, make: String, model: String): Boolean =
        year > 0 && listOf(make, model).none { it.isBlank() || it.equals("Unknown", ignoreCase = true) }

    fun isStale(checkedOn: LocalDate?, today: LocalDate, afterDays: Long = STALE_AFTER_DAYS): Boolean =
        checkedOn == null || ChronoUnit.DAYS.between(checkedOn, today) >= afterDays

    /** Open recalls first, then archived, then done, each the most recently reported first. */
    fun sorted(recalls: List<StoredRecall>): List<StoredRecall> =
        recalls.sortedWith(
            compareBy<StoredRecall> { it.done }
                .thenBy { it.archived }
                .thenByDescending { it.notice.reportedOn ?: LocalDate.MIN }
                .thenBy { it.notice.campaign },
        )

    fun open(recalls: List<StoredRecall>): OpenRecalls {
        val open = sorted(recalls).filter { it.open }
        return OpenRecalls(open.size, open.singleOrNull()?.let { componentLabel(it.notice.component) })
    }

    // Only pages checked to exist; any other make gets NHTSA's lookup instead of a guessed address.
    private val sites: Map<String, ManufacturerSite> = listOf(
        ManufacturerSite("Toyota", "https://www.toyota.com/recall/"),
        ManufacturerSite("Lexus", "https://www.lexus.com/recall"),
        ManufacturerSite("Honda", "https://mygarage.honda.com/s/recall-search?brand=honda"),
        ManufacturerSite("Acura", "https://mygarage.honda.com/s/recall-search?brand=acura"),
        ManufacturerSite("Chevrolet", "https://www.chevrolet.com/ownercenter/recalls"),
        ManufacturerSite("GMC", "https://www.gmc.com/ownercenter/recalls"),
        ManufacturerSite("Buick", "https://www.buick.com/ownercenter/recalls"),
        ManufacturerSite("Cadillac", "https://www.cadillac.com/ownercenter/recalls"),
        ManufacturerSite("Subaru", "https://www.subaru.com/recalls.html"),
        ManufacturerSite("Nissan", "https://www.nissanusa.com/owners/recalls-vin.html"),
        ManufacturerSite("Infiniti", "https://www.infinitiusa.com/owners/recalls-vin.html"),
        ManufacturerSite("Hyundai", "https://autoservice.hyundaiusa.com/campaignhome"),
        ManufacturerSite("Kia", "https://owners.kia.com/us/en/recalls.html"),
        ManufacturerSite("Mazda", "https://www.mazdausa.com/owners/recalls"),
        ManufacturerSite("Volkswagen", "https://www.vw.com/en/owners-and-services/about-my-vehicle/recalls.html"),
        ManufacturerSite("Mercedes-Benz", "https://www.mbusa.com/en/recall"),
    ).associateBy { it.name.uppercase() }

    /** The manufacturer's own VIN recall lookup, if we know it. [make] as vPIC spells it ("TOYOTA"). */
    fun manufacturerSite(make: String): ManufacturerSite? = sites[make.trim().uppercase()]

    /** vPIC shouts makes ("TOYOTA", "LAND ROVER"); this is the make as people write it. */
    fun makeName(make: String): String = manufacturerSite(make)?.name
        ?: make.trim().lowercase().split(' ').filter { it.isNotEmpty() }.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}

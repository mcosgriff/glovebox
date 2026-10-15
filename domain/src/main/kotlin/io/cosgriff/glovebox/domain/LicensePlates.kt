package io.cosgriff.glovebox.domain

import java.util.Locale

/** Where a plate was issued: a U.S. state, D.C. or territory, or a Canadian province or territory. */
data class PlateRegion(val code: String, val name: String)

/**
 * A vehicle's license plate, as the owner types it. It is kept on the phone and never sent: nothing
 * is looked up from it. Only what a plate can hold is accepted, so a stray character can't slip in.
 */
object LicensePlates {
    /** Longer than any U.S. or Canadian plate, with room for a space or a dash. */
    const val MAX = 10

    /** U.S. first, then Canada, each in alphabetical order by name: how people look for theirs. */
    val regions: List<PlateRegion> = listOf(
        "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas", "CA" to "California",
        "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware", "DC" to "District of Columbia",
        "FL" to "Florida", "GA" to "Georgia", "HI" to "Hawaii", "ID" to "Idaho", "IL" to "Illinois",
        "IN" to "Indiana", "IA" to "Iowa", "KS" to "Kansas", "KY" to "Kentucky", "LA" to "Louisiana",
        "ME" to "Maine", "MD" to "Maryland", "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota",
        "MS" to "Mississippi", "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska", "NV" to "Nevada",
        "NH" to "New Hampshire", "NJ" to "New Jersey", "NM" to "New Mexico", "NY" to "New York",
        "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio", "OK" to "Oklahoma", "OR" to "Oregon",
        "PA" to "Pennsylvania", "RI" to "Rhode Island", "SC" to "South Carolina", "SD" to "South Dakota",
        "TN" to "Tennessee", "TX" to "Texas", "UT" to "Utah", "VT" to "Vermont", "VA" to "Virginia",
        "WA" to "Washington", "WV" to "West Virginia", "WI" to "Wisconsin", "WY" to "Wyoming",
        "AS" to "American Samoa", "GU" to "Guam", "MP" to "Northern Mariana Islands", "PR" to "Puerto Rico",
        "VI" to "U.S. Virgin Islands",
        "AB" to "Alberta", "BC" to "British Columbia", "MB" to "Manitoba", "NB" to "New Brunswick",
        "NL" to "Newfoundland and Labrador", "NT" to "Northwest Territories", "NS" to "Nova Scotia",
        "NU" to "Nunavut", "ON" to "Ontario", "PE" to "Prince Edward Island", "QC" to "Quebec",
        "SK" to "Saskatchewan", "YT" to "Yukon",
    ).map { (code, name) -> PlateRegion(code, name) }

    private val byCode = regions.associateBy { it.code }

    fun region(code: String?): PlateRegion? = code?.let(byCode::get)

    /**
     * The plate in capitals, inner spaces and dashes kept as typed (single), or null when nothing is
     * left. Anything but letters, digits, spaces and dashes is dropped, and it is cut to [MAX].
     */
    fun clean(text: String): String? = text.uppercase(Locale.US)
        .filter { it in 'A'..'Z' || it in '0'..'9' || it == ' ' || it == '-' }
        .replace(Regex(" +"), " ")
        .trimStart()
        .take(MAX)
        .trim()
        .ifEmpty { null }

    /** "CO · ABC 1234", "ABC 1234", or "Colorado" alone; null when neither is set. */
    fun label(plate: String?, regionCode: String?): String? =
        listOfNotNull(regionCode?.takeIf { plate != null }, plate ?: region(regionCode)?.name).joinToString(" · ").ifEmpty { null }
}

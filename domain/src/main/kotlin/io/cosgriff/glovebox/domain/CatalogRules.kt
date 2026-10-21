package io.cosgriff.glovebox.domain

import java.util.Locale

/** The user's rule that a line written [text] is the catalog line [catalogId] (#310). */
data class CatalogRule(val text: String, val catalogId: String)

/**
 * The user's own rules for filing a line of work under a catalog line, kept from sorting Custom lines so the
 * next import files that wording itself. **A rule matches the whole text only**, ignoring case, the spaces
 * round and between the words, and Carfax's "(s)": "Water pump gasket replaced" never catches "Water pump
 * inspected". Rules belong to the user, not to a vehicle, since a report words a job the same for every car.
 */
object CatalogRules {
    private val catalogIds: Set<String> = ServiceCatalog.categories.flatMap { it.items }.map { it.id }.toSet()

    /** What two texts must share to be the same: "Tire(s)  replaced " and "tires replaced" are one. */
    fun key(text: String): String = text.replace("(s)", "s").trim().replace(Regex("""\s+"""), " ").lowercase(Locale.ROOT)

    /**
     * Whether [rule]'s line is still in the catalog. One a later version dropped is kept, and shown as such,
     * but never applied: it isn't deleted for the user, and a line it no longer names can't be filed.
     */
    fun known(rule: CatalogRule): Boolean = rule.catalogId in catalogIds

    /** The catalog line [text] is filed under by one of [rules], or null. */
    fun match(text: String, rules: List<CatalogRule>): String? {
        val key = key(text)
        return rules.firstOrNull { known(it) && key(it.text) == key }?.catalogId
    }
}

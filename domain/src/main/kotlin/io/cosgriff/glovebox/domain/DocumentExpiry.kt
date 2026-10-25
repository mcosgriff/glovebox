package io.cosgriff.glovebox.domain

import java.time.LocalDate

/** How near a document's "Expires on" date is; `app` words and colours it. */
enum class ExpiryState {
    /** More than [DocumentExpiry.LEAD_DAYS] away. */
    LATER,

    /** Within [DocumentExpiry.LEAD_DAYS], up to and including the day itself. */
    SOON,

    /** The day has gone. */
    PAST,
}

/**
 * When a registration, an insurance policy or other paper runs out (#283). The date is the owner's own,
 * typed on the document; nothing is assumed for a document without one.
 */
object DocumentExpiry {
    /** Fixed: a setting for it would be a ticket of its own. */
    const val LEAD_DAYS = 30L

    fun state(expiresOn: LocalDate, today: LocalDate): ExpiryState = when {
        today.isAfter(expiresOn) -> ExpiryState.PAST
        !today.isBefore(expiresOn.minusDays(LEAD_DAYS)) -> ExpiryState.SOON
        else -> ExpiryState.LATER
    }

    /**
     * The documents whose expiry counts: for each type, the one with the latest date (the one added last
     * on a tie), so this year's registration quietly retires last year's. Each Other document counts on its own.
     */
    fun watched(documents: List<StoredDocument>): Set<Long> {
        val dated = documents.filter { it.expiresOn != null }
        val others = dated.filter { it.type == DocumentType.OTHER }
        val newest = dated.filter { it.type != DocumentType.OTHER }.groupBy { it.type }.values
            .map { ofType -> ofType.maxWith(compareBy<StoredDocument> { it.expiresOn }.thenBy { it.id }) }
        return (others + newest).map { it.id }.toSet()
    }
}

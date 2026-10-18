package io.cosgriff.glovebox.domain

/**
 * What the home-screen widget shows (#238). It works nothing out about reminders: each row is a vehicle's
 * [ComingUp], made exactly as the garage list makes it, and these only choose and name the rows.
 */
object HomeWidget {
    /** Rows on the widget; any vehicles past these are counted in [Rows.more]. */
    const val MAX_VEHICLES = 5

    /** [shown] in the garage list's order, then how many active vehicles didn't fit. */
    data class Rows<V>(val shown: List<V>, val more: Int)

    fun <V> rows(active: List<V>): Rows<V> =
        Rows(active.take(MAX_VEHICLES), (active.size - MAX_VEHICLES).coerceAtLeast(0))

    /**
     * How a row names its vehicle, so two of a model can't be confused: the year, make and model always,
     * with the nickname first when there is one ("Big Red · 2008 Ford F-150"). [title] is null when the
     * decode gave none of it, which `app` words as an unknown vehicle.
     */
    data class Name(val nickname: String?, val title: String?)

    fun name(nickname: String?, year: Int, make: String, model: String): Name =
        Name(nickname?.trim()?.ifEmpty { null }, VehicleText.title(year, make, model, trim = null))
}

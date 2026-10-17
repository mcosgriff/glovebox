package io.cosgriff.glovebox.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** Wording and tidying for what the user sees and types on Vehicle details. */
object VehicleText {
    const val MAX_NICKNAME = 30

    /** Long enough for "0W-20 · 6.2 qt with filter" or three wiper sizes. */
    const val MAX_SPEC = 60

    /** What gets stored for typed text: trimmed, single-spaced, capped; null when nothing is left. */
    fun clean(input: String, maxLength: Int): String? =
        input.trim().replace(Regex("\\s+"), " ").take(maxLength).trim().ifEmpty { null }

    /** "2019 Toyota 4Runner SR5", leaving out whatever the decode didn't give; null when none of it is known, which `app` then says. */
    fun title(year: Int, make: String, model: String, trim: String?): String? =
        listOfNotNull(
            year.takeIf { it > 0 }?.toString(),
            make.takeUnless(::unknown)?.let(Recalls::makeName),
            model.takeUnless(::unknown),
            trim?.takeUnless(::unknown),
        ).joinToString(" ").ifEmpty { null }

    private fun unknown(text: String) = text.isBlank() || text.equals("Unknown", ignoreCase = true)
}

enum class DrivingUse { NORMAL, SEVERE, MIXED }

/** [tracked] reminders, and which intervals they follow. */
data class ScheduleSummary(val tracked: Int, val severe: Int) {
    val use: DrivingUse
        get() = when (severe) {
            0 -> DrivingUse.NORMAL
            tracked -> DrivingUse.SEVERE
            else -> DrivingUse.MIXED
        }
}

object ScheduleSummaries {
    /** [items] are each schedule item's (tracked, uses its severe interval). Untracked items don't count. */
    fun of(items: List<Pair<Boolean, Boolean>>): ScheduleSummary {
        val tracked = items.filter { it.first }
        return ScheduleSummary(tracked.size, tracked.count { it.second })
    }
}

object PhotoSizing {
    /** Longest edge a stored vehicle photo keeps: plenty for a phone screen, small on disk. */
    const val MAX_EDGE = 1600

    /** [width] x [height] scaled down to fit [maxEdge] on its longest side; never scaled up. */
    fun fit(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }
}

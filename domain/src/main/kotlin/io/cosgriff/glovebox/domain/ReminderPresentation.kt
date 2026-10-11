package io.cosgriff.glovebox.domain

import java.time.temporal.ChronoUnit

/** The one figure shown for a reminder: the limit that is closest to deciding it. */
sealed interface DueHeadline {
    /** Never logged, so there is nothing to count from yet. */
    data object NotLogged : DueHeadline

    /** Positive = miles left, zero or negative = due / overdue. */
    data class Miles(val remaining: Int) : DueHeadline

    /** Positive = days left, zero or negative = due / overdue. */
    data class Days(val remaining: Long) : DueHeadline
}

/** [fraction] is how much of the interval has been used, 0..1; null when never logged. */
data class ReminderProgress(val headline: DueHeadline, val fraction: Double?)

/** A vehicle's reminders split the way the vehicle home screen shows them. */
data class ComingUp(
    /** Logged at least once, most urgent first. */
    val counting: List<Pair<ItemStatus, ReminderProgress>>,
    /** Tracked but never logged. The engine treats these as due; the UI lists them apart. */
    val notLogged: List<ItemStatus>,
) {
    val overdue: Int get() = counting.count { it.first.status.state == DueState.OVERDUE }
    val dueSoon: Int get() = counting.count { it.first.status.state == DueState.DUE_SOON }
}

object ReminderPresentation {

    /**
     * Picks the limit to show. When both mileage and time apply, it is the one further along
     * (or, for whichever-comes-last, the one lagging) — independent of driving pace, so a
     * vehicle with a single odometer reading still shows the right figure.
     */
    fun progress(s: ItemStatus): ReminderProgress {
        val last = s.lastDone ?: return ReminderProgress(DueHeadline.NotLogged, null)
        val status = s.status

        val miles = status.milesRemaining?.let { left ->
            val total = s.item.interval.miles!!
            DueHeadline.Miles(left) to 1.0 - left.toDouble() / total
        }
        val time = status.daysRemaining?.let { left ->
            val total = ChronoUnit.DAYS.between(last.date, status.dueByDate!!).coerceAtLeast(1)
            DueHeadline.Days(left) to 1.0 - left.toDouble() / total
        }

        val chosen = when {
            miles == null -> time
            time == null -> miles
            s.item.rule == DueRule.WHICHEVER_LAST -> if (miles.second <= time.second) miles else time
            else -> if (miles.second >= time.second) miles else time
        } ?: return ReminderProgress(DueHeadline.NotLogged, null)

        return ReminderProgress(chosen.first, chosen.second.coerceIn(0.0, 1.0))
    }

    fun comingUp(statuses: List<ItemStatus>): ComingUp {
        val (logged, never) = statuses.partition { it.lastDone != null }
        return ComingUp(
            counting = logged.map { it to progress(it) }
                .sortedWith(
                    compareBy<Pair<ItemStatus, ReminderProgress>> { it.first.status.state != DueState.OVERDUE }
                        .thenByDescending { it.second.fraction ?: 0.0 }
                        .thenBy { it.first.item.name },
                ),
            notLogged = never.sortedBy { it.item.name },
        )
    }
}

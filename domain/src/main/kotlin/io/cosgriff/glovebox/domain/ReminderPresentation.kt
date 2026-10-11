package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The one figure shown for a reminder: the limit that is closest to deciding it. */
sealed interface DueHeadline {
    /** Never logged, so there is nothing to count from yet. */
    data object NotLogged : DueHeadline

    /** Positive = miles left, zero or negative = due / overdue. */
    data class Miles(val remaining: Int) : DueHeadline

    /** Positive = days left, zero or negative = due / overdue. */
    data class Days(val remaining: Long) : DueHeadline

    /** Held back by the user; shown in place of how overdue it is. */
    data class Snoozed(val snooze: Snooze) : DueHeadline
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

    /** The soonest reminder that isn't overdue yet — the "Next:" line on a garage card. */
    val next: Pair<ItemStatus, ReminderProgress>?
        get() = counting.firstOrNull { it.first.status.state != DueState.OVERDUE && it.first.snooze == null }

    /** The one status worth a chip on a garage card; null when nothing has been logged yet. */
    val attention: Attention?
        get() = when {
            overdue > 0 -> attention(DueState.OVERDUE, overdue)
            dueSoon > 0 -> attention(DueState.DUE_SOON, dueSoon)
            counting.isNotEmpty() -> Attention(DueState.OK, 0, null)
            else -> null
        }

    private fun attention(state: DueState, count: Int) =
        Attention(state, count, counting.first { it.first.status.state == state }.first.item.name.takeIf { count == 1 })
}

/** [count] reminders in [state]; [only] names the reminder when there is exactly one. */
data class Attention(val state: DueState, val count: Int, val only: String?)

/** Sections of the Reminders tab, in the order they are shown. */
enum class ReminderGroup { OVERDUE, NEXT_30_DAYS, LATER, NOT_LOGGED }

/** A reminder shown outside its vehicle's own screen, so it carries the vehicle along. */
data class UrgentItem<V>(val vehicle: V, val status: ItemStatus, val progress: ReminderProgress)

object ReminderPresentation {

    /**
     * Picks the limit to show. When both mileage and time apply, it is the one further along
     * (or, for whichever-comes-last, the one lagging) — independent of driving pace, so a
     * vehicle with a single odometer reading still shows the right figure.
     */
    fun progress(s: ItemStatus): ReminderProgress {
        val last = s.lastDone ?: return ReminderProgress(DueHeadline.NotLogged, null)
        val status = s.status
        s.snooze?.let { return ReminderProgress(DueHeadline.Snoozed(it), null) }

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
            counting = logged.map { it to progress(it) }.sortedWith(urgency),
            notLogged = never.sortedBy { it.item.name },
        )
    }

    /**
     * The reminders closest to due across several vehicles, most urgent first. Never-logged
     * and snoozed reminders are left out.
     */
    fun <V> mostUrgent(vehicles: List<Pair<V, ComingUp>>, limit: Int = 3): List<UrgentItem<V>> =
        vehicles.flatMap { (vehicle, up) -> up.counting.filter { it.first.snooze == null }.map { vehicle to it } }
            .sortedWith(compareBy(urgency) { it.second })
            .take(limit)
            .map { (vehicle, item) -> UrgentItem(vehicle, item.first, item.second) }

    /**
     * Where a reminder goes on the Reminders tab. "Next 30 days" takes anything already due soon
     * and anything expected within 30 days — by date, or by mileage at the current driving pace.
     */
    fun group(s: ItemStatus, today: LocalDate): ReminderGroup = when {
        s.lastDone == null -> ReminderGroup.NOT_LOGGED
        s.snooze != null -> ReminderGroup.LATER
        s.status.state == DueState.OVERDUE -> ReminderGroup.OVERDUE
        s.status.state == DueState.DUE_SOON -> ReminderGroup.NEXT_30_DAYS
        s.status.estimatedDueDate?.let { !it.isAfter(today.plusDays(30)) } == true -> ReminderGroup.NEXT_30_DAYS
        else -> ReminderGroup.LATER
    }

    /**
     * Every vehicle's reminders by section, empty sections left out. Overdue is most urgent first;
     * the rest run soonest first, with reminders that have no date estimate after those that do.
     */
    fun <V> grouped(vehicles: List<Pair<V, ComingUp>>, today: LocalDate): Map<ReminderGroup, List<UrgentItem<V>>> {
        val counting = vehicles.flatMap { (vehicle, up) -> up.counting.map { vehicle to it } }
            .sortedWith(compareBy(urgency) { it.second })
            .map { (vehicle, item) -> UrgentItem(vehicle, item.first, item.second) }
        val notLogged = vehicles.flatMap { (vehicle, up) ->
            up.notLogged.map { UrgentItem(vehicle, it, ReminderProgress(DueHeadline.NotLogged, null)) }
        }
        val byGroup = (counting + notLogged).groupBy { group(it.status, today) }
        return ReminderGroup.entries.mapNotNull { g ->
            val items = byGroup[g] ?: return@mapNotNull null
            g to when (g) {
                // Stable sorts: ties keep the urgency order from above.
                ReminderGroup.NEXT_30_DAYS, ReminderGroup.LATER ->
                    items.sortedWith(
                        compareBy<UrgentItem<V>> { it.status.snooze != null }
                            .thenBy(nullsLast()) { it.status.status.estimatedDueDate },
                    )
                else -> items
            }
        }.toMap()
    }

    /** Overdue, then due soon, then whatever has used the most of its interval. */
    private val urgency: Comparator<Pair<ItemStatus, ReminderProgress>> =
        compareByDescending<Pair<ItemStatus, ReminderProgress>> { it.first.status.state.ordinal }
            .thenBy { it.first.snooze != null } // snoozed goes after everything still counting down
            .thenByDescending { it.second.fraction ?: 0.0 }
            .thenBy { it.first.item.name }
}

package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How often the app asks for a fresh odometer reading. */
enum class MileageCadence(val days: Long) { WEEKLY(7), TWO_WEEKS(14), MONTHLY(30) }

/** How often NHTSA is asked about recalls without the user asking. */
enum class RecallCadence(val days: Long?) {
    WEEKLY(7),
    MONTHLY(30),

    /** Only when the user taps Refresh. */
    NEVER(null),
}

/** What the user has asked to be told about, and when. */
data class NoticePrefs(
    val serviceDue: Boolean = true,
    val overdue: Boolean = true,
    val recalls: Boolean = true,
    val mileage: Boolean = true,
    /** Time of day for the daily check, 24-hour. */
    val hour: Int = 8,
    val minute: Int = 0,
    val mileageCadence: MileageCadence = MileageCadence.TWO_WEEKS,
) {
    /** True when the daily check has anything to do. */
    val wantsDailyCheck: Boolean get() = serviceDue || overdue || mileage
}

/**
 * What has already been said, so nothing is repeated every day.
 *
 * [dueAnnounced] maps a reminder to the service it was last announced for (see [NoticeRules.cycle]);
 * logging the service again starts a new cycle and a new announcement. The other two map to the
 * day of the last nudge.
 */
data class NoticeLog(
    val dueAnnounced: Map<Long, String> = emptyMap(),
    val overdueNudged: Map<Long, LocalDate> = emptyMap(),
    val mileageAsked: Map<Long, LocalDate> = emptyMap(),
)

/** One vehicle as the daily check sees it. Archived vehicles are never passed in. */
data class NoticeVehicle(
    val id: Long,
    /** "2019 4Runner" */
    val name: String,
    val statuses: List<ItemStatus>,
    /** The newest odometer reading of any kind; null if there has never been one. */
    val lastReadingOn: LocalDate?,
)

sealed interface Notice {
    val vehicleId: Long
    val vehicleName: String

    /** Reminders that have just entered their lead time. */
    data class ServiceDue(override val vehicleId: Long, override val vehicleName: String, val services: List<String>) : Notice

    /** Reminders that are past due and haven't been mentioned for a week. */
    data class Overdue(override val vehicleId: Long, override val vehicleName: String, val services: List<String>) : Notice

    /** Time for a fresh odometer reading. */
    data class MileageCheckIn(override val vehicleId: Long, override val vehicleName: String) : Notice

    /** Recalls NHTSA lists that weren't on file before; [components] are their short names. */
    data class NewRecalls(override val vehicleId: Long, override val vehicleName: String, val components: List<String>) : Notice
}

data class NoticeDecision(val notices: List<Notice>, val log: NoticeLog)

object NoticeRules {
    const val OVERDUE_NUDGE_DAYS = 7L

    /** Identifies one run of a reminder: it changes whenever the service is logged again. */
    fun cycle(lastDone: LastDone): String = "${lastDone.date.toEpochDay()}:${lastDone.odometer}"

    /** The run a reminder is in: since it was last logged, or its wait for the first time. */
    private fun cycle(s: ItemStatus): String = s.lastDone?.let(::cycle) ?: "first"

    /**
     * The daily check: what to say today, and the log to keep afterwards.
     *
     * - A reminder is announced once when it enters its lead time, per cycle.
     * - An overdue reminder is nudged when it first goes overdue and then weekly.
     * - A snoozed reminder reads as OK, so it stays quiet until the snooze passes.
     * - A reminder that was never logged isn't due in any way the user set up, so it is left out.
     * - A mileage check-in is asked for when the newest reading is older than the cadence, at most
     *   once per cadence, and only for vehicles with reminders to keep honest.
     *
     * The log is updated for everything that is due, even when its switch is off, so turning a
     * switch on later doesn't release a backlog.
     */
    fun daily(vehicles: List<NoticeVehicle>, log: NoticeLog, prefs: NoticePrefs, today: LocalDate): NoticeDecision {
        val notices = mutableListOf<Notice>()
        val dueAnnounced = mutableMapOf<Long, String>()
        val overdueNudged = mutableMapOf<Long, LocalDate>()
        val mileageAsked = mutableMapOf<Long, LocalDate>()

        for (v in vehicles) {
            val logged = v.statuses.filter { it.counting }

            val dueSoon = logged.filter { it.status.state == DueState.DUE_SOON }
            val newlyDue = dueSoon.filter { log.dueAnnounced[it.item.id] != cycle(it) }
            dueSoon.forEach { dueAnnounced[it.item.id] = cycle(it) }
            if (prefs.serviceDue && newlyDue.isNotEmpty()) notices += Notice.ServiceDue(v.id, v.name, newlyDue.map { it.item.name })

            val overdue = logged.filter { it.status.state == DueState.OVERDUE }
            val toNudge = overdue.filter { s -> log.overdueNudged[s.item.id]?.let { ChronoUnit.DAYS.between(it, today) >= OVERDUE_NUDGE_DAYS } ?: true }
            overdue.forEach { s -> overdueNudged[s.item.id] = if (s in toNudge) today else log.overdueNudged.getValue(s.item.id) }
            // Still inside its lead-time announcement for this cycle, so it isn't announced again if the state flips back.
            overdue.forEach { s -> log.dueAnnounced[s.item.id]?.let { dueAnnounced[s.item.id] = it } }
            if (prefs.overdue && toNudge.isNotEmpty()) notices += Notice.Overdue(v.id, v.name, toNudge.map { it.item.name })

            if (v.statuses.isNotEmpty()) {
                val cadence = prefs.mileageCadence.days
                val stale = v.lastReadingOn?.let { ChronoUnit.DAYS.between(it, today) >= cadence } ?: true
                val asked = log.mileageAsked[v.id]
                val askedRecently = asked != null && ChronoUnit.DAYS.between(asked, today) < cadence
                if (stale && !askedRecently) {
                    mileageAsked[v.id] = today
                    if (prefs.mileage) notices += Notice.MileageCheckIn(v.id, v.name)
                } else if (asked != null) {
                    mileageAsked[v.id] = asked
                }
            }
        }
        // Entries for reminders and vehicles that are no longer due, or gone, are dropped.
        return NoticeDecision(notices, NoticeLog(dueAnnounced, overdueNudged, mileageAsked))
    }

    /** Milliseconds from [nowMinuteOfDay] until the next [hour]:[minute], today or tomorrow. */
    fun minutesUntil(hour: Int, minute: Int, nowMinuteOfDay: Int): Int {
        val target = hour * 60 + minute
        return if (target > nowMinuteOfDay) target - nowMinuteOfDay else target + 24 * 60 - nowMinuteOfDay
    }
}

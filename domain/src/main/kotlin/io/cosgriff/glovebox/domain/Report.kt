package io.cosgriff.glovebox.domain

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * What only the PDF report needs about a vehicle, read with the rest when the export sheet opens.
 * Every odometer here is in dash terms, as the reminders count.
 */
data class ReportFacts(
    /** Every tracked item's status today (`VehicleReminders.statuses`), whatever dates the export covers. */
    val statuses: List<ItemStatus>,
    /** Each tracked item's group, by its id. */
    val itemCategories: Map<Long, ServiceGroup>,
    /** Every reading on record, the service records' included. */
    val readings: List<OdometerReading>,
)

/** "Maintenance current", or why not. Nothing is said about a vehicle with nothing tracked. */
sealed interface ReportStatus {
    data object Current : ReportStatus
    data class Overdue(val count: Int) : ReportStatus

    /** Nothing overdue, but [count] jobs have never been logged, so there is nothing to be current on. */
    data class ToLog(val count: Int) : ReportStatus
}

/** [completed] of [total] campaigns the owner has marked done; one marked as not applying isn't counted. */
data class RecallTally(val completed: Int, val total: Int)

data class ReportStats(
    /** The highest reading on record, as the dash shows it. */
    val odometer: Int?,
    /** Records in the export, a bare "last done" left out. */
    val services: Int,
    /** From the first record in the export to [odometer]; null with no record. */
    val sinceFirst: Int?,
    /** Null when the export carries no recalls. */
    val recalls: RecallTally?,
)

/** A service, as a mark on the report's chart. */
data class ChartVisit(val date: LocalDate, val odometer: Int)

/** One row of the chart: where its work was done, and the soonest of its jobs is next due ([nextDue]). */
data class ReportLane(val category: ServiceGroup, val done: List<Int>, val nextDue: Int?)

/** The chart of service by mileage: an axis from [low] to [high] marked at [ticks], with the odometer at [now]. */
data class ReportChart(
    val low: Int,
    val high: Int,
    val ticks: List<Int>,
    val now: Int?,
    val lanes: List<ReportLane>,
    /** Every service in the export, oldest first; the renderer labels as many as fit. */
    val visits: List<ChartVisit>,
)

enum class DueNextState { OVERDUE, DUE_SOON, SNOOZED, OK, JUST_DONE }

/** A row of "What's due next". [used] is the share of its interval gone, 0..1; it is due at [dueAt] or by [dueBy]. */
data class DueNextRow(
    val name: String,
    val category: ServiceGroup,
    val used: Double,
    val dueAt: Int?,
    val dueBy: LocalDate?,
    val state: DueNextState,
)

/** Average distance a month between the first and last reading in the export, with those readings to plot. */
data class MonthlyDistance(
    /** Rounded: it is an average, and is shown as one ("~1,500"). */
    val perMonth: Int,
    val from: LocalDate,
    val to: LocalDate,
    /** One per day, the highest, oldest first. */
    val points: List<OdometerReading>,
    val low: Int,
    val high: Int,
    val ticks: List<Int>,
)

/** A row of the report's schedule table; [nextDue] / [nextBy] are null for a job with nothing to count from. */
data class ReportScheduleRow(
    val name: String,
    val category: ServiceGroup,
    val every: String,
    /** As the odometer read that day. */
    val lastDone: LastDone?,
    val nextDue: Int?,
    val nextBy: LocalDate?,
)

/** Everything the PDF report shows for one vehicle. [vehicle] has already been through `Exports.document`. */
data class ReportModel(
    val vehicle: ExportVehicle,
    val generatedOn: LocalDate,
    /** Null when the export leaves out the schedule, or the vehicle tracks nothing. */
    val status: ReportStatus?,
    val stats: ReportStats,
    /** Null when there is nothing to plot. */
    val chart: ReportChart?,
    val dueNext: List<DueNextRow>,
    val recalls: List<ExportRecall>,
    val diy: Int,
    val shop: Int,
    val monthly: MonthlyDistance?,
    val schedule: List<ReportScheduleRow>,
) {
    /** Page 2's log: a visit's records together, newest first, as every export lays them out. */
    val log: List<List<ExportRecord>> get() = vehicle.entries
}

object Reports {
    /** "What's due next" shows at most this many. */
    const val DUE_NEXT = 6

    /** Under this share of its interval used, a job reads "Just done". */
    const val JUST_DONE = 0.1

    /** One model per vehicle in [doc]. */
    fun build(doc: ExportDocument): List<ReportModel> = doc.vehicles.map { model(it, doc.options, doc.exportedOn) }

    fun model(v: ExportVehicle, options: ExportOptions, today: LocalDate): ReportModel {
        val facts = v.report ?: ReportFacts(emptyList(), emptyMap(), emptyList())
        // The schedule and the recalls go together on the sheet; off, nothing about what's due is said.
        val statuses = facts.statuses.takeIf { options.scheduleAndRecalls }.orEmpty()
        val swap = v.clusterSwap
        val logged = v.records.filter { !it.enteredAsLastDone }
        val first = v.records.minWithOrNull(compareBy<ExportRecord> { it.date }.thenBy { it.odometer })
        val byId = statuses.associateBy { it.item.id }
        return ReportModel(
            vehicle = v,
            generatedOn = today,
            status = status(statuses),
            stats = ReportStats(
                odometer = v.odometer,
                services = logged.size,
                sinceFirst = first?.let { f -> v.odometer?.let { (it - swap.toDash(f.odometer, f.date)).coerceAtLeast(0) } },
                recalls = v.recalls.takeIf { it.isNotEmpty() }?.let { recalls ->
                    RecallTally(recalls.count { it.completedOn != null }, recalls.count { it.completedOn != null || it.archivedOn == null })
                },
            ),
            chart = chart(v, statuses, facts.itemCategories),
            dueNext = dueNext(statuses, facts.itemCategories),
            recalls = v.recalls,
            diy = logged.count { it.diy },
            shop = logged.count { !it.diy },
            monthly = monthly(facts.readings.filter { Exports.inRange(it.date, options.dates, today) }),
            schedule = v.schedule.map { line ->
                val s = line.itemId?.let(byId::get)?.takeIf { it.counting }
                ReportScheduleRow(
                    name = line.name,
                    category = line.itemId?.let(facts.itemCategories::get) ?: ServiceGroup.OTHER,
                    every = line.every,
                    lastDone = line.lastDone,
                    nextDue = s?.status?.dueAtOdometer,
                    nextBy = s?.status?.dueByDate?.takeIf { s.status.dueAtOdometer == null },
                )
            },
        )
    }

    /** Overdue first; then, as on a garage card, "current" only once every tracked job has something to count from. */
    fun status(statuses: List<ItemStatus>): ReportStatus? {
        if (statuses.isEmpty()) return null
        val coming = ReminderPresentation.comingUp(statuses)
        return when {
            coming.overdueNow.isNotEmpty() -> ReportStatus.Overdue(coming.overdueNow.size)
            coming.notLogged.isNotEmpty() -> ReportStatus.ToLog(coming.notLogged.size)
            else -> ReportStatus.Current
        }
    }

    /**
     * The jobs with something to count from, the furthest through their interval first. A snoozed job counts as
     * used up: the owner has held back one that was due. A job never logged gets no row; nothing is guessed for it.
     */
    fun dueNext(statuses: List<ItemStatus>, categories: Map<Long, ServiceGroup>): List<DueNextRow> =
        statuses.filter { it.counting }
            .map { s ->
                val used = ReminderPresentation.progress(s).fraction ?: 1.0
                val state = when {
                    s.snooze != null -> DueNextState.SNOOZED
                    s.status.state == DueState.OVERDUE -> DueNextState.OVERDUE
                    s.status.state == DueState.DUE_SOON -> DueNextState.DUE_SOON
                    s.lastDone != null && used < JUST_DONE -> DueNextState.JUST_DONE
                    else -> DueNextState.OK
                }
                DueNextRow(
                    name = s.item.name,
                    category = categories[s.item.id] ?: ServiceGroup.OTHER,
                    used = used,
                    dueAt = s.status.dueAtOdometer,
                    dueBy = s.status.dueByDate.takeIf { s.status.dueAtOdometer == null },
                    state = state,
                )
            }
            .sortedWith(compareByDescending<DueNextRow> { it.used }.thenBy { it.name })
            .take(DUE_NEXT)

    /** One lane per group that has work in the export or a job coming due; null when there is neither. */
    fun chart(v: ExportVehicle, statuses: List<ItemStatus>, categories: Map<Long, ServiceGroup>): ReportChart? {
        val swap = v.clusterSwap
        val done = mutableMapOf<ServiceGroup, MutableSet<Int>>()
        v.records.forEach { r ->
            val at = swap.toDash(r.odometer, r.date)
            r.categories.ifEmpty { listOf(ServiceGroup.OTHER) }.forEach { done.getOrPut(it) { sortedSetOf() } += at }
        }
        val next = statuses.filter { it.counting }
            .mapNotNull { s -> s.status.dueAtOdometer?.let { (categories[s.item.id] ?: ServiceGroup.OTHER) to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, at) -> at.min() }
        val lanes = ServiceGroup.entries.filter { it in done || it in next }
            .map { ReportLane(it, done[it].orEmpty().sorted(), next[it]) }
        if (lanes.isEmpty()) return null
        val values = lanes.flatMap { it.done + listOfNotNull(it.nextDue) } + listOfNotNull(v.odometer)
        val (low, high, ticks) = axis(values.min(), values.max())
        val visits = v.records
            .map { ChartVisit(it.date, swap.toDash(it.odometer, it.date)) }
            .distinct()
            .sortedWith(compareBy<ChartVisit> { it.date }.thenBy { it.odometer })
        return ReportChart(low, high, ticks, v.odometer, lanes, visits)
    }

    /** Null with fewer than two readings a week apart, or none that go up: there is no average to give. */
    fun monthly(readings: List<OdometerReading>): MonthlyDistance? {
        val perDay = ReminderEngine.milesPerDay(readings) ?: return null
        val points = readings.groupBy { it.date }.map { (date, same) -> OdometerReading(date, same.maxOf { it.odometer }) }.sortedBy { it.date }
        val (low, high, ticks) = axis(points.minOf { it.odometer }, points.maxOf { it.odometer })
        return MonthlyDistance(roundAverage(perDay * DAYS_PER_MONTH), points.first().date, points.last().date, points, low, high, ticks)
    }

    /** To the nearest 100 from 1,000 up, else the nearest 10: an average isn't worth more figures than that. */
    fun roundAverage(value: Double): Int {
        val step = if (value >= 1000) 100 else 10
        return (value / step).roundToInt() * step
    }

    /**
     * An axis around [min]..[max]: round tick values one "nice" step apart (1, 2, 2.5 or 5 times a power of ten,
     * picked for about four ticks), and ends a little past the data so no mark sits on the edge.
     */
    fun axis(min: Int, max: Int): Triple<Int, Int, List<Int>> {
        // A single value sits in the middle of an axis a tenth of its size.
        if (max <= min) {
            val half = maxOf(min / 20, 5)
            return axis(min - half, min + half)
        }
        val span = max - min
        val step = niceStep(span / 4.0)
        val pad = maxOf(span * 0.06, step * 0.2)
        val low = (min - pad).coerceAtLeast(0.0)
        val high = max + pad
        val firstTick = kotlin.math.ceil(low / step) * step
        val ticks = generateSequence(firstTick) { it + step }.takeWhile { it <= high }.map { it.roundToInt() }.toList()
        return Triple(low.roundToInt(), high.roundToInt(), ticks)
    }

    /** The smallest of 1, 2, 2.5, 5 times a power of ten that is at least [raw], and never under 1. */
    fun niceStep(raw: Double): Double {
        if (raw <= 1) return 1.0
        val power = 10.0.pow(floor(log10(raw)))
        return listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * power }.first { it >= raw - abs(raw) * 1e-9 }
    }

    private const val DAYS_PER_MONTH = 365.25 / 12
}

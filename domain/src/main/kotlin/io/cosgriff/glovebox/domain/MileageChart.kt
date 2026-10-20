package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * One reading on the chart. [plotted] is the distance really covered (dash terms plus a replaced
 * cluster's offset), so the line doesn't drop at a swap; [row] keeps the reading as typed, and
 * whether it is the odometer or one to check, exactly as the list says.
 */
data class ChartPoint(val row: ReadingRow, val plotted: Int) {
    val date: LocalDate get() = row.reading.date
    val doubtful: Boolean get() = row.outOfLine || row.jump != null
}

/**
 * What the chart draws. The x-axis runs from [start], the first reading in the range, to [end], the
 * last; the y-axis from [low] to [high]. [swapOn] is a cluster replacement inside the range.
 * [milesPerDay] is the reminders' own pace over the same readings, null when they can't give one;
 * [perYear] is that pace over a year, rounded, in the vehicle's unit.
 */
data class MileageSeries(
    val points: List<ChartPoint>,
    val start: LocalDate,
    val end: LocalDate,
    val low: Int,
    val high: Int,
    val swapOn: LocalDate?,
    val milesPerDay: Double?,
    val perYear: Int?,
) {
    val toCheck: Int get() = points.count { it.doubtful }
}

/** The odometer over time, from the same readings and flags as the Mileage readings list. */
object MileageChart {
    const val DAYS_A_YEAR = 365.25

    /**
     * The chart for [rows] (as `MileageReadings.rows` gives them, any order) over the last [years],
     * counted back from [today], or over every reading when it is null. Null with fewer than two
     * readings in the range: one point is no line.
     *
     * The pace is `ReminderEngine.milesPerDay` over the range's readings in dash terms, with a
     * replaced cluster's first reading among them as the reminders have it, so over every reading
     * the chart and the reminders can't disagree.
     */
    fun series(rows: List<ReadingRow>, swap: ClusterSwap?, years: Int?, today: LocalDate): MileageSeries? {
        val from = years?.let { today.minusYears(it.toLong()) }
        val inRange = rows.filter { from == null || !it.reading.date.isBefore(from) }
            .sortedWith(compareBy<ReadingRow> { it.reading.date }.thenBy { swap.toDash(it.reading.odometer, it.reading.date) })
        if (inRange.size < 2) return null
        val points = inRange.map { ChartPoint(it, swap.actual(swap.toDash(it.reading.odometer, it.reading.date))) }
        val dash = inRange.map { OdometerReading(it.reading.date, swap.toDash(it.reading.odometer, it.reading.date)) } +
            listOfNotNull(swap?.reading?.takeIf { from == null || !it.date.isBefore(from) })
        val pace = ReminderEngine.milesPerDay(dash)
        return MileageSeries(
            points = points,
            start = points.first().date,
            end = points.last().date,
            low = points.minOf { it.plotted },
            high = points.maxOf { it.plotted },
            swapOn = swap?.on?.takeIf { !it.isBefore(points.first().date) && !it.isAfter(points.last().date) },
            milesPerDay = pace,
            perYear = pace?.let(::perYear),
        )
    }

    /**
     * How many years back the readings go from [today], counting a part year as a whole one: the
     * longest range the chart can be asked for. At least 1.
     */
    fun yearsOnRecord(rows: List<ReadingRow>, today: LocalDate): Int {
        val first = rows.minOfOrNull { it.reading.date } ?: return 1
        val whole = ChronoUnit.YEARS.between(first, today).toInt()
        return (if (first.plusYears(whole.toLong()).isBefore(today)) whole + 1 else whole).coerceAtLeast(1)
    }

    /** A pace over a year: to the nearest 10 under 1,000, otherwise to the nearest 100. Never 0 for a vehicle that moved. */
    fun perYear(milesPerDay: Double): Int {
        val year = milesPerDay * DAYS_A_YEAR
        val step = if (year < 1_000) 10 else 100
        return ((year / step).roundToInt() * step).coerceAtLeast(step)
    }
}

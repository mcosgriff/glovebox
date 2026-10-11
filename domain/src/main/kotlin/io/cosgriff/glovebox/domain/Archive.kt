package io.cosgriff.glovebox.domain

import java.time.LocalDate

enum class SaleProblem { DATE_IN_FUTURE, ODOMETER_BELOW_LAST }

/** What "Mark as sold" was given. The odometer may be left empty for a vehicle with no mileage. */
data class Sale(val soldOn: LocalDate, val finalOdometer: Int?)

object Archive {
    /** What stops [sale] being saved; empty when it is fine. [lastOdometer] is the highest reading so far. */
    fun problems(sale: Sale, lastOdometer: Int?, today: LocalDate): Set<SaleProblem> = buildSet {
        if (sale.soldOn.isAfter(today)) add(SaleProblem.DATE_IN_FUTURE)
        if (sale.finalOdometer != null && lastOdometer != null && sale.finalOdometer < lastOdometer) add(SaleProblem.ODOMETER_BELOW_LAST)
    }

    /**
     * The odometer reading the sale adds to the log, dated the day it was sold. Null when it says
     * nothing new: no odometer given, or the same as the last reading.
     */
    fun finalReading(sale: Sale, lastOdometer: Int?): OdometerReading? =
        sale.finalOdometer?.takeIf { lastOdometer == null || it > lastOdometer }?.let { OdometerReading(sale.soldOn, it) }
}

package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ExportPhrase
import io.cosgriff.glovebox.domain.ExportWords
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.quantityText
import java.time.LocalDate

/** An export's words in the app's language, for both writers. */
object AppExportWords : ExportWords {
    override fun phrase(phrase: ExportPhrase, vararg args: String): String = AppText.res.getString(
        when (phrase) {
            ExportPhrase.ALL_TIME -> R.string.export_all_time
            ExportPhrase.DATE_RANGE -> R.string.export_date_range
            ExportPhrase.CLUSTER_SHOWS_MORE -> R.string.export_cluster_shows_more
            ExportPhrase.CLUSTER_SHOWS_LESS -> R.string.export_cluster_shows_less
            ExportPhrase.TOTAL -> R.string.export_total
            ExportPhrase.LAST_DONE_AT -> R.string.export_last_done_at
            ExportPhrase.NOT_LOGGED -> R.string.export_not_logged
            ExportPhrase.RECALL_MARKED_DONE -> R.string.export_recall_marked_done
            ExportPhrase.RECALL_ARCHIVED -> R.string.export_recall_archived
            ExportPhrase.RECALL_OPEN -> R.string.export_recall_open
            ExportPhrase.RECALL_NOTE -> R.string.export_recall_note
            ExportPhrase.LAST_DONE_NOTE -> R.string.export_last_done_note
            ExportPhrase.IMPORTED_NOTE -> R.string.export_imported_note
            ExportPhrase.VISIT -> R.string.export_visit
            ExportPhrase.VISIT_TOTAL -> R.string.export_visit_total
            ExportPhrase.JOB_LABOR -> R.string.export_job_labor
            ExportPhrase.JOB_PARTS -> R.string.export_job_parts
            ExportPhrase.LINES_JOINED -> R.string.export_lines_joined
            ExportPhrase.LABOR_ON_LINES -> R.string.export_labor_on_lines
            ExportPhrase.PARTS_ON_LINES -> R.string.export_parts_on_lines
            ExportPhrase.TAX_AND_FEES -> R.string.export_tax_and_fees
            ExportPhrase.LESS_THAN_LINES -> R.string.export_less_than_lines
            ExportPhrase.PART_QTY_EACH -> R.string.export_part_qty_each
            ExportPhrase.PART_QTY -> R.string.export_part_qty
            ExportPhrase.PART_EACH -> R.string.export_part_each
            ExportPhrase.SHOP_SUPPLIES -> R.string.export_shop_supplies
            ExportPhrase.TAX -> R.string.export_tax
            ExportPhrase.FEES -> R.string.export_fees
            ExportPhrase.OTHER_CHARGES -> R.string.export_other_charges
            ExportPhrase.SCHEDULE_NOTE -> R.string.export_schedule_note
            ExportPhrase.TAKES_NOTE -> R.string.export_takes_note
            ExportPhrase.DOCUMENTS_NOTE -> R.string.export_documents_note
            ExportPhrase.SERVICE_HISTORY -> R.string.export_service_history
            ExportPhrase.SERVICE_HISTORY_OF -> R.string.export_service_history_of
            ExportPhrase.LABELLED -> R.string.export_labelled
            ExportPhrase.NICKNAME -> R.string.export_nickname
            ExportPhrase.VIN -> R.string.export_vin
            ExportPhrase.PLATE -> R.string.export_plate
            ExportPhrase.ODOMETER -> R.string.export_odometer
            ExportPhrase.ODOMETER_HIGHEST -> R.string.export_odometer_highest
            ExportPhrase.ACTUAL_MILEAGE -> R.string.export_actual_mileage
            ExportPhrase.INSTRUMENT_CLUSTER -> R.string.export_instrument_cluster
            ExportPhrase.SOLD -> R.string.export_sold
            ExportPhrase.PERIOD -> R.string.export_period
            ExportPhrase.RECORDS -> R.string.export_records
            ExportPhrase.EXPORTED_ON -> R.string.export_exported_on
            ExportPhrase.WHAT_IT_TAKES -> R.string.export_what_it_takes
            ExportPhrase.DOCUMENTS_ON_FILE -> R.string.export_documents_on_file
            ExportPhrase.TYPE -> R.string.export_type
            ExportPhrase.NAME -> R.string.export_name
            ExportPhrase.DATE -> R.string.export_date
            ExportPhrase.EXPIRES -> R.string.export_expires
            ExportPhrase.SERVICE_RECORDS -> R.string.export_service_records
            ExportPhrase.NO_RECORDS -> R.string.export_no_records
            ExportPhrase.DONE_BY -> R.string.export_done_by
            ExportPhrase.DONE_BY_LABELLED -> R.string.export_done_by_labelled
            ExportPhrase.COST -> R.string.export_cost
            ExportPhrase.MAINTENANCE_SCHEDULE -> R.string.export_maintenance_schedule
            ExportPhrase.ITEM -> R.string.export_item
            ExportPhrase.EVERY -> R.string.export_every
            ExportPhrase.LAST_DONE -> R.string.export_last_done
            ExportPhrase.RECALLS -> R.string.export_recalls
            ExportPhrase.CAMPAIGN -> R.string.export_campaign
            ExportPhrase.COMPONENT -> R.string.export_component
            ExportPhrase.STATUS -> R.string.export_status
            ExportPhrase.PHOTO_UNREADABLE -> R.string.export_photo_unreadable
            ExportPhrase.PAGE -> R.string.export_page
        },
        *args,
    )

    override fun records(count: Int): String = AppText.res.getQuantityString(R.plurals.export_record_count, count, count.asNumber())

    override fun date(date: LocalDate): String = date.format(longDate)

    override fun distance(distance: Int, unit: DistanceUnit): String = distance.asDistance(unit)

    // Dollars and cents, whatever the language: costs are kept with no currency.
    override fun money(cents: Long): String = Money.format(cents)

    override fun quantity(milli: Long): String = quantityText(milli)
}

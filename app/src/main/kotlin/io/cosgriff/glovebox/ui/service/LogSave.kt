package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.KeptRecommendation
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.domain.DoneChange
import io.cosgriff.glovebox.domain.ServiceRecord
import io.cosgriff.glovebox.ui.AppText

/**
 * What saving Log service writes (#617), as the form stands ([LogServiceForm.toSave]). The ViewModel adds the photos
 * picked and the invoice's pages, which it holds, and hands it on to `RecordActions`.
 */
sealed interface LogSave {
    val record: ServiceRecord

    /** An edit: [record] in [original]'s place, the photo rows [removedPhotoIds] taken off. [done] is #593's. */
    data class Edit(
        val original: ServiceRecordWithItems,
        override val record: ServiceRecord,
        val removedPhotoIds: Set<Long>,
        val done: DoneChange,
    ) : LogSave

    /**
     * A new record, keeping [kept] for later and closing [done] of the shop's recommendations (#572). While
     * [keepInvoice] is on, an invoice's pages go with it as its photos and its PDF among the vehicle's documents, as
     * [invoiceName].
     */
    data class New(
        override val record: ServiceRecord,
        val kept: List<KeptRecommendation>,
        val done: List<Long>,
        val keepInvoice: Boolean,
    ) : LogSave {
        /** Paperwork of the vehicle's, named by the shop where there is one. */
        val invoiceName: String
            get() = record.shopName?.let { AppText.res.getString(R.string.invoice_document_name, it) }
                ?: AppText.res.getString(R.string.invoice_document_name_plain)
    }
}

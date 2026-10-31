package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.domain.StoredDocument
import io.cosgriff.glovebox.domain.VehicleDocuments

/** The saved row as the Documents screen, the reminders and exports read it. */
fun DocumentEntity.toStored() = StoredDocument(
    id = id,
    type = VehicleDocuments.type(type),
    name = name,
    date = date,
    format = VehicleDocuments.format(format),
    sizeBytes = sizeBytes,
    expiresOn = expiresOn,
)

package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.export.ExportNames
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.whatItTakes
import io.cosgriff.glovebox.ui.service.wordedLines

/** What the export builder names in the app's language. */
object AppExportNames : ExportNames {
    override val unknownVehicle: String get() = AppText.res.getString(R.string.unknown_vehicle)

    override val diy: String get() = AppText.res.getString(R.string.who_diy)

    override val aShop: String get() = AppText.res.getString(R.string.export_a_shop)

    override fun whatItTakes(vehicle: VehicleEntity): List<Pair<String, String>> = vehicle.whatItTakes().wordedLines()

    override fun documentType(type: DocumentType): String = type.label

    override fun recallComponent(component: String): String = io.cosgriff.glovebox.ui.service.recallComponent(component)

    override fun every(item: TrackedItem, unit: DistanceUnit): String = item.text(unit)
}

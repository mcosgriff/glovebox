package io.cosgriff.glovebox.ui.torque

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.KnownSpecList
import io.cosgriff.glovebox.domain.KnownSpecPlan
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueTemplate
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asNumber

/** "Engine & oil" */
val TorqueCategory.label: String
    get() = AppText.res.getString(
        when (this) {
            TorqueCategory.ENGINE -> R.string.torque_category_engine
            TorqueCategory.WHEELS -> R.string.torque_category_wheels
            TorqueCategory.DRIVELINE -> R.string.torque_category_driveline
            TorqueCategory.SUSPENSION -> R.string.torque_category_suspension
            TorqueCategory.OTHER -> R.string.torque_category_other
        },
    )

/** A starter row's name, as it is saved when the row is added. */
val TorqueTemplate.label: String
    get() = AppText.res.getString(
        when (key) {
            "oil-drain-plug" -> R.string.fastener_oil_drain_plug
            "oil-filter" -> R.string.fastener_oil_filter
            "spark-plugs" -> R.string.fastener_spark_plugs
            "lug-nuts" -> R.string.fastener_lug_nuts
            "front-caliper-bracket" -> R.string.fastener_front_caliper
            "rear-caliper-bracket" -> R.string.fastener_rear_caliper
            else -> R.string.fastener_transmission_drain_plug
        },
    )

/** "N·m", beside a field or a choice. */
val TorqueUnit.symbolWord: String
    get() = AppText.res.getString(
        when (this) {
            TorqueUnit.NEWTON_METER -> R.string.torque_unit_nm
            TorqueUnit.FOOT_POUND -> R.string.torque_unit_ft_lb
            TorqueUnit.INCH_POUND -> R.string.torque_unit_in_lb
        },
    )

/** "83 ft-lb", rounded the way a torque wrench is read. */
fun Torque.text(unit: TorqueUnit): String = AppText.res.getString(R.string.torque_value, rounded(unit).asNumber(), unit.symbolWord)

/** What is said where a list is offered: whose values they are, what they aren't, and what happens to the user's own rows. */
fun offer(list: KnownSpecList, plan: KnownSpecPlan): String {
    val res = AppText.res
    val offer = res.getString(
        R.string.known_specs_offer,
        res.getQuantityString(R.plurals.known_specs_values, plan.changes, plan.changes.asNumber()),
        list.source.name,
        list.source.note,
    )
    if (plan.kept.isEmpty()) return offer
    return res.getString(R.string.known_interval_offer_kept, offer, res.getQuantityString(R.plurals.known_specs_kept, plan.kept.size, plan.kept.size.asNumber()))
}

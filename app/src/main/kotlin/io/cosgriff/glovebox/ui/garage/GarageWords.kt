package io.cosgriff.glovebox.ui.garage

import android.content.res.Resources
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Attention
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.ui.service.ahead
import io.cosgriff.glovebox.ui.service.amount
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.until

// A garage card's status words. The home-screen widget (#238) shows the same, so they are plain functions
// over Resources rather than composables.

/** The chip: "Tire rotation overdue", "2 due soon", "3 to log", "Up to date". */
fun Attention.label(res: Resources): String = when {
    toLog > 0 -> res.getQuantityString(R.plurals.count_to_log, toLog, toLog.asNumber())
    else -> when (state) {
        DueState.OVERDUE -> only?.let { res.getString(R.string.item_overdue, it) }
            ?: res.getQuantityString(R.plurals.count_overdue, count, count.asNumber())
        DueState.DUE_SOON -> only?.let { res.getString(R.string.item_due_soon, it) }
            ?: res.getQuantityString(R.plurals.count_due_soon, count, count.asNumber())
        DueState.OK -> res.getString(R.string.up_to_date)
    }
}

/** "Next: Oil & filter in 1,688 mi", in the vehicle's own unit. */
fun ComingUp.nextLine(res: Resources, unit: DistanceUnit): String {
    next?.let { (status, progress) ->
        val name = status.item.name
        val h = progress.headline
        val amount = h.amount(unit)
        return when {
            amount != null && h.ahead == true -> res.getString(R.string.garage_next_in, name, amount)
            amount != null -> res.getString(R.string.garage_next_overdue, name, amount)
            h is DueHeadline.Miles -> res.getString(R.string.garage_next_due_now, name)
            h is DueHeadline.Days -> res.getString(R.string.garage_next_due_today, name)
            h is DueHeadline.Snoozed -> res.getString(R.string.garage_next_snoozed, name, h.snooze.until(unit))
            else -> res.getString(R.string.garage_next_not_logged, name)
        }
    }
    return when {
        counting.isNotEmpty() -> res.getString(R.string.garage_nothing_else)
        notLogged.isNotEmpty() -> res.getQuantityString(R.plurals.garage_not_logged, notLogged.size, notLogged.size.asNumber())
        else -> res.getString(R.string.garage_none_tracked)
    }
}

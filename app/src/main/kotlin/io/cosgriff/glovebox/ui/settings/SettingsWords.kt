package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.LicenceGroup
import io.cosgriff.glovebox.domain.LicenceName
import io.cosgriff.glovebox.domain.Licences
import io.cosgriff.glovebox.domain.LockDelay
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.PasswordStrength
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.andList
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.fileSize
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The words for Settings' values, the notifications and the backup, for screens and workers alike. */

val LockDelay.label: String
    get() = AppText.res.getString(
        when (this) {
            LockDelay.IMMEDIATELY -> R.string.lock_immediately
            LockDelay.ONE_MINUTE -> R.string.lock_one_minute
            LockDelay.FIVE_MINUTES -> R.string.lock_five_minutes
            LockDelay.ON_RESTART -> R.string.lock_on_restart
        },
    )

/** "Every 2 weeks", "Every 10 days": how often the mileage check-in asks. */
fun checkInLabel(days: Int): String = when (days) {
    7 -> AppText.res.getString(R.string.cadence_every_week)
    14 -> AppText.res.getString(R.string.cadence_every_two_weeks)
    30 -> AppText.res.getString(R.string.cadence_every_month)
    else -> AppText.res.getQuantityString(R.plurals.cadence_every_days, days, days.asNumber())
}

val RecallCadence.label: String
    get() = AppText.res.getString(
        when (this) {
            RecallCadence.WEEKLY -> R.string.recall_cadence_weekly
            RecallCadence.MONTHLY -> R.string.recall_cadence_monthly
            RecallCadence.NEVER -> R.string.recall_cadence_never
        },
    )

/** "weekly", inside a sentence. */
val RecallCadence.inSentence: String
    get() = AppText.res.getString(
        when (this) {
            RecallCadence.WEEKLY -> R.string.recall_cadence_weekly_sentence
            RecallCadence.MONTHLY -> R.string.recall_cadence_monthly_sentence
            RecallCadence.NEVER -> R.string.recall_cadence_never_sentence
        },
    )

val PasswordStrength.label: String
    get() = AppText.res.getString(
        when (this) {
            PasswordStrength.WEAK -> R.string.strength_weak
            PasswordStrength.FAIR -> R.string.strength_fair
            PasswordStrength.STRONG -> R.string.strength_strong
            PasswordStrength.VERY_STRONG -> R.string.strength_very_strong
        },
    )

/** The theme row: what is showing. */
fun themeText(followsPhone: Boolean, dark: Boolean): String {
    val shade = AppText.res.getString(if (dark) R.string.theme_dark else R.string.theme_light)
    return AppText.res.getString(if (followsPhone) R.string.theme_follows else R.string.theme_always, shade)
}

/** "On" only when every kind is; otherwise how many are. */
fun notificationsText(prefs: NoticePrefs): String {
    val kinds = listOf(prefs.serviceDue, prefs.overdue, prefs.recalls, prefs.mileage)
    return when (val on = kinds.count { it }) {
        0 -> AppText.res.getString(R.string.off)
        kinds.size -> AppText.res.getString(R.string.on)
        else -> AppText.res.getString(R.string.notifications_some, on.asNumber(), kinds.size.asNumber())
    }
}

/** The schedule row: the vehicle's name when there is one, a count to choose from otherwise. Null with no vehicles. */
fun schedulesText(vehicleNames: List<String>): String? = when (vehicleNames.size) {
    0 -> null
    1 -> vehicleNames.single()
    else -> AppText.res.getQuantityString(R.plurals.garage_vehicles, vehicleNames.size, vehicleNames.size.asNumber())
}

/** "2 vehicles · 14 records · 9 photos", with the documents' size beside them, which is what can make a backup big. */
fun backupContents(vehicles: Int, records: Int, photos: Int, documents: Int = 0, documentBytes: Long = 0): String {
    val res = AppText.res
    return listOfNotNull(
        res.getQuantityString(R.plurals.garage_vehicles, vehicles, vehicles.asNumber()),
        res.getQuantityString(R.plurals.records, records, records.asNumber()),
        res.getQuantityString(R.plurals.photos, photos, photos.asNumber()),
        documents.takeIf { it > 0 }?.let { res.getQuantityString(R.plurals.backup_documents_size, it, it.asNumber(), fileSize(documentBytes)) },
    ).joinToString(" · ")
}

/** "2 vehicles, 14 service records and 9 photos" */
fun restoreSummary(vehicles: Int, records: Int, photos: Int, documents: Int = 0): String {
    val res = AppText.res
    val parts = listOfNotNull(
        res.getQuantityString(R.plurals.garage_vehicles, vehicles, vehicles.asNumber()),
        res.getQuantityString(R.plurals.service_records, records, records.asNumber()),
        res.getQuantityString(R.plurals.photos, photos, photos.asNumber()),
        documents.takeIf { it > 0 }?.let { res.getQuantityString(R.plurals.documents, it, it.asNumber()) },
    )
    return if (parts.size <= 2) andList(parts) else res.getString(R.string.list_and, parts.dropLast(1).joinToString(", "), parts.last())
}

/** "8:00 AM", in the app's language. */
fun timeLabel(hour: Int, minute: Int): String =
    LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(AppText.locale))

/** "Tire rotation", "Tire rotation and Oil & filter", "3 services". */
fun servicesText(names: List<String>): String = when (names.size) {
    1, 2 -> andList(names)
    else -> AppText.res.getQuantityString(R.plurals.services, names.size, names.size.asNumber())
}

val LicenceGroup.title: String
    get() = when (val n = name) {
        is LicenceName.Given -> n.text
        is LicenceName.OwnOf -> AppText.res.getString(R.string.licence_own_of, n.library)
        LicenceName.NotRecorded -> AppText.res.getString(R.string.licence_not_recorded)
    }

/** "169 libraries under 5 licences" */
fun licencesSummary(groups: List<LicenceGroup>): String {
    val libraries = Licences.libraryCount(groups)
    return AppText.res.getString(
        R.string.licences_summary,
        AppText.res.getQuantityString(R.plurals.libraries, libraries, libraries.asNumber()),
        AppText.res.getQuantityString(R.plurals.licences, groups.size, groups.size.asNumber()),
    )
}

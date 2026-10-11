package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How well a backup password would stand up to guessing. A rough guide, not a guarantee. */
enum class PasswordStrength(val label: String, val bars: Int) {
    /** Under [BackupRules.MIN_PASSWORD_LENGTH] characters; a backup can't be made with it. */
    TOO_SHORT("Too short", 0),

    /** Long enough but easy to guess; a backup can't be made with it either. */
    WEAK("Weak", 1),
    FAIR("Fair", 2),
    STRONG("Strong", 3),
    VERY_STRONG("Very strong", 4),
    ;

    val allowed: Boolean get() = this >= FAIR
}

object BackupRules {
    const val MIN_PASSWORD_LENGTH = 10

    /** A backup older than this is worth a reminder. */
    const val REMIND_AFTER_DAYS = 30L

    /**
     * Length counts for most: a long phrase beats a short jumble. Variety of characters helps a
     * little, and a password made of very few different characters is weak whatever its length.
     */
    fun strength(password: String): PasswordStrength {
        if (password.length < MIN_PASSWORD_LENGTH) return PasswordStrength.TOO_SHORT
        if (password.toSet().size < 6) return PasswordStrength.WEAK
        val kinds = listOf(password.any(Char::isLowerCase), password.any(Char::isUpperCase), password.any(Char::isDigit), password.any { !it.isLetterOrDigit() }).count { it }
        return when {
            password.length >= 20 -> PasswordStrength.VERY_STRONG
            password.length >= 16 -> PasswordStrength.STRONG
            password.length >= 14 && kinds >= 2 -> PasswordStrength.STRONG
            kinds >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    /**
     * Whether to remind about backing up today: there is something to lose, the last backup is
     * missing or more than a month old, and the last reminder was more than a month ago too.
     */
    fun shouldRemind(hasData: Boolean, lastBackupOn: LocalDate?, lastRemindedOn: LocalDate?, today: LocalDate): Boolean {
        if (!hasData) return false
        fun old(date: LocalDate?) = date == null || ChronoUnit.DAYS.between(date, today) >= REMIND_AFTER_DAYS
        return old(lastBackupOn) && old(lastRemindedOn)
    }

    /** "glovebox-backup-2026-10-11.glovebox" */
    fun fileName(date: LocalDate, extension: String): String = "glovebox-backup-$date.$extension"

    /** "2 vehicles · 14 records · 9 photos" */
    fun contents(vehicles: Int, records: Int, photos: Int): String = listOf(
        if (vehicles == 1) "1 vehicle" else "$vehicles vehicles",
        if (records == 1) "1 record" else "$records records",
        if (photos == 1) "1 photo" else "$photos photos",
    ).joinToString(" · ")
}

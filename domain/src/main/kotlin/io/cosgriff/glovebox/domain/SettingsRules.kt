package io.cosgriff.glovebox.domain

/** "Lock the app": how long the app may sit in the background before it asks to be unlocked again. */
enum class LockDelay(val seconds: Int, val label: String) {
    IMMEDIATELY(0, "Immediately"),
    ONE_MINUTE(60, "After 1 min"),
    FIVE_MINUTES(300, "After 5 min"),

    /** Only a fresh start of the app locks it. */
    ON_RESTART(-1, "On restart");

    companion object {
        /** The stored number of seconds as one of the choices; anything unexpected is the nearest stricter one. */
        fun fromSeconds(seconds: Int): LockDelay = when {
            seconds < 0 -> ON_RESTART
            else -> entries.filter { it.seconds in 0..seconds }.maxByOrNull { it.seconds } ?: IMMEDIATELY
        }

        /** Whether coming back after [awaySeconds] in the background locks the app. */
        fun relocks(lockAfterSeconds: Int, awaySeconds: Long): Boolean = lockAfterSeconds >= 0 && awaySeconds >= lockAfterSeconds
    }
}

/** The short values shown on the right of Settings rows. */
object SettingsText {
    fun theme(followsPhone: Boolean, dark: Boolean): String {
        val shade = if (dark) "dark" else "light"
        return if (followsPhone) "Follows your phone \u2014 currently $shade" else "Always $shade, whatever your phone is set to"
    }

    /** "On" only when every kind is; otherwise how many are. */
    fun notifications(prefs: NoticePrefs): String {
        val kinds = listOf(prefs.serviceDue, prefs.overdue, prefs.recalls, prefs.mileage)
        return when (val on = kinds.count { it }) {
            0 -> "Off"
            kinds.size -> "On"
            else -> "$on of ${kinds.size} on"
        }
    }

    /** The schedule belongs to a vehicle: its name when there is one, a count to choose from otherwise. Null with no vehicles. */
    fun schedules(vehicleNames: List<String>): String? = when (vehicleNames.size) {
        0 -> null
        1 -> vehicleNames.single()
        else -> "${vehicleNames.size} vehicles"
    }
}

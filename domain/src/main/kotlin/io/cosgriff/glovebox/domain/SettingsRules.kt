package io.cosgriff.glovebox.domain

/** "Lock the app": how long the app may sit in the background before it asks to be unlocked again. */
enum class LockDelay(val seconds: Int) {
    IMMEDIATELY(0),
    ONE_MINUTE(60),
    FIVE_MINUTES(300),

    /** Only a fresh start of the app locks it. */
    ON_RESTART(-1);

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

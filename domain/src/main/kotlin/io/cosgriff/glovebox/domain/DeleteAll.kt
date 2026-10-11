package io.cosgriff.glovebox.domain

/** What this phone can ask for before something drastic. */
enum class ScreenLock {
    /** A fingerprint or face is enrolled; the PIN, pattern or password is the fallback. */
    BIOMETRIC,

    /** A PIN, pattern or password only. */
    CREDENTIAL,

    /** No screen lock at all: there is nothing to ask for. */
    NONE,
}

/** Rules for "Delete all data". */
object DeleteAll {
    /** What the user types to confirm. */
    const val WORD = "DELETE"

    /** The typed word only unlocks the button; the phone's own lock is the second check, where there is one. */
    fun asksForScreenLock(lock: ScreenLock): Boolean = lock != ScreenLock.NONE

    /** The button says what pressing it will really do next. */
    fun confirmLabel(lock: ScreenLock): String = when (lock) {
        ScreenLock.BIOMETRIC -> "Confirm with fingerprint"
        ScreenLock.CREDENTIAL -> "Confirm with screen lock"
        ScreenLock.NONE -> "Delete everything"
    }

    /** Shown under the button on a phone where typing the word is the only check. */
    fun note(lock: ScreenLock): String? = when (lock) {
        ScreenLock.NONE -> "This phone has no screen lock, so there is no second check. Pressing the button deletes everything."
        else -> null
    }
}

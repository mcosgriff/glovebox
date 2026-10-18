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
}

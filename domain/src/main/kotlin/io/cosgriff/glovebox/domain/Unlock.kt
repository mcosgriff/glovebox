package io.cosgriff.glovebox.domain

/** Why an unlock attempt ended without unlocking. */
enum class UnlockError {
    /** The user, or the system, dismissed the prompt. */
    CANCELLED,

    /** Too many failed tries; the phone lifts this by itself after a short wait. */
    LOCKED_OUT,

    /** Too many failed tries; fingerprint and face stay off until the screen lock is used. */
    LOCKED_OUT_UNTIL_SCREEN_LOCK,

    /** The sensor or the screen lock can't be used right now. */
    UNAVAILABLE,

    OTHER,
}

/** What the unlock screen offers on this phone. */
data class UnlockOptions(
    /** Under the round button. */
    val instruction: String,
    /** What the round button does, for a screen reader. */
    val buttonLabel: String,
    /** Whether the round button shows a fingerprint; otherwise a padlock. */
    val fingerprint: Boolean,
    /** The link under the instruction that skips the fingerprint or face; null when there is nothing to skip to. */
    val screenLockLink: String?,
)

object Unlock {
    /**
     * Null when the phone has no screen lock: there is nothing to ask for, and the app opens.
     *
     * [fingerprintSensor] is whether the phone has one at all; Android doesn't say which biometric is
     * enrolled, so "touch the sensor" is only said where there is a sensor to touch.
     * [screenLockAlone] is whether Android can ask for the PIN, pattern or password without offering
     * the biometric first (Android 11 and later).
     */
    fun options(lock: ScreenLock, fingerprintSensor: Boolean, screenLockAlone: Boolean): UnlockOptions? = when (lock) {
        ScreenLock.NONE -> null
        ScreenLock.CREDENTIAL -> UnlockOptions(
            instruction = "Use your screen lock to open your Glovebox",
            buttonLabel = "Unlock with screen lock",
            fingerprint = false,
            screenLockLink = null,
        )
        ScreenLock.BIOMETRIC -> UnlockOptions(
            instruction = if (fingerprintSensor) "Touch the sensor to open your Glovebox" else "Unlock to open your Glovebox",
            buttonLabel = if (fingerprintSensor) "Unlock with fingerprint" else "Unlock",
            fingerprint = fingerprintSensor,
            // A phone's lock may be a pattern or a password, so it isn't called a PIN.
            screenLockLink = if (screenLockAlone) "Use screen lock instead" else null,
        )
    }

    /**
     * What to tell the user after a failed attempt; null when nothing needs saying (they cancelled).
     * [systemMessage] is Android's own wording, used when the app has nothing more useful to add.
     */
    fun message(error: UnlockError, lock: ScreenLock, systemMessage: String?): String? = when (error) {
        UnlockError.CANCELLED -> null
        UnlockError.LOCKED_OUT ->
            if (lock == ScreenLock.BIOMETRIC) "Too many tries. Wait a moment, or use your screen lock." else "Too many tries. Wait a moment and try again."
        UnlockError.LOCKED_OUT_UNTIL_SCREEN_LOCK -> "Too many tries. Fingerprint and face are off until you use your screen lock."
        UnlockError.UNAVAILABLE, UnlockError.OTHER -> systemMessage?.trim()?.takeIf { it.isNotEmpty() } ?: "Couldn't check just now. Try again."
    }
}

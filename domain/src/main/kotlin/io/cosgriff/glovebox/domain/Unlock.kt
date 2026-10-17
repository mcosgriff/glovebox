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

/** What the unlock screen asks for; `app` words it. */
enum class UnlockPrompt {
    /** The PIN, pattern or password: the phone has no biometric. */
    SCREEN_LOCK,

    /** The phone has a fingerprint sensor, so the user is told to touch it. */
    FINGERPRINT,

    /** A biometric, but no sensor to point at (face, say): Android doesn't say which is enrolled. */
    BIOMETRIC,
}

/** What the unlock screen offers on this phone. */
data class UnlockOptions(
    val prompt: UnlockPrompt,
    /** Whether there is a link under the instruction that skips the fingerprint or face. */
    val screenLockLink: Boolean,
) {
    /** Whether the round button shows a fingerprint; otherwise a padlock. */
    val fingerprint: Boolean get() = prompt == UnlockPrompt.FINGERPRINT
}

/** What to tell the user after a failed attempt; `app` words it. */
sealed interface UnlockMessage {
    /** Too many tries; the biometric comes back after a wait, and the screen lock works now. */
    data object WaitOrUseScreenLock : UnlockMessage

    /** Too many tries, and nothing else to offer. */
    data object Wait : UnlockMessage

    /** Too many tries; the biometric stays off until the screen lock is used. */
    data object OffUntilScreenLock : UnlockMessage

    /** Android said what went wrong, and that is the most useful thing to show. */
    data class FromSystem(val text: String) : UnlockMessage

    /** Something failed and nobody said what. */
    data object CouldntCheck : UnlockMessage
}

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
        ScreenLock.CREDENTIAL -> UnlockOptions(UnlockPrompt.SCREEN_LOCK, screenLockLink = false)
        ScreenLock.BIOMETRIC -> UnlockOptions(
            prompt = if (fingerprintSensor) UnlockPrompt.FINGERPRINT else UnlockPrompt.BIOMETRIC,
            screenLockLink = screenLockAlone,
        )
    }

    /**
     * What to tell the user after a failed attempt; null when nothing needs saying (they cancelled).
     * [systemMessage] is Android's own wording, used when the app has nothing more useful to add.
     */
    fun message(error: UnlockError, lock: ScreenLock, systemMessage: String?): UnlockMessage? = when (error) {
        UnlockError.CANCELLED -> null
        UnlockError.LOCKED_OUT -> if (lock == ScreenLock.BIOMETRIC) UnlockMessage.WaitOrUseScreenLock else UnlockMessage.Wait
        UnlockError.LOCKED_OUT_UNTIL_SCREEN_LOCK -> UnlockMessage.OffUntilScreenLock
        UnlockError.UNAVAILABLE, UnlockError.OTHER ->
            systemMessage?.trim()?.takeIf { it.isNotEmpty() }?.let(UnlockMessage::FromSystem) ?: UnlockMessage.CouldntCheck
    }
}

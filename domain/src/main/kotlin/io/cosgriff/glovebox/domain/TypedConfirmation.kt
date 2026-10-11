package io.cosgriff.glovebox.domain

/** Destructive actions are confirmed by typing a word: the vehicle's name, or DELETE. */
object TypedConfirmation {
    /** Forgiving about case and stray spaces, so a phone keyboard's habits don't get in the way. */
    fun matches(typed: String, expected: String): Boolean {
        val want = normalize(expected)
        return want.isNotEmpty() && normalize(typed) == want
    }

    private fun normalize(text: String) = text.trim().replace(Regex("\\s+"), " ").lowercase()
}

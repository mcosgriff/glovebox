package io.cosgriff.glovebox.domain

import java.util.Locale

/** Names as the owner typed them, compared the one way. */
object Names {
    /**
     * What makes two names the same name: the spaces round them and the case of their letters ignored, whatever
     * the phone's language (a fixed locale, so "I" and "i" pair the same way everywhere).
     */
    fun key(name: String): String = name.trim().lowercase(Locale.US)
}

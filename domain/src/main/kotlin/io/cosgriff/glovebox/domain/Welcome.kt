package io.cosgriff.glovebox.domain

object Welcome {
    /**
     * The welcome is for a phone with nothing on it yet. An install that already has vehicles was
     * set up before the welcome existed, or has been restored, and goes straight in.
     */
    fun shouldShow(alreadySeen: Boolean, hasVehicles: Boolean): Boolean = !alreadySeen && !hasVehicles
}

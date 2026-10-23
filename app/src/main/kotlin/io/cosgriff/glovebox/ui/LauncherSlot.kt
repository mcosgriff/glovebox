package io.cosgriff.glovebox.ui

/**
 * Where a singleton keeps a launcher `MainActivity` registered for it. The activity attaches its launcher
 * when it is created and detaches it when it goes; a launcher is only let go by the activity that attached
 * it, since a new activity can attach before the old one is destroyed.
 */
class LauncherSlot<T : Any> {
    var launcher: T? = null
        private set

    fun attach(launcher: T) {
        this.launcher = launcher
    }

    /** True when [launcher] was the one attached, and so is let go. */
    fun detach(launcher: T): Boolean {
        if (this.launcher !== launcher) return false
        this.launcher = null
        return true
    }
}

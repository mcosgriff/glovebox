package io.cosgriff.glovebox.domain

/**
 * What the last [window] camera frames were read as, oldest first, for [DashReading.steady] and [VinScan]'s
 * agreement rules: a reading is offered only when enough recent frames say the same. Kept by one frame
 * reader on one thread; [clear] forgets them when what is in front of the camera may have changed.
 */
class RecentReads<T>(private val window: Int) {
    private val reads = ArrayDeque<T>()

    /** Adds [read] as the newest, letting the oldest go past the window, and returns the window. */
    fun add(read: T): List<T> {
        reads.addLast(read)
        while (reads.size > window) reads.removeFirst()
        return reads.toList()
    }

    fun clear() = reads.clear()
}

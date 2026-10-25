package io.cosgriff.glovebox.domain

/**
 * The other model names a vehicle's recalls are filed under (#460). NHTSA matches the model name exactly, and
 * often files a vehicle under a name vPIC doesn't give ("F-250 SD" for vPIC's "F-250"), so the owner ticks the
 * names that are theirs. None is ever picked for them.
 */
object RecallNames {
    /**
     * The names NHTSA lists that begin with [model], ignoring case, spaces and hyphens, and aren't [model] itself;
     * plus any already [chosen], so one NHTSA has stopped listing can still be unticked.
     */
    fun offered(model: String, listed: List<String>, chosen: Set<String>): List<String> {
        val key = key(model)
        return (listed.filter { key(it).startsWith(key) && key(it) != key } + chosen)
            .distinctBy { key(it) }
            .sortedBy { it.uppercase() }
    }

    /** What the recalls are asked for under: the model first, then each ticked name once. */
    fun checkedUnder(model: String, chosen: List<String>): List<String> = (listOf(model) + chosen).distinctBy { key(it) }

    private fun key(name: String) = name.uppercase().filter { it.isLetterOrDigit() }
}

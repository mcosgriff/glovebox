package io.cosgriff.glovebox.domain

/** A line Log service already has, by its id and its name as shown. */
data class NamedLine(val id: String, val name: String)

/**
 * The lines Log service already has that a custom item being typed looks like, so work goes under its line
 * and not under Custom (#309). They are only offered: the typed text is saved as typed unless one is tapped.
 *
 * A line is offered when its whole name is in what was typed ("Water pump" in "replaced the water pump"),
 * or, while it is still being typed, when every word so far starts a word of the name ("wat pu"). A name
 * with alternatives ("Shocks / struts") is in what was typed when any one of them is. Words are compared
 * ignoring case, punctuation and a plural "s"; nothing is guessed past that ("fuzzy" matching is #312's).
 */
object WorkSuggestions {
    const val MAX = 4

    /** Up to [MAX] ids from [lines]: whole names first, those of more words first, then by the order given. */
    fun suggest(typed: String, lines: List<NamedLine>): List<String> {
        val words = words(typed)
        if (words.isEmpty()) return emptyList()
        return lines.mapIndexedNotNull { index, line ->
            val named = named(line.name, words)
            when {
                named > 0 -> Triple(line.id, 0, -named) to index
                startsAll(line.name, words) -> Triple(line.id, 1, 0) to index
                else -> null
            }
        }
            .sortedWith(compareBy({ it.first.second }, { it.first.third }, { it.second }))
            .map { it.first.first }
            .distinct()
            .take(MAX)
    }

    /** How many words of [name]'s longest alternative are all in [typed]; 0 when none is. */
    private fun named(name: String, typed: List<String>): Int =
        alternatives(name).filter { it.isNotEmpty() && it.all { w -> typed.any { t -> same(t, w) } } }.maxOfOrNull { it.size } ?: 0

    /**
     * "Shocks / struts" is either word. "Brake lines / hoses" is brake lines or brake hoses: a one-word
     * alternative after several words takes their head, or "hoses" alone would match coolant hoses.
     */
    private fun alternatives(name: String): List<List<String>> {
        val parts = name.split(" / ").map(::words)
        val head = parts.first().dropLast(1)
        return listOf(parts.first()) + parts.drop(1).map { if (it.size == 1) head + it else it }
    }

    private fun startsAll(name: String, typed: List<String>): Boolean {
        val nameWords = words(name)
        return typed.all { t -> nameWords.any { it.startsWith(t) } }
    }

    private fun same(a: String, b: String) = a == b || a == b + "s" || b == a + "s"

    /** Lower case, split on anything that isn't a letter or digit; a single letter ("a", the "&" gone) says nothing. */
    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.length >= 2 }
}

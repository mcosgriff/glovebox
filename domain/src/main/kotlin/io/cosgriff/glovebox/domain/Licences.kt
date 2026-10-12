package io.cosgriff.glovebox.domain

/** A library built into the app. [id] is its Maven coordinate without the version ("io.ktor:ktor-client-core"). */
data class BundledLibrary(val id: String, val name: String, val version: String?, val licences: List<String>)

/** A licence as the build recorded it. [text] is missing for licences the build doesn't carry the wording of. */
data class Licence(val id: String, val name: String, val url: String?, val text: String?)

/** One licence and everything in the app that is under it. */
data class LicenceGroup(val licence: Licence, val name: String, val libraries: List<BundledLibrary>)

/** Rules for the open-source licences list. */
object Licences {
    /** The app's own licence, listed first and apart from its libraries. */
    const val APP_LICENCE_ID = "glovebox"
    const val APP_LICENCE_NAME = "GNU General Public License v3.0"

    fun displayName(library: BundledLibrary): String = library.name.trim().ifEmpty { library.id }

    /**
     * Libraries under each licence, biggest group first. A library under two licences is listed under
     * both. A library whose licence the build knows nothing about still appears, under that licence's id,
     * so nothing bundled can go unlisted.
     */
    fun grouped(libraries: List<BundledLibrary>, licences: Map<String, Licence>): List<LicenceGroup> =
        libraries
            .flatMap { library -> library.licences.ifEmpty { listOf(UNKNOWN) }.map { it to library } }
            .groupBy({ it.first }, { it.second })
            .map { (id, members) ->
                val licence = licences[id] ?: Licence(id, if (id == UNKNOWN) "Licence not recorded" else id, null, null)
                val sorted = members.distinctBy { it.id }.sortedBy { displayName(it).lowercase() }
                LicenceGroup(licence, groupName(licence, sorted), sorted)
            }
            .sortedWith(compareByDescending<LicenceGroup> { it.libraries.size }.thenBy { it.name.lowercase() })

    /** Some libraries publish their own licence with no name; it is then named after the library. */
    private fun groupName(licence: Licence, libraries: List<BundledLibrary>): String =
        licence.name.trim().ifEmpty { libraries.singleOrNull()?.let { "${displayName(it)} licence" } ?: licence.id }

    /** Where the wording of a library's own licence is kept when the build doesn't carry it. */
    fun assetName(libraryId: String): String = "licences/" + libraryId.replace(':', '_') + ".txt"

    /** "169 libraries under 5 licences" */
    fun summary(groups: List<LicenceGroup>): String {
        val libraries = groups.flatMap { it.libraries }.distinctBy { it.id }.size
        return "$libraries ${if (libraries == 1) "library" else "libraries"} under ${groups.size} ${if (groups.size == 1) "licence" else "licences"}"
    }

    /**
     * A licence's wording as paragraphs for a narrow screen. Licence files are wrapped and indented for
     * 80 columns; the line breaks inside a paragraph are dropped and every word is kept.
     */
    fun paragraphs(text: String): List<String> =
        text.trim().split(Regex("\\n\\s*\\n")).map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.isNotEmpty() }

    private const val UNKNOWN = "unknown"
}

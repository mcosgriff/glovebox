package io.cosgriff.glovebox.ui.about

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.BundledLibrary
import io.cosgriff.glovebox.domain.Licence
import io.cosgriff.glovebox.domain.LicenceGroup
import io.cosgriff.glovebox.domain.Licences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A licence ready to read. [text] is null only if neither the build nor the app carries its wording. */
data class LicenceDocument(val name: String, val url: String?, val text: String?)

/**
 * The libraries built into this APK and their licences. The list is written at build time by the
 * AboutLibraries Gradle plugin from the release dependencies (`R.raw.aboutlibraries`), so it can't
 * fall behind the build. Nothing is fetched while the app runs.
 */
@Singleton
class LicenceCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    @Serializable
    private class Definitions(val libraries: List<Library> = emptyList(), val licenses: Map<String, Terms> = emptyMap())

    @Serializable
    private class Library(val uniqueId: String, val artifactVersion: String? = null, val name: String = "", val licenses: List<String> = emptyList())

    @Serializable
    private class Terms(val name: String = "", val url: String? = null, val content: String? = null)

    private val json = Json { ignoreUnknownKeys = true }

    // The fonts in res/font are files in this repo, not Maven dependencies, so the generated list can't know them.
    // Their licence, with IBM's copyright line, is assets/licences/com.ibm_plex.txt.
    private val fonts = listOf(BundledLibrary("com.ibm:plex", "IBM Plex Sans and IBM Plex Mono (fonts)", null, listOf(FONT_LICENCE)))
    private val fontLicence = FONT_LICENCE to Licence(FONT_LICENCE, "SIL Open Font License 1.1", "https://openfontlicense.org", null)

    private val loaded: List<LicenceGroup> by lazy {
        val text = context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        val definitions = json.decodeFromString<Definitions>(text)
        Licences.grouped(
            definitions.libraries.map { BundledLibrary(it.uniqueId, it.name, it.artifactVersion, it.licenses) } + fonts,
            definitions.licenses.mapValues { (id, terms) -> Licence(id, terms.name, terms.url, terms.content?.takeIf(String::isNotBlank)) } + fontLicence,
        )
    }

    suspend fun groups(): List<LicenceGroup> = withContext(Dispatchers.IO) { loaded }

    /** [id] is a licence id from [groups], or [Licences.APP_LICENCE_ID] for the app's own. Null if there is no such licence. */
    suspend fun document(id: String): LicenceDocument? = withContext(Dispatchers.IO) {
        if (id == Licences.APP_LICENCE_ID) {
            return@withContext LicenceDocument(Licences.APP_LICENCE_NAME, null, asset(Licences.assetName(Licences.APP_LICENCE_ID)))
        }
        val group = loaded.firstOrNull { it.licence.id == id } ?: return@withContext null
        // A library's own licence comes without its wording; the app keeps a copy for each of those.
        val text = group.licence.text ?: group.libraries.singleOrNull()?.let { asset(Licences.assetName(it.id)) }
        LicenceDocument(group.name, group.licence.url, text)
    }

    private companion object {
        const val FONT_LICENCE = "OFL-1.1"
    }

    private fun asset(name: String): String? = try {
        context.assets.open(name).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        null
    }
}

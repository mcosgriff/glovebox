package io.cosgriff.glovebox.ui.about

import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.settings.title
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
 * fall behind the build. Nothing is fetched while the app runs, nor while it builds (`offlineMode`): the wording
 * of each licence is the app's own copy in assets/licences.
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
    private val fonts get() = listOf(BundledLibrary("com.ibm:plex", AppText.res.getString(R.string.licences_plex), null, listOf(FONT_LICENCE)))
    private val fontLicence = FONT_LICENCE to Licence(FONT_LICENCE, "SIL Open Font License 1.1", "https://openfontlicense.org", null)

    // Tesseract4Android is published without licence details, so the generated list has it under none. It is
    // Apache 2.0, as are the Tesseract engine and the language file in assets/tessdata; the image libraries
    // built into it have their own terms, kept in assets/licences/cz.adaptech.tesseract4android_tesseract4android.txt.
    private val tesseract get() = BundledLibrary(
        TESSERACT,
        AppText.res.getString(R.string.licences_tesseract),
        null,
        listOf(APACHE, TESSERACT_BUNDLED),
    )
    private val tesseractLicence get() = TESSERACT_BUNDLED to Licence(TESSERACT_BUNDLED, AppText.res.getString(R.string.licences_tesseract_bundled), null, null)

    private val loaded: List<LicenceGroup> by lazy {
        val text = context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        val definitions = json.decodeFromString<Definitions>(text)
        Licences.grouped(
            definitions.libraries.map {
                if (it.uniqueId == TESSERACT) tesseract.copy(version = it.artifactVersion) else BundledLibrary(it.uniqueId, it.name, it.artifactVersion, it.licenses)
            } + fonts,
            definitions.licenses.mapValues { (id, terms) -> Licence(id, STANDARD[id] ?: terms.name, terms.url, terms.content?.takeIf(String::isNotBlank)) } + fontLicence + tesseractLicence,
        )
    }

    suspend fun groups(): List<LicenceGroup> = withContext(Dispatchers.IO) { loaded }

    /** [id] is a licence id from [groups], or [Licences.APP_LICENCE_ID] for the app's own. Null if there is no such licence. */
    suspend fun document(id: String): LicenceDocument? = withContext(Dispatchers.IO) {
        if (id == Licences.APP_LICENCE_ID) {
            return@withContext LicenceDocument(Licences.APP_LICENCE_NAME, null, asset(Licences.assetName(Licences.APP_LICENCE_ID)))
        }
        val group = loaded.firstOrNull { it.licence.id == id } ?: return@withContext null
        // The build lists licences without their wording (it fetches nothing), so the app keeps a copy of each
        // standard licence, and of each library's own.
        val text = group.licence.text ?: asset(Licences.standardAssetName(id)) ?: group.libraries.singleOrNull()?.let { asset(Licences.assetName(it.id)) }
        LicenceDocument(group.title, group.licence.url, text)
    }

    private companion object {
        const val FONT_LICENCE = "OFL-1.1"
        const val TESSERACT = "cz.adaptech.tesseract4android:tesseract4android"
        const val TESSERACT_BUNDLED = "tesseract4android-bundled"
        const val APACHE = "Apache-2.0"

        /**
         * The standard licences the libraries use, by SPDX id, with SPDX's full name; their wording is in
         * assets/licences/spdx. The build, working offline, gives only the name a library's POM happens to use.
         */
        val STANDARD = mapOf(
            APACHE to "Apache License 2.0",
            "BSD-3-Clause" to "BSD 3-Clause \"New\" or \"Revised\" License",
            "MIT" to "MIT License",
        )
    }

    private fun asset(name: String): String? = try {
        context.assets.open(name).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        null
    }
}

package io.cosgriff.glovebox.ui.torque

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.domain.KnownSpecFile
import io.cosgriff.glovebox.domain.KnownSpecList
import io.cosgriff.glovebox.domain.KnownSpecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The torque lists that ship with the app: every file in `assets/known-specs`, read once. What a
 * list is and what may be in one is `KnownSpecs` in `:domain`, where a test reads these same files,
 * so one that doesn't parse fails the build's tests; here such a file is simply left out.
 */
@Singleton
class KnownSpecCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    private val loaded: List<KnownSpecList> by lazy {
        try {
            context.assets.list(KnownSpecs.ASSET_DIR).orEmpty().sorted().mapNotNull { name ->
                val text = context.assets.open("${KnownSpecs.ASSET_DIR}/$name").bufferedReader().use { it.readText() }
                (KnownSpecs.parse(text) as? KnownSpecFile.Ok)?.list
            }
        } catch (_: IOException) {
            emptyList()
        }
    }

    suspend fun lists(): List<KnownSpecList> = withContext(Dispatchers.IO) { loaded }
}

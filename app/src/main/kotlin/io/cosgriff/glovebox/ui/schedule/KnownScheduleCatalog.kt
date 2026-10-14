package io.cosgriff.glovebox.ui.schedule

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.domain.KnownScheduleFile
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The makers' maintenance schedules that ship with the app: every file in `assets/known-schedules`,
 * read once. What a list is and what may be in one is `KnownSchedules` in `:domain`, where a test
 * reads these same files, so one that doesn't parse fails the build's tests; here such a file is
 * simply left out.
 */
@Singleton
class KnownScheduleCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    private val loaded: List<KnownScheduleList> by lazy {
        try {
            context.assets.list(KnownSchedules.ASSET_DIR).orEmpty().sorted().mapNotNull { name ->
                val text = context.assets.open("${KnownSchedules.ASSET_DIR}/$name").bufferedReader().use { it.readText() }
                (KnownSchedules.parse(text) as? KnownScheduleFile.Ok)?.list
            }
        } catch (_: IOException) {
            emptyList()
        }
    }

    suspend fun lists(): List<KnownScheduleList> = withContext(Dispatchers.IO) { loaded }
}

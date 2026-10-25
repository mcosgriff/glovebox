package io.cosgriff.glovebox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.cosgriff.glovebox.domain.NoticeLog
import io.cosgriff.glovebox.domain.NoticeRun
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.noticeLog by preferencesDataStore("notice_log")

/**
 * What has already been notified about, so the daily check doesn't repeat itself. It holds only
 * ids and dates, no vehicle data; each check rewrites the parts it covered.
 */
class NoticeLogStore(private val context: Context) {
    suspend fun read(): NoticeLog {
        val p = context.noticeLog.data.first().asMap()
        fun <T> under(prefix: String, value: (Any) -> T?): Map<Long, T> = buildMap {
            for ((key, v) in p) {
                val id = key.name.removePrefix(prefix).takeIf { key.name.startsWith(prefix) }?.toLongOrNull() ?: continue
                value(v)?.let { put(id, it) }
            }
        }
        return NoticeLog(
            dueAnnounced = under(DUE) { it as? String },
            overdueNudged = under(OVERDUE) { (it as? Long)?.let(LocalDate::ofEpochDay) },
            mileageAsked = under(MILEAGE) { (it as? Long)?.let(LocalDate::ofEpochDay) },
            expiryAnnounced = under(EXPIRY) { it as? String },
        )
    }

    /**
     * Rewrites the parts of the log that [runs] covered and leaves the rest alone, so the mileage
     * check-in at a time of its own and the rest of the daily check can't undo each other's entries.
     */
    suspend fun write(log: NoticeLog, runs: Set<NoticeRun> = NoticeRun.entries.toSet()) {
        context.noticeLog.edit { p ->
            val prefixes = buildList {
                if (NoticeRun.SERVICES in runs) addAll(listOf(DUE, OVERDUE, EXPIRY))
                if (NoticeRun.MILEAGE in runs) add(MILEAGE)
            }
            p.asMap().keys.filter { key -> prefixes.any { key.name.startsWith(it) } }.forEach { p.remove(it) }
            if (NoticeRun.SERVICES in runs) {
                log.dueAnnounced.forEach { (id, cycle) -> p[stringPreferencesKey("$DUE$id")] = cycle }
                log.overdueNudged.forEach { (id, day) -> p[longPreferencesKey("$OVERDUE$id")] = day.toEpochDay() }
                log.expiryAnnounced.forEach { (id, key) -> p[stringPreferencesKey("$EXPIRY$id")] = key }
            }
            if (NoticeRun.MILEAGE in runs) log.mileageAsked.forEach { (id, day) -> p[longPreferencesKey("$MILEAGE$id")] = day.toEpochDay() }
        }
    }

    private companion object {
        const val DUE = "due:"
        const val OVERDUE = "overdue:"
        const val MILEAGE = "mileage:"

        /** Keyed by document id, not vehicle. */
        const val EXPIRY = "expiry:"
    }
}

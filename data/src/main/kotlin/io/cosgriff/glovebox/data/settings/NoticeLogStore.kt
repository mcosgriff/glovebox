package io.cosgriff.glovebox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.cosgriff.glovebox.domain.NoticeLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.noticeLog by preferencesDataStore("notice_log")

/**
 * What has already been notified about, so the daily check doesn't repeat itself. It holds only
 * ids and dates, no vehicle data, and is rewritten whole after every check.
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
        )
    }

    suspend fun write(log: NoticeLog) {
        context.noticeLog.edit { p ->
            p.clear()
            log.dueAnnounced.forEach { (id, cycle) -> p[stringPreferencesKey("$DUE$id")] = cycle }
            log.overdueNudged.forEach { (id, day) -> p[longPreferencesKey("$OVERDUE$id")] = day.toEpochDay() }
            log.mileageAsked.forEach { (id, day) -> p[longPreferencesKey("$MILEAGE$id")] = day.toEpochDay() }
        }
    }

    private companion object {
        const val DUE = "due:"
        const val OVERDUE = "overdue:"
        const val MILEAGE = "mileage:"
    }
}

package io.cosgriff.glovebox.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.domain.NoticeLog
import io.cosgriff.glovebox.domain.NoticeRun
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoticeLogStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)
    private lateinit var store: NoticeLogStore

    @Before fun setUp() {
        resetDataStores()
        store = NoticeLogStore(context)
    }

    private val full get() = NoticeLog(
        dueAnnounced = mapOf(1L to "oil@60000", 2L to "tires@55000"),
        overdueNudged = mapOf(1L to day),
        mileageAsked = mapOf(7L to day.minusDays(14)),
    )

    @Test
    fun `nothing has been said on a new install`() = runBlocking {
        assertEquals(NoticeLog(), store.read())
    }

    @Test
    fun `the log reads back as written`() = runBlocking {
        store.write(full)
        assertEquals(full, store.read())
    }

    @Test
    fun `a run rewrites only its own part, so the two checks can't undo each other`() = runBlocking {
        store.write(full)

        store.write(NoticeLog(dueAnnounced = mapOf(3L to "brakes@70000")), setOf(NoticeRun.SERVICES))
        assertEquals(NoticeLog(dueAnnounced = mapOf(3L to "brakes@70000"), mileageAsked = full.mileageAsked), store.read())

        store.write(NoticeLog(mileageAsked = mapOf(8L to day)), setOf(NoticeRun.MILEAGE))
        assertEquals(NoticeLog(dueAnnounced = mapOf(3L to "brakes@70000"), mileageAsked = mapOf(8L to day)), store.read())
    }

    @Test
    fun `writing an empty log for every run forgets everything`() = runBlocking {
        store.write(full)
        store.write(NoticeLog())
        assertEquals(NoticeLog(), store.read())
    }
}

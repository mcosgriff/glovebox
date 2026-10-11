package io.cosgriff.glovebox

import io.cosgriff.glovebox.data.DataWipe
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.notify.NoticeScheduler
import io.cosgriff.glovebox.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GloveboxApp : Application() {
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var scheduler: NoticeScheduler
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        // Before Hilt builds anything: a "Delete all data" that was cut short must finish before the database is opened.
        DataWipe.finishInterrupted(this)
        super.onCreate()
        notifier.createChannels()
        // The background jobs follow the settings: scheduled on first launch, and again whenever one changes.
        scope.launch {
            settings.settings.map { it.notices to it.recallCadence }.distinctUntilChanged().collect { (notices, recalls) ->
                scheduler.sync(notices, recalls)
            }
        }
    }
}

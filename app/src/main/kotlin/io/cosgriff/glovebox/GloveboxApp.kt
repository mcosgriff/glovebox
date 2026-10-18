package io.cosgriff.glovebox

import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.widget.GarageWidgets
import io.cosgriff.glovebox.widget.WidgetView
import kotlinx.coroutines.flow.collectLatest
import io.cosgriff.glovebox.data.DataWipe
import android.app.Application
import android.os.Process
import dagger.hilt.android.HiltAndroidApp
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.notify.NoticeScheduler
import io.cosgriff.glovebox.notify.Notifier
import io.cosgriff.glovebox.ui.AppText
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
    @Inject lateinit var widgets: GarageWidgets
    // Lazy: nothing reads the garage for the widget while it is off.
    @Inject lateinit var garage: dagger.Lazy<GarageOverview>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        // The PDF viewer's renderer runs in an isolated process of this app (#224), which starts the
        // Application as well. It has no files, network or notification service: nothing of the app runs there.
        if (Process.isIsolated()) {
            super.onCreate()
            return
        }
        AppText.install(this)
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
        // The home-screen widget (#238) follows its switch on every launch, so a restore, a wipe or a
        // reinstall can't leave it offered while the setting says off. While it is on, it is redrawn
        // whenever what it shows changes: a record, a reading, the schedule, a setting, a restore.
        scope.launch {
            settings.settings.map { it.homeWidget }.distinctUntilChanged().collectLatest { on ->
                widgets.follow(on)
                if (on) garage.get().garage.map(WidgetView::of).distinctUntilChanged().collect { widgets.refresh() }
            }
        }
    }
}

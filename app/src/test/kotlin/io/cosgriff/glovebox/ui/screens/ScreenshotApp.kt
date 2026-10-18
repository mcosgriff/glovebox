package io.cosgriff.glovebox.ui.screens

import android.app.Application
import io.cosgriff.glovebox.ui.AppText

/**
 * The application the screenshot tests run in, in place of `GloveboxApp`: no Hilt, no database, no
 * workers. Only what drawing a screen needs, the app's language for the shared formatters.
 */
class ScreenshotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppText.install(this)
    }
}

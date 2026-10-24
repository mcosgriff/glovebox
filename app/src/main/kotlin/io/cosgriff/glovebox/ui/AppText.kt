package io.cosgriff.glovebox.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.content.res.Resources
import android.text.format.DateFormat
import io.cosgriff.glovebox.R
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The app's language, for the shared formatters in `Mappers.kt` (distances, dates, due headlines).
 * They are called from composables, view models and workers alike, so they read the application's
 * resources here instead of taking a Context, the way Java's own formatting reads the default
 * locale. Wording that belongs to one screen uses `stringResource` (CLAUDE.md, Conventions).
 *
 * Figures and dates follow [locale], the translation's own (`format_locale`, see [inAppLocale]),
 * not the phone's: the app reads the same on every phone until it is translated, and a
 * translation brings its formats. Code outside composables words things through [res].
 */
object AppText {
    private lateinit var app: Application
    private var resources: Pair<LocaleList, Resources>? = null
    private var formats: Formats? = null

    /** From `GloveboxApp.onCreate`, before anything is drawn or a worker runs. */
    fun install(application: Application) {
        app = application
    }

    /** The application's resources in the app's language; made again only when the phone's language changes. */
    val res: Resources
        get() {
            val phone = app.resources.configuration.locales
            resources?.takeIf { it.first == phone }?.let { return it.second }
            return app.inAppLocale().resources.also { resources = phone to it }
        }

    val locale: Locale get() = res.configuration.locales[0]

    val current: Formats
        get() {
            val locale = locale
            return formats?.takeIf { it.locale == locale } ?: Formats(locale).also { formats = it }
        }

    class Formats(val locale: Locale) {
        /** "12,345" */
        val number: NumberFormat = NumberFormat.getIntegerInstance(locale)

        /** "Oct 17, 2026" */
        val longDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

        /** "Oct 17" */
        val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)

        /** "Oct 2026" */
        val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMyyyy"), locale)

        /** "2026" */
        val year: DateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "yyyy"), locale)
    }
}

/**
 * This context in the app's language: the locale of the translation Android picked
 * (`format_locale`), not the phone's. Plural forms, figures, dates and layout direction then all
 * follow the words on screen, so the English app on a Russian phone says "21 days", not "21 day".
 * `MainActivity` runs in it, and [AppText] reads through it.
 */
// About Play app bundles split by language; Glovebox ships one APK, with every language in it.
@SuppressLint("AppBundleLocaleChanges")
fun Context.inAppLocale(): Context {
    val locale = Locale.forLanguageTag(resources.getString(R.string.format_locale))
    if (resources.configuration.locales.size() == 1 && resources.configuration.locales[0] == locale) return this
    return createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}

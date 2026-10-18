package io.cosgriff.glovebox

import android.content.Context
import io.cosgriff.glovebox.ui.inAppLocale
import io.cosgriff.glovebox.ui.welcome.WelcomeScreen
import io.cosgriff.glovebox.ui.welcome.WelcomeAction
import io.cosgriff.glovebox.ui.welcome.FirstRun
import io.cosgriff.glovebox.ui.settings.ImportPicker
import io.cosgriff.glovebox.ui.settings.RestorePicker
import io.cosgriff.glovebox.data.backup.BackupRestorer
import io.cosgriff.glovebox.data.backup.BackupFormat
import io.cosgriff.glovebox.notify.PendingDestination
import io.cosgriff.glovebox.notify.Destination
import android.content.Intent
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.domain.ExportFormat
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.lock.AppLock
import io.cosgriff.glovebox.lock.LockScreen
import io.cosgriff.glovebox.ui.GloveboxNavHost
import io.cosgriff.glovebox.ui.TAB_COUNT
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import io.cosgriff.glovebox.ui.photo.RecordPhotoPicker
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import io.cosgriff.glovebox.ui.vehicle.DocumentPicker
import io.cosgriff.glovebox.ui.vehicle.VehiclePhotoPicker
import kotlinx.coroutines.launch
import javax.inject.Inject

// FragmentActivity (not plain ComponentActivity) because BiometricPrompt needs it.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appLock: AppLock
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var photoPicker: VehiclePhotoPicker

    // Registered here, not in a screen, so the answer still arrives if the app locked meanwhile.
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { photoPicker.onPicked(it) }

    @Inject lateinit var recordPhotoPicker: RecordPhotoPicker
    @Inject lateinit var recordPhotos: RecordPhotoStore

    private val pickRecordPhotos =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(RecordPhotos.MAX_PER_RECORD)) { recordPhotoPicker.onPicked(it) }
    private val takeRecordPhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { recordPhotoPicker.onCaptured(it) }
    private val launchRecordPhotoPicker: () -> Unit = {
        pickRecordPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    @Inject lateinit var exportSaver: ExportSaver
    @Inject lateinit var firstRun: FirstRun
    @Inject lateinit var pendingDestination: PendingDestination

    // One launcher per file type: the type is fixed when the launcher is made.
    private val savePdf = registerForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)) { exportSaver.onDestination(it) }
    private val saveMarkdown =
        registerForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.MARKDOWN.mimeType)) { exportSaver.onDestination(it) }
    private val saveBackup = registerForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.MIME_TYPE)) { exportSaver.onDestination(it) }
    private val launchExportSave: (String, String) -> Unit = { mimeType, fileName ->
        when (mimeType) {
            ExportFormat.PDF.mimeType -> savePdf
            ExportFormat.MARKDOWN.mimeType -> saveMarkdown
            else -> saveBackup
        }.launch(fileName)
    }

    @Inject lateinit var restorePicker: RestorePicker
    @Inject lateinit var restorer: BackupRestorer

    // Any file type: a backup's type is "octet-stream", and cloud apps report all sorts of things for it.
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { restorePicker.onPicked(it) }
    private val launchBackupPicker: () -> Unit = { openBackup.launch(arrayOf("*/*")) }

    @Inject lateinit var importPicker: ImportPicker

    // Any file type here too: a .json file is reported as JSON, plain text or nothing in particular depending on the app holding it.
    private val openImport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { importPicker.onPicked(it) }
    private val launchImportPicker: () -> Unit = { openImport.launch(arrayOf("*/*")) }

    @Inject lateinit var documentPicker: DocumentPicker

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { documentPicker.onPicked(it) }
    private val takeDocumentPhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { documentPicker.onCaptured(it) }
    private val launchDocumentPicker: () -> Unit = { openDocument.launch(arrayOf("application/pdf", "image/*")) }

    private val launchPhotoPicker: () -> Unit = {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // The app's language, not the phone's (see inAppLocale).
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(newBase.inAppLocale())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch carries a new tap; a restored activity would replay an old one.
        if (savedInstanceState == null) pendingDestination.set(Destination.from(intent))
        photoPicker.attach(launchPhotoPicker)
        recordPhotoPicker.attach(launchRecordPhotoPicker) { takeRecordPhoto.launch(it) }
        exportSaver.attach(launchExportSave)
        restorePicker.attach(launchBackupPicker)
        importPicker.attach(launchImportPicker)
        documentPicker.attach(launchDocumentPicker) { takeDocumentPhoto.launch(it) }
        // Once per process, and only on a fresh start: a restored activity may have a form with photos attached.
        if (savedInstanceState == null && !swept) {
            swept = true
            exportSaver.clear()
            lifecycleScope.launch {
                // A restore that was cut off after replacing the database still has photos to move in;
                // that has to happen before the sweep, which would see them as missing.
                restorer.finishInterrupted()
                restorePicker.clear()
                recordPhotos.sweep()
            }
        }
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = null)
            val locked by appLock.locked.collectAsStateWithLifecycle()

            LaunchedEffect(settings?.hideInRecents) { applySecureFlag(settings) }
            LaunchedEffect(settings?.biometricLock) {
                if (settings?.biometricLock == false) appLock.unlock()
            }

            // The lock screen takes the app's screens out of composition, so nothing of them (sheets
            // and dialogs included) is on screen or readable while locked. Their saved state is kept
            // here, which puts the user back where they were once they unlock.
            val place = rememberSaveableStateHolder()
            // The screens' ViewModels, one store per tab, kept here for the same reason: made inside the nav
            // host, they would be cleared each time the lock took it away.
            val screenModels = List(TAB_COUNT) { rememberViewModelStoreProvider(key = "screens-$it") }

            val welcome by firstRun.needsWelcome.collectAsStateWithLifecycle()

            GloveboxTheme(mode = settings?.theme ?: Settings().theme) {
                when {
                    settings == null || welcome == null -> Unit // first frames while DataStore and the database load
                    // Before the lock: there is nothing on the phone to protect yet, and the lock is one of the things being chosen.
                    welcome == true -> WelcomeScreen(
                        onDone = { choice, then ->
                            lifecycleScope.launch {
                                firstRun.finish(choice)
                                // They have just set the lock up; asking for a fingerprint straight away would only get in the way.
                                appLock.unlock()
                                when (then) {
                                    WelcomeAction.ADD_VEHICLE -> pendingDestination.set(Destination.AddVehicle)
                                    WelcomeAction.RESTORE -> restorePicker.pick()
                                }
                            }
                        },
                    )
                    locked && settings?.biometricLock == true -> LockScreen(onUnlocked = appLock::unlock)
                    else -> place.SaveableStateProvider(APP_SCREENS) { GloveboxNavHost(screenModels) }
                }
            }
        }
    }

    /** A notification tapped while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDestination.set(Destination.from(intent))
    }

    override fun onDestroy() {
        photoPicker.detach(launchPhotoPicker)
        recordPhotoPicker.detach(launchRecordPhotoPicker)
        exportSaver.detach(launchExportSave)
        restorePicker.detach(launchBackupPicker)
        importPicker.detach(launchImportPicker)
        documentPicker.detach(launchDocumentPicker)
        super.onDestroy()
    }

    private companion object {
        const val APP_SCREENS = "app"

        /** The orphaned-photo sweep has run in this process. */
        var swept = false
    }

    /** "Hide in recent apps": blocks screenshots and blanks the app-switcher thumbnail. */
    private fun applySecureFlag(settings: Settings?) {
        if (settings?.hideInRecents != false) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

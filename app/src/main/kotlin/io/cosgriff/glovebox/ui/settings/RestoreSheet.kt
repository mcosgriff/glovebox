package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.data.displayName
import io.cosgriff.glovebox.ui.LauncherSlot
import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.ui.components.SheetActions
import io.cosgriff.glovebox.ui.components.TypedConfirmField
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.theme.PlexMono
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.data.backup.BackupException
import io.cosgriff.glovebox.data.backup.BackupRestorer
import io.cosgriff.glovebox.data.backup.BackupSummary
import io.cosgriff.glovebox.domain.BackupRules
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A backup file the user picked, copied into the app's cache so it can be read more than once. */
data class PickedBackup(val file: File, val name: String)

/**
 * Lets the user pick a backup with Android's file picker. A singleton with its launcher in the
 * activity, like the other pickers: choosing a file leaves the app, which can lock it. The copy in
 * the cache is still encrypted; it is removed when the sheet closes and on the next launch.
 */
@Singleton
class RestorePicker @Inject constructor(@ApplicationContext private val context: Context, @ApplicationScope private val scope: CoroutineScope) {
    private val slot = LauncherSlot<() -> Unit>()

    private val _picked = MutableStateFlow<PickedBackup?>(null)
    val picked: StateFlow<PickedBackup?> = _picked.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    private val dir: File get() = File(context.cacheDir, "restore").apply { mkdirs() }

    fun attach(launcher: () -> Unit) {
        slot.attach(launcher)
    }

    fun detach(launcher: () -> Unit) {
        slot.detach(launcher)
    }

    fun pick() {
        _problem.value = null
        slot.launcher?.invoke()
    }

    /** Null when the user backed out. */
    fun onPicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val copy = File(dir, "backup.tmp")
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input -> copy.outputStream().use { input.copyTo(it) } } != null
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            }
            if (ok) _picked.value = PickedBackup(copy, context.contentResolver.displayName(uri) ?: AppText.res.getString(R.string.restore_backup_file)) else _problem.value = AppText.res.getString(R.string.document_problem_unreadable)
        }
    }

    fun clear() {
        _picked.value = null
        dir.listFiles()?.forEach { it.delete() }
    }
}

@HiltViewModel
class RestoreViewModel @Inject constructor(private val picker: RestorePicker, private val restorer: BackupRestorer) : ViewModel() {
    val picked = picker.picked
    val problem = picker.problem

    var password by mutableStateOf("")

    /** Set once the password has opened the file. */
    var summary by mutableStateOf<BackupSummary?>(null)
        private set

    /** Whether there is anything on this phone for the restore to replace. */
    var replaces by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var checking by mutableStateOf(false)
        private set

    /** Fraction done while restoring; null when idle. */
    var progress by mutableStateOf<Float?>(null)
        private set

    fun pick() = picker.pick()

    fun close() {
        password = ""
        summary = null
        error = null
        picker.clear()
    }

    /** Proves the password and reads what the backup holds. Nothing is changed. */
    fun open() {
        val backup = picked.value ?: return
        if (checking) return
        checking = true
        error = null
        viewModelScope.launch {
            try {
                summary = restorer.inspect(backup.file, password.toCharArray())
                replaces = restorer.hasData()
            } catch (e: BackupException) {
                error = e.message()
            } catch (_: IOException) {
                error = AppText.res.getString(R.string.restore_unreadable)
            }
            checking = false
        }
    }

    fun restore(onDone: (BackupSummary) -> Unit) {
        val backup = picked.value ?: return
        if (progress != null) return
        progress = 0f
        error = null
        viewModelScope.launch {
            try {
                val done = restorer.restore(backup.file, password.toCharArray()) { n, total -> progress = if (total == 0) 1f else n.toFloat() / total }
                progress = null
                close()
                onDone(done)
            } catch (e: BackupException) {
                progress = null
                error = AppText.res.getString(R.string.restore_nothing_changed, e.message())
            } catch (_: IOException) {
                progress = null
                error = AppText.res.getString(R.string.restore_cut_short)
            }
        }
    }

    private fun BackupException.message(): String = when (reason) {
        BackupException.Reason.WRONG_PASSWORD -> AppText.res.getString(R.string.restore_wrong_password)
        BackupException.Reason.DAMAGED -> AppText.res.getString(R.string.restore_damaged)
        BackupException.Reason.INCOMPLETE -> AppText.res.getString(R.string.restore_incomplete)
        BackupException.Reason.NEWER_VERSION -> AppText.res.getString(R.string.restore_newer)
        BackupException.Reason.NOT_A_BACKUP -> AppText.res.getString(R.string.restore_not_a_backup)
    }
}

/** Design: the restore sheet on 0a · First-run welcome. Shown whenever a backup file has been picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreSheet(onRestored: (BackupSummary) -> Unit, vm: RestoreViewModel = hiltViewModel()) {
    val picked = vm.picked.collectAsStateWithLifecycle().value ?: return
    val scheme = MaterialTheme.colorScheme
    var show by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val summary = vm.summary
    val busy = vm.checking || vm.progress != null

    GloveboxSheet(onDismiss = { if (vm.progress == null) vm.close() }) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.restore_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(picked.name, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)

            OutlinedTextField(
                value = vm.password,
                onValueChange = { vm.password = it },
                label = { Text(stringResource(R.string.backup_password)) },
                singleLine = true,
                // Once the file is open the password is fixed; changing it would mean checking again.
                enabled = summary == null && !busy,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = stringResource(if (show) R.string.restore_hide_password else R.string.restore_show_password))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            vm.error?.let {
                Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium, color = scheme.error)
            }

            if (summary == null) {
                Text(stringResource(R.string.restore_cant_recover), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            } else {
                Text(
                    restoreSummary(summary.vehicles, summary.records, summary.photos, summary.documents).let { what ->
                        summary.createdOn?.let { stringResource(R.string.restore_summary_dated, what, it.format(longDate)) } ?: stringResource(R.string.sentence, what)
                    },
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (vm.replaces) {
                    Surface(color = scheme.errorContainer, contentColor = scheme.onSurface, shape = MaterialTheme.shapes.small, border = BorderStroke(1.5.dp, scheme.error)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.restore_replaces), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.restore_replaces_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColors().soon,
                            )
                            TypedConfirmField(BackupRules.REPLACE_WORD, typed, { typed = it }, enabled = !busy)
                        }
                    }
                }
            }

            vm.progress?.let { p ->
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.restore_keep_open), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }

            val confirmed = summary == null || !vm.replaces || TypedConfirmation.matches(typed, BackupRules.REPLACE_WORD)
            SheetActions(
                onCancel = vm::close,
                onConfirm = { if (summary == null) vm.open() else vm.restore(onRestored) },
                enabled = if (summary == null) vm.password.isNotEmpty() && !busy else confirmed && !busy,
                cancelEnabled = vm.progress == null,
                colors = if (summary != null && vm.replaces) ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError) else ButtonDefaults.buttonColors(),
            ) {
                Text(
                    stringResource(
                        when {
                            summary == null -> if (vm.checking) R.string.add_vehicle_recalls_checking else R.string.restore_open
                            vm.progress != null -> R.string.restore_restoring
                            vm.replaces -> R.string.restore_replace
                            else -> R.string.restore
                        },
                    ),
                )
            }
        }
    }
}

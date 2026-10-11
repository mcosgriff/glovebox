package io.cosgriff.glovebox.ui.settings

import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.backup.BackupContents
import io.cosgriff.glovebox.data.backup.BackupFormat
import io.cosgriff.glovebox.data.backup.BackupWriter
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.BackupRules
import io.cosgriff.glovebox.domain.PasswordStrength
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val writer: BackupWriter,
    private val saver: ExportSaver,
) : ViewModel() {
    /** Null while loading. */
    val settings: StateFlow<Settings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var contents by mutableStateOf<BackupContents?>(null)
        private set

    // Held only while this screen is open, and never written anywhere.
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")

    /** Fraction done while a backup is being written; null when idle. */
    var progress by mutableStateOf<Float?>(null)
        private set
    var failed by mutableStateOf(false)
        private set

    val strength: PasswordStrength get() = BackupRules.strength(password)
    val matches: Boolean get() = password == confirm
    // Any password the user chooses is accepted; an empty one would be no protection at all.
    val canCreate: Boolean get() = password.isNotEmpty() && matches && progress == null && (contents?.vehicles ?: 0) > 0

    init {
        viewModelScope.launch { contents = writer.contents() }
    }

    fun setReminder(on: Boolean) = viewModelScope.launch { repo.setBackupReminder(on) }

    /**
     * Writes the backup to a working file, already encrypted, then hands it to Android's "save as".
     * The last-backup date is only recorded once the file has really been saved.
     */
    fun create() {
        if (!canCreate) return
        failed = false
        progress = 0f
        val secret = password.toCharArray()
        val today = LocalDate.now()
        viewModelScope.launch {
            val file = saver.newFile()
            try {
                writer.write(file.outputStream(), secret, today) { done, total -> progress = if (total == 0) 1f else done.toFloat() / total }
            } catch (_: IOException) {
                file.delete()
                failed = true
                progress = null
                return@launch
            } finally {
                secret.fill('\u0000')
            }
            progress = null
            password = ""
            confirm = ""
            saver.save(file, BackupRules.fileName(today, BackupFormat.FILE_EXTENSION), BackupFormat.MIME_TYPE, "Backup saved") {
                repo.setLastBackup(today)
            }
        }
    }
}

/** Design: 9b · Create encrypted backup. */
@Composable
fun BackupScreen(onBack: () -> Unit, onRestore: () -> Unit, vm: BackupViewModel = hiltViewModel()) {
    val settings = vm.settings.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var show by remember { mutableStateOf(false) }
    val contents = vm.contents
    val last = settings.lastBackupOn
    val stale = last == null || BackupRules.shouldRemind(true, last, null, LocalDate.now())

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text(
                "Encrypted backup",
                Modifier.padding(start = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = if (stale) scheme.primaryContainer else scheme.tertiaryContainer,
                contentColor = scheme.onSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, (if (stale) scheme.primary else scheme.tertiary).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Last backup: ${last?.format(longDate) ?: "never"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Your data's encryption key can't leave this phone. A backup is the only way to move to a new phone or recover after a reset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColors().soon,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PasswordField("Backup password", vm.password, { vm.password = it }, show, { show = !show }, ImeAction.Next)
                StrengthMeter(vm.strength, typed = vm.password.isNotEmpty())
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PasswordField(
                    "Confirm password",
                    vm.confirm,
                    { vm.confirm = it },
                    show,
                    { show = !show },
                    ImeAction.Done,
                    error = vm.confirm.isNotEmpty() && !vm.matches,
                )
                Text(
                    if (vm.confirm.isNotEmpty() && !vm.matches) {
                        "The two passwords don't match."
                    } else {
                        "We can't recover a forgotten password. Store it in your password manager."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vm.confirm.isNotEmpty() && !vm.matches) scheme.error else scheme.onSurfaceVariant,
                )
            }

            Surface(color = scheme.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, scheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Includes", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            contents?.let { BackupRules.contents(it.vehicles, it.records, it.photos) } ?: "Counting…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = settings.backupReminder, role = Role.Switch, onValueChange = vm::setReminder),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text("Monthly backup reminder", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "A notification when your last backup is over a month old.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = settings.backupReminder, onCheckedChange = null)
                    }
                }
            }

            val progress = vm.progress
            if (progress != null) {
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("Encrypting your data…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            if (vm.failed) {
                Text(
                    "The backup couldn't be made: one of the photos couldn't be read. Nothing was saved.",
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.create()
                },
                enabled = vm.canCreate,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (progress != null) "Creating backup…" else "Create backup file", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                when {
                    contents != null && contents.vehicles == 0 -> "There is nothing to back up yet. Add a vehicle first."
                    else -> "Android asks where to save it: a folder, USB drive or a cloud app you choose. Glovebox never uploads it."
                },
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(color = scheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Bring back a backup file made on this or another phone. It replaces what is here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Restore from a backup file", color = scheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    show: Boolean,
    onToggleShow: () -> Unit,
    imeAction: ImeAction,
    error: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        // Password type also keeps it out of the keyboard's suggestions and learned words.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (show) "Hide passwords" else "Show passwords")
            }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Four bars and a word, as on the canvas. The word carries the meaning; the bars are decoration. */
@Composable
private fun StrengthMeter(strength: PasswordStrength, typed: Boolean) {
    val scheme = MaterialTheme.colorScheme
    // Advice, not a gate: a weak password still makes a backup, so it isn't shown as an error.
    val color = when (strength) {
        PasswordStrength.WEAK -> scheme.onPrimaryContainer
        PasswordStrength.FAIR -> statusColors().soon
        else -> statusColors().ok
    }
    Row(Modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { i ->
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (typed && i < strength.bars) color else scheme.outlineVariant))
        }
    }
    Text(
        when {
            !typed -> "Your choice. A long phrase is harder to guess than a short jumble."
            strength == PasswordStrength.WEAK -> "Weak: easy to guess if someone gets hold of the file. It will still work."
            else -> strength.label
        },
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (typed) FontWeight.SemiBold else FontWeight.Normal,
        color = if (typed) color else scheme.onSurfaceVariant,
    )
}

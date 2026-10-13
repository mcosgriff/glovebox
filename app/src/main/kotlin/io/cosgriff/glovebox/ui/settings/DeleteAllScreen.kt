package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.theme.PlexMono
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.domain.DeleteAll
import io.cosgriff.glovebox.domain.ScreenLock
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.lock.rememberScreenLockCheck
import io.cosgriff.glovebox.lock.screenLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class DeleteAllViewModel @Inject constructor(private val wipe: DataWipe) : ViewModel() {
    var deleting by mutableStateOf(false)
        private set

    /** No Undo. [onWiped] must restart the process: everything still in memory describes data that is gone. */
    fun delete(onWiped: () -> Unit) {
        if (deleting) return
        deleting = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) { wipe.deleteEverything() }
            onWiped()
        }
    }
}

/** Starts the app again in a new process, which finds nothing and shows the welcome. */
private fun Activity.restartFromScratch() {
    NotificationManagerCompat.from(this).cancelAll()
    packageManager.getLaunchIntentForPackage(packageName)?.let {
        startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
    exitProcess(0)
}

/** Design: 9d · Delete all data. */
@Composable
fun DeleteAllScreen(onCancel: () -> Unit, onBackupFirst: () -> Unit, vm: DeleteAllViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    val activity = LocalActivity.current as FragmentActivity
    // Read each time the screen is drawn: a screen lock may have been set since.
    val lock = activity.screenLock()
    var typed by rememberSaveable { mutableStateOf("") }
    val confirmed = TypedConfirmation.matches(typed, DeleteAll.WORD)

    fun wipe() = vm.delete { activity.restartFromScratch() }
    // Cancelling does nothing: the button is still there.
    val confirm = rememberScreenLockCheck("Delete everything in Glovebox", "This can't be undone", onPassed = ::wipe)

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onCancel, enabled = !vm.deleting, modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Cancel")
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(color = scheme.errorContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(28.dp), tint = scheme.error)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Delete everything in Glovebox?",
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "All vehicles, archived vehicles, records, photos, specs, reminders and settings. The app goes back to its welcome screen.",
                    color = scheme.onSurfaceVariant,
                )
            }

            Surface(
                Modifier.fillMaxWidth(),
                color = scheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How it's erased", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "We delete the encryption keys from the phone's secure hardware, then the database and photos. " +
                            "Without the keys, nothing left behind can be read.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Backup and export files you've saved elsewhere aren't touched. Delete those yourself if you want them gone.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("This can't be undone.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = onBackupFirst,
                enabled = !vm.deleting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, scheme.onSurface),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text("Make a backup first")
            }

            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("Type ")
                            withStyle(SpanStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)) { append(DeleteAll.WORD) }
                            append(" to confirm")
                        },
                    )
                },
                singleLine = true,
                enabled = !vm.deleting,
                textStyle = TextStyle(fontFamily = PlexMono),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmed) scheme.error else scheme.primary,
                    unfocusedBorderColor = if (confirmed) scheme.error else scheme.outline,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    if (DeleteAll.asksForScreenLock(lock)) {
                        confirm(false)
                    } else {
                        wipe()
                    }
                },
                enabled = confirmed && !vm.deleting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
            ) {
                if (lock == ScreenLock.BIOMETRIC) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
                }
                Text(DeleteAll.confirmLabel(lock), style = MaterialTheme.typography.titleMedium)
            }
            DeleteAll.note(lock)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

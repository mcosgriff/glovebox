package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.WrittenFile

/**
 * "Save as an import file", with [caption] under it saying what the file holds. [write] builds the
 * file when the button is tapped; [onSave] gets its text once the user has gone ahead.
 *
 * A file never claims to hold more than it does: when something on the screen can't be written
 * (`WrittenFile.leftOut`), each one is listed with the reason before anything is saved, and the user
 * chooses whether the rest is worth having.
 */
@Composable
fun SaveAsImportFile(caption: String, write: () -> WrittenFile, onSave: (text: String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    var asking by remember { mutableStateOf<WrittenFile?>(null) }

    asking?.let { file ->
        val text = file.text
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(if (text == null) stringResource(R.string.save_file_nothing) else pluralStringResource(R.plurals.save_file_left_out, file.leftOut.size, file.leftOut.size.asNumber())) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    file.leftOut.forEach { left ->
                        Column {
                            Text(left.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(left.why.replaceFirstChar { it.uppercase(AppText.locale) }, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        if (text == null) {
                            stringResource(R.string.save_file_stricter)
                        } else {
                            pluralStringResource(R.plurals.save_file_rest, file.leftOut.size, pluralStringResource(R.plurals.save_file_others, file.count, file.count.asNumber()))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                if (text != null) {
                    TextButton(
                        onClick = {
                            asking = null
                            onSave(text)
                        },
                    ) { Text(stringResource(R.string.save_file_rest_button)) }
                }
            },
            dismissButton = { TextButton(onClick = { asking = null }) { Text(stringResource(if (text == null) R.string.ok else R.string.cancel)) } },
        )
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = {
                val file = write()
                val text = file.text
                if (text != null && file.leftOut.isEmpty()) onSave(text) else asking = file
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.save_file_button), fontWeight = FontWeight.SemiBold)
        }
        Text(caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

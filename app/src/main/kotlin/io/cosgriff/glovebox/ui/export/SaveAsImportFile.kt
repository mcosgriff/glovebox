package io.cosgriff.glovebox.ui.export

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
            title = { Text(if (text == null) "Nothing here can go in a file" else if (file.leftOut.size == 1) "1 can't go in the file" else "${file.leftOut.size} can't go in the file") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    file.leftOut.forEach { left ->
                        Column {
                            Text(left.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(left.why.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        if (text == null) {
                            "An import file is stricter than this screen. Change these and try again."
                        } else {
                            "An import file is stricter than this screen. The other ${file.count} can be saved without " +
                                (if (file.leftOut.size == 1) "it" else "them") + ", or change these first."
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
                    ) { Text("Save the rest") }
                }
            },
            dismissButton = { TextButton(onClick = { asking = null }) { Text(if (text == null) "OK" else "Cancel") } },
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
            Text("Save as an import file", fontWeight = FontWeight.SemiBold)
        }
        Text(caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

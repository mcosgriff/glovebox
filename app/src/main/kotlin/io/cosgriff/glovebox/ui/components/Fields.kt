package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

/**
 * A field that checks what is typed: [check] gives the standard form, or null when it isn't valid.
 * Empty is always allowed and clears it. The error shows once the user has typed something wrong.
 */
@Composable
internal fun CheckedField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    error: String,
    check: (String) -> Any?,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    val bad = value.isNotBlank() && check(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = bad,
        supportingText = (if (bad) error else supporting)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = keyboard, imeAction = ImeAction.Next),
    )
}

/** What was typed, without the spaces round it; null when that leaves nothing. */
internal fun String.orNullIfBlank() = trim().takeIf { it.isNotEmpty() }

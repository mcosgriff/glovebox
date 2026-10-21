package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.ui.styledStringResource
import io.cosgriff.glovebox.ui.theme.PlexMono

/**
 * "Type DELETE to confirm": the field a destructive action waits on, its border red once [typed] matches
 * [word] (`TypedConfirmation.matches`). A word is typed in capitals; a vehicle's name as it is written.
 */
@Composable
internal fun TypedConfirmField(
    word: String,
    typed: String,
    onTyped: (String) -> Unit,
    enabled: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Characters,
) {
    val scheme = MaterialTheme.colorScheme
    val confirmed = TypedConfirmation.matches(typed, word)
    OutlinedTextField(
        value = typed,
        onValueChange = onTyped,
        label = { Text(styledStringResource(R.string.type_word_to_confirm, word, SpanStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold))) },
        singleLine = true,
        enabled = enabled,
        textStyle = TextStyle(fontFamily = PlexMono),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (confirmed) scheme.error else scheme.primary,
            unfocusedBorderColor = if (confirmed) scheme.error else scheme.outline,
        ),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The red, full-width button that carries out a destructive action once it is confirmed. */
@Composable
internal fun DangerButton(onClick: () -> Unit, enabled: Boolean, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
        content = content,
    )
}

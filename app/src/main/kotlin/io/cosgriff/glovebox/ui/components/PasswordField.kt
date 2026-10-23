package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A backup password: hidden unless [show], with the eye that turns it on or off, which TalkBack reads as
 * [toggleLabel] ("Show password").
 */
@Composable
internal fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    show: Boolean,
    onToggleShow: () -> Unit,
    toggleLabel: String,
    imeAction: ImeAction = ImeAction.Default,
    error: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        // Password type also keeps it out of the keyboard's suggestions and learned words.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = toggleLabel)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

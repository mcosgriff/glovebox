package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.ui.theme.PlexMono

/**
 * A field for an odometer reading, in the figures' font. It keeps only what [OdometerInput.typed] keeps
 * (digits, no leading zero unless [keepZero]); read the value with `OdometerInput.parse(text.typedNumber())`.
 */
@Composable
internal fun OdometerField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isError: Boolean = false,
    keepZero: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(OdometerInput.typed(it, keepZero)) },
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

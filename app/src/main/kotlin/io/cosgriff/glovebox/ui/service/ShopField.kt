package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.ShopUse
import io.cosgriff.glovebox.domain.Shops

/**
 * A shop's name, offering the shops of earlier records (every vehicle's) as it is typed. What is typed is
 * kept as it is: a suggestion is only ever taken by tapping it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopField(
    value: String,
    onValueChange: (String) -> Unit,
    shops: List<ShopUse>,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    // Closed by a tap outside or a pick; typing opens it again.
    var dismissed by remember { mutableStateOf(false) }
    val suggestions = remember(shops, value) { Shops.suggest(shops, value) }
    val expanded = focused && !dismissed && suggestions.isNotEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { dismissed = !it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                dismissed = false
            },
            label = label,
            placeholder = placeholder,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .onFocusChanged { focused = it.isFocused },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { dismissed = true },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            suggestions.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onValueChange(name)
                        dismissed = true
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

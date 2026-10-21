package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.ui.theme.PlexMono

/**
 * A filter or a choice among a few, as the canvas draws one: a white pill with a light border, solid ink
 * once chosen. Material keeps a 48dp tap area round it. [mono] sets a figure (a grade, a size) in the figures' font.
 */
@Composable
internal fun InkChip(label: String, selected: Boolean, modifier: Modifier = Modifier, mono: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontFamily = if (mono) PlexMono else null, fontWeight = FontWeight.SemiBold) },
        modifier = modifier,
        shape = CircleShape,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            borderWidth = 1.5.dp,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
    )
}

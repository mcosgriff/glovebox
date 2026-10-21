package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The small label over a group of rows, cards or fields: grey unless the group has a colour of its own
 * ([color]), and [bold] where it heads a whole list. A heading to TalkBack, so it can be jumped to.
 */
@Composable
internal fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bold: Boolean = false,
) {
    Text(
        text,
        modifier.semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (bold) FontWeight.Bold else null,
        color = color,
    )
}

/** The arrow at the end of a row that opens something. */
@Composable
internal fun Chevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

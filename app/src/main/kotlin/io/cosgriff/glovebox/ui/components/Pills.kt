package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.theme.PlexMono

/**
 * A rounded label saying what state something is in. The ordinary size is a status on its own (a garage
 * card's, vehicle home's, a recall's), in bold or, with [bold] false, semibold; [small] is a tag beside a
 * figure or a name inside a row (ODOMETER, NEXT, a count), always bold.
 */
@Composable
internal fun Pill(text: String, container: Color, content: Color, small: Boolean = false, bold: Boolean = true) {
    Surface(color = container, shape = CircleShape) {
        Text(
            text,
            Modifier.padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 3.dp else 5.dp),
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = if (small || bold) FontWeight.Bold else FontWeight.SemiBold,
            color = content,
        )
    }
}

/** A square-cornered label on a row: where a value came from, or a size ([mono], in the figures' font). */
@Composable
internal fun Tag(text: String, container: Color, content: Color, mono: Boolean = false) {
    Surface(color = container, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = if (mono) PlexMono else null,
            fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Bold,
            color = content,
        )
    }
}

/** The tag on a value filled from a bundled list and not yet checked, on torque specs and what a vehicle takes. */
@Composable
internal fun DoubleCheckTag() {
    Tag(stringResource(R.string.torque_tag_double_check), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
}

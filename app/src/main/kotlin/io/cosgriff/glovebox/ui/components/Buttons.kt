package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R

/** The one thing a screen is for (Save, Add, Continue): solid accent, the full width, 56dp tall. */
@Composable
internal fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick, modifier.fillMaxWidth().height(56.dp), enabled = enabled, shape = MaterialTheme.shapes.medium, colors = colors, content = content)
}

/**
 * A second action beside or under the main one, outlined in ink (the canvas's "solid ink" outline). [quiet]
 * draws the border in the grey outline instead, for Cancel and other ways out. Its size is the caller's.
 */
@Composable
internal fun InkOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    quiet: Boolean = false,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick,
        modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
        border = BorderStroke(1.5.dp, if (!enabled) scheme.outlineVariant else if (quiet) scheme.outline else scheme.onSurface),
        contentPadding = contentPadding,
        content = content,
    )
}

/** A way to remove or undo something, in red text: Delete, Remove, Mark as open. */
@Composable
internal fun DangerTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier, enabled = enabled, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), content = content)
}

/**
 * The foot of a sheet: Cancel, quiet, and the button that does what the sheet is for, twice as wide. [colors]
 * changes the confirm button where the sheet warns (a restore that replaces data, a reading that looks wrong).
 */
@Composable
internal fun SheetActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    cancelEnabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    confirm: @Composable RowScope.() -> Unit,
) {
    SheetActions(onCancel, { Text(stringResource(R.string.cancel)) }, onConfirm, enabled, cancelEnabled, quiet = true, colors, confirm)
}

/** [SheetActions] with a second action of its own in place of Cancel ("Already done", "Read dash"), in ink. */
@Composable
internal fun SheetActions(
    onSecondary: () -> Unit,
    secondary: @Composable RowScope.() -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    quiet: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    confirm: @Composable RowScope.() -> Unit,
) {
    // Narrow side padding: each label is one line, and the narrower button can't spare Material's 24dp.
    val padding = PaddingValues(horizontal = 8.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InkOutlinedButton(onSecondary, Modifier.weight(1f).height(52.dp), enabled = secondaryEnabled, quiet = quiet, contentPadding = padding, content = secondary)
        Button(onConfirm, Modifier.weight(2f).height(52.dp), enabled = enabled, shape = MaterialTheme.shapes.small, colors = colors, contentPadding = padding, content = confirm)
    }
}

package io.cosgriff.glovebox.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * A string resource with one `%1$s`, the argument drawn in [style] ("Type **DELETE** to confirm").
 * The placeholder stays in the resource so a translation can put the argument where its grammar wants it.
 */
@Composable
fun styledStringResource(@StringRes id: Int, arg: String, style: SpanStyle): AnnotatedString {
    val (before, after) = stringResource(id).split("%1\$s", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return buildAnnotatedString {
        append(before)
        withStyle(style) { append(arg) }
        append(after)
    }
}

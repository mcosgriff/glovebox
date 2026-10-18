package io.cosgriff.glovebox.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * A string resource whose `%1$s` is drawn in [style] ("Type **DELETE** to confirm"); [others] fill
 * `%2$s` on, as plain text. The placeholders stay in the resource so a translation can put the
 * styled part where its grammar wants it.
 */
@Composable
fun styledStringResource(@StringRes id: Int, styled: String, style: SpanStyle, vararg others: String): AnnotatedString {
    val (before, after) = stringResource(id, MARK, *others).split(MARK, limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return buildAnnotatedString {
        append(before)
        withStyle(style) { append(styled) }
        append(after)
    }
}

// Stands in for the styled part while the rest is formatted; a private-use character no text holds.
private const val MARK = ""

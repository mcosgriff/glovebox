package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The white card with a thin border, the full width of the page. With [onClick] the whole card opens something. */
@Composable
internal fun BorderedCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(1.dp, scheme.outlineVariant)
    if (onClick == null) {
        Surface(modifier.fillMaxWidth(), color = scheme.surface, shape = shape, border = border, content = content)
    } else {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = scheme.surface, shape = shape, border = border, content = content)
    }
}

/** A [BorderedCard] of rows, padded for them; the rows draw their own dividers. */
@Composable
internal fun RowsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BorderedCard(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

/** A [RowsCard] under a [SectionLabel] (a heading to TalkBack), with an optional [note] under it. */
@Composable
internal fun LabelledCard(label: String, modifier: Modifier = Modifier, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        RowsCard(content = content)
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/**
 * The white card with no border, the full width of the page, its content [padding] in from the edge
 * (none by default: the rows inside pad themselves).
 */
@Composable
internal fun FilledCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(0.dp), content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(padding), content = content)
    }
}

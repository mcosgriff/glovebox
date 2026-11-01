package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.FindingColour
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.RecommendedItem
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors

/** A red or yellow finding's tint and its text's colour, in the shade for light or dark (#572); never the PDF's own. */
@Composable
internal fun findingColours(colour: FindingColour): Pair<Color, Color> {
    val s = statusColors()
    return if (colour == FindingColour.RED) s.needsWorkTint to s.needsWork else s.watchTint to s.watch
}

/**
 * The tag on a shop's finding (#572): a dot as well as colour, so it reads without telling red from yellow, on the
 * finding's tint. [text] is NEEDS WORK or WATCH, or a count of them on vehicle home.
 */
@Composable
internal fun FindingTag(colour: FindingColour, text: String) {
    val (tint, ink) = findingColours(colour)
    Surface(color = tint, shape = RoundedCornerShape(6.dp)) {
        Row(
            Modifier.heightIn(min = 22.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(ink, CircleShape))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, letterSpacing = 0.44.sp, color = ink)
        }
    }
}

/** NEEDS WORK or WATCH, for a finding's colour; none for green, which is never listed. */
@Composable
internal fun findingTagText(colour: FindingColour): String? = when (colour) {
    FindingColour.RED -> stringResource(R.string.recommended_tag_red)
    FindingColour.AMBER -> stringResource(R.string.recommended_tag_amber)
    FindingColour.GREEN -> null
}

/**
 * The top of a recommendation's row, on Log service and the vehicle's list (#572): a finding's tag and its inspection
 * item, the shop's wording with its price, and what was found, quoted.
 */
@Composable
internal fun RecommendationHead(item: RecommendedItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val colour = item.colour
        val tag = colour?.let { findingTagText(it) }
        if (colour != null && tag != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FindingTag(colour, tag)
                item.area?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else if (item.area != null && item.finding != null) {
            Text(item.area.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(item.text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold)
            item.cents?.let { Text(Money.format(it), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, maxLines = 1) }
        }
        item.finding?.let {
            Text(stringResource(R.string.recommended_finding, it), style = MaterialTheme.typography.bodyMedium, color = statusColors().soon)
        }
    }
}

package io.cosgriff.glovebox.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import kotlinx.coroutines.launch

/**
 * A translucent "↑ Top" pill for a long list, to put at the bottom centre over it: it shows once [state] is
 * more than about a screen down, while the list is going back up or has stopped, and goes while it is
 * scrolled further down. A tap scrolls back to the first item.
 */
@Composable
fun BackToTop(state: LazyListState, modifier: Modifier = Modifier) {
    val far by remember(state) { derivedStateOf { state.pastOneScreen() } }
    TopPill(
        shown = far && (!state.isScrollInProgress || state.lastScrolledBackward),
        modifier = modifier,
        toTop = { state.animateScrollToItem(0) },
    )
}

/**
 * The same pill over a `Column(Modifier.verticalScroll(state))`: a page laid out whole, so how far down it is
 * is known exactly. It shows once [state] is more than a screen (its viewport) down.
 */
@Composable
fun BackToTop(state: ScrollState, modifier: Modifier = Modifier) {
    val far by remember(state) { derivedStateOf { state.viewportSize > 0 && state.value > state.viewportSize } }
    TopPill(
        shown = far && (!state.isScrollInProgress || state.lastScrolledBackward),
        modifier = modifier,
        toTop = { state.animateScrollTo(0) },
    )
}

@Composable
private fun TopPill(shown: Boolean, modifier: Modifier, toTop: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.scroll_back_to_top)
    AnimatedVisibility(
        visible = shown,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        val scheme = MaterialTheme.colorScheme
        Surface(
            onClick = { scope.launch { toTop() } },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
            shape = CircleShape,
            // See-through enough to show the list going under it; the arrow and word stay opaque, so they read.
            // The highest container, as the light theme draws the lower ones white like the cards beneath.
            color = scheme.surfaceContainerHighest.copy(alpha = 0.88f),
            contentColor = scheme.onSurface,
            shadowElevation = 3.dp,
        ) {
            Row(
                Modifier.heightIn(min = 48.dp).padding(start = 14.dp, end = 18.dp).clearAndSetSemantics { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = null)
                Text(stringResource(R.string.scroll_top), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Space to leave at the end of a list or page with [BackToTop] over it, so the button never covers the last item. */
val BackToTopRoom = 64.dp

/**
 * Roughly how far down the list is: the items above the first one showing, at the average height of those
 * on screen, plus how far into it. A lazy list doesn't know the height of what it hasn't drawn.
 */
private fun LazyListState.pastOneScreen(): Boolean {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty() || firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) return false
    val average = visible.sumOf { it.size } / visible.size
    val viewport = info.viewportEndOffset - info.viewportStartOffset
    return firstVisibleItemIndex.toLong() * average + firstVisibleItemScrollOffset > viewport
}

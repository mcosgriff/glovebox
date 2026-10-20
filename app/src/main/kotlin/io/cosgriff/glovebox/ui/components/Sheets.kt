package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Every bottom sheet's handle: Material's, drawn the same, in a slot 48dp wide. The sheet makes the slot
 * its tap target (expand, collapse, dismiss), and Material's own is the 32dp handle alone, under the 48dp
 * a touch target needs (#234). [GloveboxSheet] passes it; a sheet made another way passes it as
 * `dragHandle = { SheetHandle() }`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHandle() {
    Box(Modifier.widthIn(min = 48.dp), contentAlignment = Alignment.Center) {
        BottomSheetDefaults.DragHandle()
    }
}

/** A bottom sheet as the app draws one: opened all the way, in the page colour, with [SheetHandle]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GloveboxSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { SheetHandle() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

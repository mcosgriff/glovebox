package io.cosgriff.glovebox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Every bottom sheet's handle: Material's, drawn the same, in a slot 48dp wide. The sheet makes the slot
 * its tap target (expand, collapse, dismiss), and Material's own is the 32dp handle alone, under the 48dp
 * a touch target needs (#234). Pass it as `dragHandle = { SheetHandle() }`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHandle() {
    Box(Modifier.widthIn(min = 48.dp), contentAlignment = Alignment.Center) {
        BottomSheetDefaults.DragHandle()
    }
}

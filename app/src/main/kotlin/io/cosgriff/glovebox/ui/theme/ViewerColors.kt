package io.cosgriff.glovebox.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The dark colours of the screens that show a picture full screen: the camera screens, the photo viewer and the
 * document viewer. They keep the canvas's own colours in both themes, since what is behind them is a photo or
 * the live camera, not the page.
 */
object ViewerColors {
    val Backdrop = Color(0xFF0E1012)
    val OnBackdrop = Color(0xFFF4F3EF)
    val OnBackdropMuted = Color(0xFFB9B6AE)
}

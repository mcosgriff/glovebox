package io.cosgriff.glovebox.ui.scan

import androidx.camera.core.ImageProxy
import io.cosgriff.glovebox.domain.RecentReads
import io.cosgriff.glovebox.ui.theme.ViewerColors
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.DashReading
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.theme.PlexMono

/**
 * "Read dash" from the Update mileage sheet: the camera is pointed at the instrument cluster and the
 * odometer reading is read off it. No rendering on the canvas beyond the button; follows the VIN
 * scanner. Frames are read on the phone and thrown away; nothing is saved or sent.
 *
 * A reading is only ever handed back to fill the sheet's field. The user still checks it and saves.
 * [last] is the vehicle's latest known reading in dash terms, which tells the odometer from the
 * other numbers on a cluster. [unit] is what the vehicle counts in: the number read is taken to be in
 * it, whatever the dash is showing.
 */
@Composable
fun ReadDashScreen(last: Int?, unit: DistanceUnit, onClose: () -> Unit, onUse: (miles: Int) -> Unit) {
    CameraScreen(
        why = stringResource(R.string.read_dash_camera_why),
        instead = stringResource(R.string.read_dash_type_instead),
        onClose = onClose,
    ) {
        Reader(last, unit, onClose, onUse)
    }
}

@Composable
private fun Reader(last: Int?, unit: DistanceUnit, onClose: () -> Unit, onUse: (Int) -> Unit) {
    /** A reading enough frames agreed on; reading pauses while it is on screen. */
    var found by rememberSaveable { mutableStateOf<Int?>(null) }
    /** The text recogniser couldn't be started on this phone. */
    var unavailable by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    val paused by rememberUpdatedState(found != null)

    val analysis = rememberFrameAnalysis()
    FrameReader(analysis) { context ->
        val reader = TextReader.digits(context)
        // What each recent frame came to; only ever touched on the analyzer's thread.
        val frames = RecentReads<Int?>(DashReading.WINDOW)
        object : FrameJob {
            override fun read(image: ImageProxy) {
                if (paused) {
                    // What was read before "Rescan" says nothing about what is in front of the camera now.
                    frames.clear()
                    return
                }
                val (band, width, height) = image.guideBand()
                val text = reader.read(band, width, height)
                if (reader.failed) {
                    unavailable = true
                    return
                }
                DashReading.steady(frames.add(text?.let { read -> DashReading.pick(read, last, unit) }))?.let { steady -> found = steady }
            }

            override fun close() = reader.close()
        }
    }
    CameraFeed(analysis, torch)

    Column(Modifier.fillMaxSize()) {
        CameraTopBar(stringResource(R.string.read_dash_title), stringResource(R.string.close), torch, onTorch = { torch = it }, onClose = onClose)

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            ScrimText(stringResource(R.string.read_dash_instruction), MaterialTheme.typography.bodyLarge)
            GuideBox()
            ScrimText(
                when {
                    unavailable -> stringResource(R.string.read_dash_unavailable)
                    last != null -> stringResource(R.string.read_dash_hint_from, last.asDistance(unit))
                    else -> stringResource(R.string.read_dash_hint)
                },
                MaterialTheme.typography.bodySmall,
                color = if (unavailable) Guide else ViewerColors.OnBackdropMuted,
                live = true,
            )
        }

        found?.let { miles ->
            FoundCard(
                heading = {
                    // Never "detected" or a tick: a camera can misread a digit, and the dash is right there to check.
                    Text(
                        stringResource(R.string.read_dash_check),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                value = miles.asDistance(unit),
                valueStyle = MaterialTheme.typography.headlineMedium.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                again = stringResource(R.string.read_dash_again),
                onAgain = { found = null },
                use = stringResource(R.string.read_dash_use),
                onUse = { onUse(miles) },
            )
        }
    }
}

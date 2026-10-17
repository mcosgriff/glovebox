package io.cosgriff.glovebox.ui.scan

import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.DashReading
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.theme.PlexMono
import java.util.concurrent.Executors

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
        why = "Glovebox uses the camera here only to read the odometer. The picture is read on this phone and isn't saved or sent anywhere.",
        instead = "Type the reading instead",
        onClose = onClose,
    ) {
        Reader(last, unit, onClose, onUse)
    }
}

@Composable
private fun Reader(last: Int?, unit: DistanceUnit, onClose: () -> Unit, onUse: (Int) -> Unit) {
    val context = LocalContext.current
    /** A reading enough frames agreed on; reading pauses while it is on screen. */
    var found by rememberSaveable { mutableStateOf<Int?>(null) }
    /** The text recogniser couldn't be started on this phone. */
    var unavailable by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    val paused by rememberUpdatedState(found != null)

    val analysis = rememberFrameAnalysis()
    DisposableEffect(analysis) {
        val executor = Executors.newSingleThreadExecutor()
        val reader = TextReader.digits(context.applicationContext)
        // What each recent frame came to; only ever touched on the analyzer's thread.
        val frames = ArrayDeque<Int?>()
        analysis.setAnalyzer(executor) { image ->
            image.use {
                if (paused) {
                    // What was read before "Rescan" says nothing about what is in front of the camera now.
                    frames.clear()
                    return@use
                }
                val (band, width, height) = it.guideBand()
                val text = reader.read(band, width, height)
                if (reader.failed) {
                    unavailable = true
                    return@use
                }
                frames.addLast(text?.let { read -> DashReading.pick(read, last, unit) })
                while (frames.size > DashReading.WINDOW) frames.removeFirst()
                DashReading.steady(frames.toList())?.let { steady -> found = steady }
            }
        }
        onDispose {
            analysis.clearAnalyzer()
            // The engine is closed on the thread that uses it, after any frame still being read.
            executor.execute { reader.close() }
            executor.shutdown()
        }
    }
    CameraFeed(analysis, torch)

    Column(Modifier.fillMaxSize()) {
        CameraTopBar("Read dash", "Close", torch, onTorch = { torch = it }, onClose = onClose)

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text(
                "Put the odometer reading\nin the box",
                style = MaterialTheme.typography.bodyLarge,
                color = OnScrim,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Scrim.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            // Only a band across the middle of the frame is read; this box sits inside it.
            Box(Modifier.fillMaxWidth().height(150.dp).border(3.dp, Guide, RoundedCornerShape(18.dp)))
            Text(
                when {
                    unavailable -> "Reading the dash doesn't work on this phone. Close this and type the reading."
                    last != null -> "Hold it straight on and steady, with the digits filling the box. " +
                        "It looks for a reading of ${last.asDistance(unit)} or more; if the dash shows less, close this and type it."
                    else -> "Hold it straight on and steady. Fill the box with the digits and keep other numbers out of it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (unavailable) Guide else OnScrimMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Scrim.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        found?.let { miles ->
            Surface(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Never "detected" or a tick: a camera can misread a digit, and the dash is right there to check.
                    Text(
                        "This is what the camera read. Check it against the dash.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(miles.asDistance(unit), style = MaterialTheme.typography.headlineMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { found = null },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            // The default padding leaves too little of a third of the row for two words on one line.
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text("Read again", maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { onUse(miles) },
                            modifier = Modifier.weight(2f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text("Use this reading", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

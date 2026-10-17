package io.cosgriff.glovebox.ui.scan

import io.cosgriff.glovebox.ui.theme.PlexMono
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.BarcodeSeen
import io.cosgriff.glovebox.domain.ScanReport
import io.cosgriff.glovebox.domain.VinScan
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import io.cosgriff.glovebox.ui.theme.statusColors
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.Executors

/**
 * Design: 2 · Scan VIN. Reads the VIN from a barcode (ZXing) or, with no barcode in view, from print
 * (Tesseract, as the dash reader does). Frames are read on the phone; nothing is saved or sent.
 */
@HiltViewModel
class ScanVinViewModel @Inject constructor(repo: SettingsRepository) : ViewModel() {
    /** Settings' "Show what the scanner sees". */
    val details: StateFlow<Boolean> = repo.settings.map { it.scannerDetails }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

@Composable
fun ScanVinScreen(onClose: () -> Unit, onUseVin: (String) -> Unit, vm: ScanVinViewModel = hiltViewModel()) {
    val details by vm.details.collectAsStateWithLifecycle()
    CameraScreen(
        why = "Glovebox uses the camera here only to read the VIN, from a barcode or from print. The picture is read on this phone and isn't saved or sent anywhere.",
        instead = "Type the VIN instead",
        onClose = onClose,
    ) {
        Scanner(details, onClose, onUseVin)
    }
}

@Composable
private fun Scanner(details: Boolean, onClose: () -> Unit, onUseVin: (String) -> Unit) {
    val context = LocalContext.current
    /** A VIN that passed its check digit; scanning pauses while it is on screen. */
    var found by rememberSaveable { mutableStateOf<String?>(null) }
    /** [found] was read from print, not from a barcode. */
    var fromPrint by rememberSaveable { mutableStateOf(false) }
    /** A barcode was read but it wasn't a VIN. */
    var notAVin by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    // What the scanner is doing, for the panel under the guide. Written from the analyzer's thread.
    var rate by remember { mutableStateOf<Int?>(null) }
    var seen by remember { mutableStateOf<BarcodeSeen?>(null) }
    var printText by remember { mutableStateOf<String?>(null) }
    var printRan by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf<BandView?>(null) }
    val paused by rememberUpdatedState(found != null)
    val drawing by rememberUpdatedState(details)

    val analysis = rememberFrameAnalysis()
    DisposableEffect(analysis) {
        val executor = Executors.newSingleThreadExecutor()
        val reader = VinBarcodeReader()
        val print = TextReader.vin(context.applicationContext)
        // What each recent frame's print and barcode came to; only ever touched on the analyzer's thread.
        val printed = ArrayDeque<String?>()
        val barcodes = ArrayDeque<String>()
        var frame = 0
        // For the panel: frames counted over the last second, and when a barcode was last in view.
        var counted = 0
        var second = System.currentTimeMillis()
        var lastSeen: VinBarcodeReader.Sighting? = null
        var lastSeenAt = 0L
        rate = 0
        analysis.setAnalyzer(executor) { image ->
            image.use {
                val now = System.currentTimeMillis()
                counted++
                if (now - second >= 1000) {
                    rate = (counted * 1000 / (now - second)).toInt()
                    counted = 0
                    second = now
                }
                if (paused) {
                    // What was read before "Rescan" says nothing about what is in front of the camera now.
                    printed.clear()
                    barcodes.clear()
                    return@use
                }
                val (band, width, height) = it.guideBand()
                // A barcode first: it is quicker to read and carries its own error checking.
                val sighting = reader.look(band, width, height)
                // A barcode stays reported for a moment after it leaves the frame, so the panel doesn't flicker.
                if (sighting != null) {
                    lastSeen = sighting
                    lastSeenAt = now
                } else if (now - lastSeenAt > HOLD_MILLIS) {
                    lastSeen = null
                }
                seen = lastSeen?.let { s -> BarcodeSeen(s.format, s.text) }
                if (drawing && counted % THUMB_EVERY == 0) view = BandView.of(band, width, height, lastSeen)
                val barcode = sighting?.text
                if (barcode != null) {
                    val vin = VinScan.vin(barcode)
                    if (vin != null) {
                        fromPrint = false
                        found = vin
                    } else {
                        barcodes.addLast(barcode)
                        while (barcodes.size > VinScan.WINDOW) barcodes.removeFirst()
                        notAVin = VinScan.notAVin(barcodes.toList())
                    }
                    return@use
                }
                // The print reader is slow: give the barcode reader most of the frames.
                if (frame++ % VinScan.PRINT_EVERY != 0) return@use
                // No barcode in the frame: look for the VIN in print. If the recogniser can't run on
                // this phone it reads nothing, and the scanner is a barcode scanner as before.
                val text = print.read(band, width, height)
                printRan = true
                printText = text
                printed.addLast(text?.let(VinScan::printed))
                while (printed.size > VinScan.WINDOW) printed.removeFirst()
                VinScan.agreed(printed.toList())?.let { vin ->
                    fromPrint = true
                    found = vin
                }
            }
        }
        onDispose {
            analysis.clearAnalyzer()
            // The recogniser is closed on the thread that uses it, after any frame still being read.
            executor.execute { print.close() }
            executor.shutdown()
        }
    }
    CameraFeed(analysis, torch)

    Column(Modifier.fillMaxSize()) {
        CameraTopBar("Scan VIN", "Close scanner", torch, onTorch = { torch = it }, onClose = onClose)

        val instruction = @Composable {
            Text(
                "Line up the barcode on the door-jamb\nsticker, or the VIN printed on the dash",
                style = MaterialTheme.typography.bodyLarge,
                color = OnScrim,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Scrim.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        // Only a band across the middle of the frame is read; this box sits inside it.
        val guide = @Composable { Box(Modifier.fillMaxWidth().height(150.dp).border(3.dp, Guide, RoundedCornerShape(18.dp))) }
        val panel = @Composable { if (details && found == null) ScanPanel(view, rate, seen, printText.takeIf { printRan }, printRan) }
        val hint = @Composable {
            Text(
                if (notAVin && found == null) "That barcode isn't a VIN. Aim at the one on the door-jamb sticker." else "Code 39 or QR · also reads printed VINs",
                style = MaterialTheme.typography.bodySmall,
                color = if (notAVin && found == null) Guide else OnScrimMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Scrim.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth > maxHeight) {
                // Held sideways there is no room for the panel under the guide: it goes beside it.
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        Modifier.weight(3f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        instruction()
                        guide()
                        hint()
                    }
                    if (details) Box(Modifier.weight(2f)) { panel() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                ) {
                    instruction()
                    guide()
                    panel()
                    hint()
                }
            }
        }

        found?.let { vin ->
            Surface(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = statusColors().ok)
                        Text(
                            if (fromPrint) "VIN read from print · check digit valid" else "VIN detected · check digit valid",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColors().ok,
                        )
                    }
                    Text(vin, style = MaterialTheme.typography.titleLarge, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                found = null
                                notAVin = false
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        ) {
                            Text("Rescan")
                        }
                        Button(onClick = { onUseVin(vin) }, modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                            Text("Use this VIN")
                        }
                    }
                }
            }
        }
    }
}

/** How long a barcode stays reported after the last frame it was in. */
private const val HOLD_MILLIS = 1000L

/** The panel's picture of what is read is redrawn on one frame in this many. */
private const val THUMB_EVERY = 3

/**
 * A small greyscale copy of the band being read, for the panel, with the outline of the barcode in
 * it, if any, as fractions of the band. Held in memory while the scanner is open, never stored.
 */
internal class BandView(val image: ImageBitmap, val aspect: Float, val corners: List<Offset>?, val read: Boolean) {
    companion object {
        private const val WIDTH = 360

        fun of(band: ByteArray, width: Int, height: Int, sighting: VinBarcodeReader.Sighting?): BandView {
            val step = (width / WIDTH).coerceAtLeast(1)
            val w = width / step
            val h = height / step
            val pixels = IntArray(w * h) { i ->
                val l = band[(i / w) * step * width + (i % w) * step].toInt() and 0xFF
                (0xFF shl 24) or (l shl 16) or (l shl 8) or l
            }
            val image = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
            val corners = sighting?.corners?.map { Offset(it.x.toFloat() / width, it.y.toFloat() / height) }
            return BandView(image, width.toFloat() / height, corners, sighting?.text != null)
        }
    }
}

/**
 * What the scanner is doing, so a scan that finds nothing is seen to be trying: the strip of the
 * frame being read, with a barcode outlined in it (green read, amber found but not readable), how
 * many frames a second are read, and what the last barcode and print reads came to.
 */
@Composable
private fun ScanPanel(view: BandView?, rate: Int?, seen: BarcodeSeen?, print: String?, printRan: Boolean) {
    val ok = statusColors().ok
    Column(
        Modifier.fillMaxWidth()
            .background(Scrim.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(120.dp).aspectRatio(view?.aspect ?: 1.5f).background(Color.Black, RoundedCornerShape(6.dp))) {
                view?.let { v ->
                    Image(v.image, contentDescription = null, modifier = Modifier.fillMaxSize())
                    v.corners?.let { corners ->
                        Canvas(Modifier.fillMaxSize()) {
                            val path = Path().apply {
                                corners.forEachIndexed { i, c ->
                                    val p = Offset(c.x * size.width, c.y * size.height)
                                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                }
                                close()
                            }
                            drawPath(path, if (v.read) ok else Guide, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("WHAT THE SCANNER SEES", style = MaterialTheme.typography.labelSmall, color = OnScrimMuted)
                Text(ScanReport.rate(rate), style = MaterialTheme.typography.bodySmall, color = OnScrim)
            }
        }
        Text(
            ScanReport.barcode(seen),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = PlexMono,
            color = when {
                seen == null -> OnScrimMuted
                seen.text == null -> Guide
                else -> OnScrim
            },
        )
        Text(
            if (printRan) ScanReport.print(print) else "Print is read when no barcode is in the box",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = PlexMono,
            color = if (print.isNullOrBlank()) OnScrimMuted else OnScrim,
        )
    }
}


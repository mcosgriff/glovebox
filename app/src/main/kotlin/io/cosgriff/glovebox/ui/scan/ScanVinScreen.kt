package io.cosgriff.glovebox.ui.scan

import io.cosgriff.glovebox.ui.theme.PlexMono
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import java.util.concurrent.Executors

// The scanner sits over a live camera picture, so it keeps the canvas's own colours in both themes.
// Shared with the dash reader (ReadDashScreen), which is the same kind of screen.
internal val Scrim = Color(0xFF0E1012)
internal val OnScrim = Color(0xFFF4F3EF)
internal val OnScrimMuted = Color(0xFFB9B6AE)
internal val Guide = Color(0xFFFDBA74)
internal val Ink = Color(0xFF17191C)

// The part of the frame that is read: a band across the middle, comfortably larger than the guide
// box drawn over it, since the preview is cropped to fill the screen and the two don't line up exactly.
internal const val GUIDE_WIDTH = 0.8
internal const val GUIDE_HEIGHT = 0.3

/** Door-jamb barcodes are small; zoomed in, the phone can sit far enough back to focus on one. */
private const val START_ZOOM = 2f

/**
 * Design: 2 · Scan VIN. Reads the VIN from a barcode (ZXing) or, with no barcode in view, from print
 * (Tesseract, as the dash reader does). Frames are read on the phone; nothing is saved or sent.
 */
@Composable
fun ScanVinScreen(onClose: () -> Unit, onUseVin: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasCamera()) }
    // Set once the user has answered the system prompt with no.
    var refused by rememberSaveable { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        refused = !ok
    }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }
    // Coming back from Settings with the permission switched on.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) granted = context.hasCamera()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Scrim)) {
        if (granted) {
            Scanner(onClose, onUseVin)
        } else {
            NoCamera(
                why = "Glovebox uses the camera here only to read the VIN, from a barcode or from print. The picture is read on this phone and isn't saved or sent anywhere.",
                instead = "Type the VIN instead",
                refused = refused,
                onAsk = { ask.launch(Manifest.permission.CAMERA) },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun Scanner(onClose: () -> Unit, onUseVin: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    val hasFlash = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            // A 17-character Code 39 barcode is wide; the default analysis size is too coarse to resolve its bars.
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build()
        }
    }
    val turn = rememberFrameTurn(controller)
    DisposableEffect(controller, lifecycleOwner) {
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
        controller.setImageAnalysisAnalyzer(executor) { image ->
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
                val (band, width, height) = it.guideBand(turn)
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
                if (counted % THUMB_EVERY == 0) view = BandView.of(band, width, height, lastSeen)
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
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            // The recogniser is closed on the thread that uses it, after any frame still being read.
            executor.execute { print.close() }
            executor.shutdown()
        }
    }
    LaunchedEffect(torch) { controller.enableTorch(torch) }
    // Zoom in once the camera is up and says how far it can go. Pinch still changes it afterwards.
    DisposableEffect(controller, lifecycleOwner) {
        var zoomed = false
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { state ->
            if (!zoomed && state != null) {
                zoomed = true
                controller.setZoomRatio(START_ZOOM.coerceIn(state.minZoomRatio, state.maxZoomRatio))
            }
        }
        controller.zoomState.observe(lifecycleOwner, observer)
        onDispose { controller.zoomState.removeObserver(observer) }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        modifier = Modifier.fillMaxSize(),
    )

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Outlined.Close, "Close scanner", container = Color.Black.copy(alpha = 0.45f), content = OnScrim, onClick = onClose)
            Text(
                "Scan VIN",
                Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnScrim,
                textAlign = TextAlign.Center,
            )
            if (hasFlash) {
                RoundButton(
                    Icons.Outlined.FlashOn,
                    if (torch) "Turn flashlight off" else "Turn flashlight on",
                    container = if (torch) Guide else Color.Black.copy(alpha = 0.45f),
                    content = if (torch) Ink else OnScrim,
                    onClick = { torch = !torch },
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text(
                "Line up the barcode on the door-jamb\nsticker, or the VIN printed on the dash",
                style = MaterialTheme.typography.bodyLarge,
                color = OnScrim,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Scrim.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            // Only a band across the middle of the frame is read; this box sits inside it.
            Box(Modifier.fillMaxWidth().height(150.dp).border(3.dp, Guide, RoundedCornerShape(18.dp)))
            if (found == null) ScanPanel(view, rate, seen, printText.takeIf { printRan }, printRan)
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

/** [why] says what the camera is for on this screen; [instead] is the way on without it. */
@Composable
internal fun NoCamera(why: String, instead: String, refused: Boolean, onAsk: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            RoundButton(Icons.Outlined.Close, "Close scanner", container = Color.Black.copy(alpha = 0.45f), content = OnScrim, onClick = onClose)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text("Camera access is off", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnScrim)
            Text(why, color = OnScrimMuted, textAlign = TextAlign.Center)
            if (refused) {
                // After a refusal Android may stop showing its prompt, so Settings is the reliable way.
                Button(onClick = { context.openAppSettings() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("Open app settings")
                }
                TextButton(onClick = onAsk, colors = ButtonDefaults.textButtonColors(contentColor = Guide)) { Text("Ask again") }
            } else {
                Button(onClick = onAsk, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("Allow camera")
                }
            }
            TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = OnScrim)) { Text(instead) }
        }
    }
}

@Composable
internal fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, container: Color, content: Color, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
    ) {
        Icon(icon, contentDescription = label)
    }
}

/** How far each camera frame must turn clockwise to stand as the preview shows it; null until the camera is up. */
internal class FrameTurn {
    @Volatile var degrees: Int? = null
}

/**
 * Keeps [FrameTurn] to the screen. A [CameraController] marks its frames with the way the motion sensor
 * says the phone is held, not the way the screen is showing them, so a phone tipped while aimed at a
 * door jamb or a dash had its frames turned a quarter from the picture on screen: a barcode level on
 * screen reached the reader with its bars along the rows, and the band held only a slice of it. The
 * user lines the guide up on the screen, so frames are turned as the screen is.
 */
@Composable
internal fun rememberFrameTurn(controller: CameraController): FrameTurn {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val turn = remember { FrameTurn() }
    DisposableEffect(controller, lifecycleOwner) {
        // The camera's details are there once it is bound, which is when its zoom state first arrives.
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> {
            val rotation = ContextCompat.getDisplayOrDefault(context).rotation
            controller.cameraInfo?.let { info -> turn.degrees = info.getSensorRotationDegrees(rotation) }
        }
        controller.zoomState.observe(lifecycleOwner, observer)
        onDispose { controller.zoomState.removeObserver(observer) }
    }
    return turn
}

/**
 * The part of a frame the on-screen guide sits in: its greyscale plane, turned upright as the screen
 * shows it ([turn]) and cut to the band across the middle.
 */
internal fun ImageProxy.guideBand(turn: FrameTurn): Triple<ByteArray, Int, Int> {
    val image = this
    val plane = image.planes[0]
    val width = image.width
    val height = image.height
    val buffer = plane.buffer
    // Rows can be padded past the picture's width; copy just the pixels.
    val pixels = ByteArray(width * height)
    for (row in 0 until height) {
        buffer.position(row * plane.rowStride)
        buffer.get(pixels, row * width, width)
    }
    val (upright, w, h) = VinBarcodeReader.rotate(pixels, width, height, turn.degrees ?: image.imageInfo.rotationDegrees)
    return VinBarcodeReader.centre(upright, w, h, GUIDE_WIDTH, GUIDE_HEIGHT)
}

internal fun Context.hasCamera(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

internal fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
}

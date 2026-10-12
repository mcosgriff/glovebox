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
import io.cosgriff.glovebox.domain.VinScan
import io.cosgriff.glovebox.ui.theme.statusColors
import java.util.concurrent.Executors

// The scanner sits over a live camera picture, so it keeps the canvas's own colours in both themes.
private val Scrim = Color(0xFF0E1012)
private val OnScrim = Color(0xFFF4F3EF)
private val OnScrimMuted = Color(0xFFB9B6AE)
private val Guide = Color(0xFFFDBA74)
private val Ink = Color(0xFF17191C)

// The part of the frame that is read: a band across the middle, comfortably larger than the guide
// box drawn over it, since the preview is cropped to fill the screen and the two don't line up exactly.
private const val GUIDE_WIDTH = 0.8
private const val GUIDE_HEIGHT = 0.3

/** Door-jamb barcodes are small; zoomed in, the phone can sit far enough back to focus on one. */
private const val START_ZOOM = 2f

/** Design: 2 · Scan VIN. Frames are read on the phone; nothing is saved or sent. */
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
            NoCamera(refused = refused, onAsk = { ask.launch(Manifest.permission.CAMERA) }, onClose = onClose)
        }
    }
}

@Composable
private fun Scanner(onClose: () -> Unit, onUseVin: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    /** A VIN that passed its check digit; scanning pauses while it is on screen. */
    var found by rememberSaveable { mutableStateOf<String?>(null) }
    /** A barcode was read but it wasn't a VIN. */
    var notAVin by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
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
    DisposableEffect(controller, lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val reader = VinBarcodeReader()
        controller.setImageAnalysisAnalyzer(executor) { image ->
            image.use {
                if (paused) return@use
                val text = reader.read(it) ?: return@use
                val vin = VinScan.vin(text)
                if (vin != null) found = vin else notAVin = true
            }
        }
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
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
                "Line up the barcode on the\ndriver's door-jamb sticker",
                style = MaterialTheme.typography.bodyLarge,
                color = OnScrim,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Scrim.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            // Only a band across the middle of the frame is read; this box sits inside it.
            Box(Modifier.fillMaxWidth().height(150.dp).border(3.dp, Guide, RoundedCornerShape(18.dp)))
            Text(
                if (notAVin && found == null) "That barcode isn't a VIN. Aim at the one on the door-jamb sticker." else "Code 39 or QR",
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
                            "VIN detected · check digit valid",
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

@Composable
private fun NoCamera(refused: Boolean, onAsk: () -> Unit, onClose: () -> Unit) {
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
            Text(
                "Glovebox uses the camera only to read the VIN barcode. The picture is read on this phone and isn't saved or sent anywhere.",
                color = OnScrimMuted,
                textAlign = TextAlign.Center,
            )
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
            TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = OnScrim)) { Text("Type the VIN instead") }
        }
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, container: Color, content: Color, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
    ) {
        Icon(icon, contentDescription = label)
    }
}

/** The frame's greyscale plane, turned upright, handed to ZXing. */
private fun VinBarcodeReader.read(image: ImageProxy): String? {
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
    val (upright, w, h) = VinBarcodeReader.rotate(pixels, width, height, image.imageInfo.rotationDegrees)
    val (guide, gw, gh) = VinBarcodeReader.centre(upright, w, h, GUIDE_WIDTH, GUIDE_HEIGHT)
    return read(guide, gw, gh)
}

private fun Context.hasCamera(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
}

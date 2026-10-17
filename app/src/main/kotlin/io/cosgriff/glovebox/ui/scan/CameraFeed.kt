package io.cosgriff.glovebox.ui.scan

import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation

// The camera screens (the VIN scanner and the dash reader) sit over a live camera picture, so they keep
// the canvas's own colours in both themes.
internal val Scrim = Color(0xFF0E1012)
internal val OnScrim = Color(0xFFF4F3EF)
internal val OnScrimMuted = Color(0xFFB9B6AE)
internal val Guide = Color(0xFFFDBA74)
internal val Ink = Color(0xFF17191C)

// The part of the frame that is read: a band across the middle, comfortably larger than the guide
// box drawn over it, since the preview is cropped to fill the screen and the two don't line up exactly.
internal const val GUIDE_WIDTH = 0.8
internal const val GUIDE_HEIGHT = 0.3

/**
 * Door-jamb barcodes and odometer digits are small; zoomed in, the phone can sit far enough back to
 * focus on one, or the driver can fill the guide from their seat. Pinch still changes it.
 */
private const val START_ZOOM = 2f

/**
 * A camera screen: the dark page, the camera permission, and [content] once the camera may be used.
 * [why] says what the camera is for on this screen; [instead] is the way on without it.
 */
@Composable
internal fun CameraScreen(why: String, instead: String, onClose: () -> Unit, content: @Composable () -> Unit) {
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
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasCamera()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Scrim)) {
        if (granted) {
            content()
        } else {
            NoCamera(why, instead, refused, onAsk = { ask.launch(Manifest.permission.CAMERA) }, onClose = onClose)
        }
    }
}

/**
 * The frames the screen reads. A frame is turned upright by its `rotationDegrees`, which CameraX works
 * out from the use case's target rotation; [CameraFeed] keeps that to the way the screen is turned.
 */
@Composable
internal fun rememberFrameAnalysis(): ImageAnalysis = remember {
    ImageAnalysis.Builder()
        // A 17-character Code 39 barcode is wide and a cluster's digits are small in the frame; the
        // default analysis size is too coarse for either.
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build(),
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
}

/**
 * The back camera's picture filling the screen, with [analysis] bound beside it. Starts at [START_ZOOM];
 * pinch zooms and a tap focuses there. [torch] is the flashlight.
 *
 * **Frames are turned as the screen shows them, not as the motion sensor says the phone is held.** The
 * camera controller this replaced (#223) set its frames' rotation from the motion sensor, so a phone
 * tipped while aimed at a door jamb or a dash had its frames turned a quarter from the picture on
 * screen: a barcode level on screen reached the reader with its bars along the rows, and the band held
 * only a slice of it. The user lines the guide up on the screen, so the target rotation of both use
 * cases is the display's, and is followed when the screen turns.
 */
@Composable
internal fun CameraFeed(analysis: ImageAnalysis, torch: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var request by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val preview = remember { Preview.Builder().build().apply { setSurfaceProvider { request = it } } }

    LaunchedEffect(analysis, lifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        try {
            camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                .also { bound ->
                    val zoom = bound.cameraInfo.zoomState.value
                    bound.cameraControl.setZoomRatio(zoom?.let { START_ZOOM.coerceIn(it.minZoomRatio, it.maxZoomRatio) } ?: START_ZOOM)
                }
            awaitCancellation()
        } catch (_: IllegalArgumentException) {
            // No back camera: the screen stays dark and its text still offers the way on by hand.
        } finally {
            provider.unbind(preview, analysis)
            camera = null
        }
    }
    LaunchedEffect(camera, torch) { camera?.cameraControl?.enableTorch(torch) }
    // Turning from one landscape straight to the other doesn't recreate the activity (its size is the
    // same), so the screen's turn is followed here; otherwise frames would arrive upside down.
    DisposableEffect(preview, analysis) {
        val display = ContextCompat.getDisplayOrDefault(context)
        val update = {
            preview.targetRotation = display.rotation
            analysis.targetRotation = display.rotation
        }
        update()
        val displays = context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == display.displayId) update()
            }
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
        }
        displays.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displays.unregisterDisplayListener(listener) }
    }

    val surface = request ?: return
    val transformer = remember { MutableCoordinateTransformer() }
    CameraXViewfinder(
        surfaceRequest = surface,
        // Drawn in Compose, so it clips and moves with the screen during a predictive back swipe.
        implementationMode = ImplementationMode.EMBEDDED,
        coordinateTransformer = transformer,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(camera) {
                val control = camera?.cameraControl ?: return@pointerInput
                detectTapGestures { tap ->
                    val point = with(transformer) { tap.transform() }
                    val factory = SurfaceOrientedMeteringPointFactory(surface.resolution.width.toFloat(), surface.resolution.height.toFloat())
                    control.startFocusAndMetering(FocusMeteringAction.Builder(factory.createPoint(point.x, point.y)).build())
                }
            }
            .pointerInput(camera) {
                val bound = camera ?: return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    val state = bound.cameraInfo.zoomState.value ?: return@detectTransformGestures
                    bound.cameraControl.setZoomRatio((state.zoomRatio * zoom).coerceIn(state.minZoomRatio, state.maxZoomRatio))
                }
            },
    )
}

/** The row along the top of a camera screen: close, [title], and the flashlight where there is one. */
@Composable
internal fun CameraTopBar(title: String, closeLabel: String, torch: Boolean, onTorch: (Boolean) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val hasFlash = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(Icons.Outlined.Close, closeLabel, container = Color.Black.copy(alpha = 0.45f), content = OnScrim, onClick = onClose)
        Text(
            title,
            Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnScrim,
            textAlign = TextAlign.Center,
        )
        if (hasFlash) {
            RoundButton(
                Icons.Outlined.FlashOn,
                stringResource(if (torch) R.string.scan_flashlight_off else R.string.scan_flashlight_on),
                container = if (torch) Guide else Color.Black.copy(alpha = 0.45f),
                content = if (torch) Ink else OnScrim,
                onClick = { onTorch(!torch) },
            )
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun NoCamera(why: String, instead: String, refused: Boolean, onAsk: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            RoundButton(Icons.Outlined.Close, stringResource(R.string.scan_close_scanner), container = Color.Black.copy(alpha = 0.45f), content = OnScrim, onClick = onClose)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.scan_camera_off), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnScrim)
            Text(why, color = OnScrimMuted, textAlign = TextAlign.Center)
            if (refused) {
                // After a refusal Android may stop showing its prompt, so Settings is the reliable way.
                Button(onClick = { context.openAppSettings() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(R.string.scan_open_settings))
                }
                TextButton(onClick = onAsk, colors = ButtonDefaults.textButtonColors(contentColor = Guide)) { Text(stringResource(R.string.scan_ask_again)) }
            } else {
                Button(onClick = onAsk, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(R.string.scan_allow_camera))
                }
            }
            TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = OnScrim)) { Text(instead) }
        }
    }
}

@Composable
internal fun RoundButton(icon: ImageVector, label: String, container: Color, content: Color, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
    ) {
        Icon(icon, contentDescription = label)
    }
}

/**
 * The part of a frame the on-screen guide sits in: its greyscale plane, turned upright as the screen
 * shows it (see [CameraFeed]) and cut to the band across the middle.
 */
internal fun ImageProxy.guideBand(): Triple<ByteArray, Int, Int> {
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
    val (upright, w, h) = VinBarcodeReader.rotate(pixels, width, height, image.imageInfo.rotationDegrees)
    return VinBarcodeReader.centre(upright, w, h, GUIDE_WIDTH, GUIDE_HEIGHT)
}

internal fun Context.hasCamera(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

internal fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
}

package io.cosgriff.glovebox.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.cosgriff.glovebox.domain.DashReading
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.theme.PlexMono
import java.util.concurrent.Executors

/** Close enough to fill the guide with the digits from where the driver sits. Pinch still changes it. */
private const val START_ZOOM = 2f

/**
 * "Read dash" from the Update mileage sheet: the camera is pointed at the instrument cluster and the
 * odometer reading is read off it. No rendering on the canvas beyond the button; follows the VIN
 * scanner. Frames are read on the phone and thrown away; nothing is saved or sent.
 *
 * A reading is only ever handed back to fill the sheet's field. The user still checks it and saves.
 * [last] is the vehicle's latest known reading in dash terms, which tells the odometer from the
 * other numbers on a cluster.
 */
@Composable
fun ReadDashScreen(last: Int?, onClose: () -> Unit, onUse: (miles: Int) -> Unit) {
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
            Reader(last, onClose, onUse)
        } else {
            NoCamera(
                why = "Glovebox uses the camera here only to read the odometer. The picture is read on this phone and isn't saved or sent anywhere.",
                instead = "Type the reading instead",
                refused = refused,
                onAsk = { ask.launch(Manifest.permission.CAMERA) },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun Reader(last: Int?, onClose: () -> Unit, onUse: (Int) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    /** A reading enough frames agreed on; reading pauses while it is on screen. */
    var found by rememberSaveable { mutableStateOf<Int?>(null) }
    /** The text recogniser couldn't be started on this phone. */
    var unavailable by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    val paused by rememberUpdatedState(found != null)
    val hasFlash = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            // Digits on a cluster are small in the frame; the default analysis size is too coarse for them.
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build()
        }
    }
    DisposableEffect(controller, lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val reader = DashReader(context.applicationContext)
        // What each recent frame came to; only ever touched on the analyzer's thread.
        val frames = ArrayDeque<Int?>()
        controller.setImageAnalysisAnalyzer(executor) { image ->
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
                frames.addLast(text?.let { read -> DashReading.pick(read, last) })
                while (frames.size > DashReading.WINDOW) frames.removeFirst()
                DashReading.steady(frames.toList())?.let { steady -> found = steady }
            }
        }
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            // The engine is closed on the thread that uses it, after any frame still being read.
            executor.execute { reader.close() }
            executor.shutdown()
        }
    }
    LaunchedEffect(torch) { controller.enableTorch(torch) }
    // Zoom in once the camera is up and says how far it can go.
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
            RoundButton(Icons.Outlined.Close, "Close", container = Color.Black.copy(alpha = 0.45f), content = OnScrim, onClick = onClose)
            Text(
                "Read dash",
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
                        "It looks for a reading of ${last.asMiles()} or more; if the dash shows less, close this and type it."
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
                    Text(miles.asMiles(), style = MaterialTheme.typography.headlineMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
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

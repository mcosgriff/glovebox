package io.cosgriff.glovebox.ui.photo

import io.cosgriff.glovebox.ui.theme.ViewerColors
import io.cosgriff.glovebox.ui.handOut
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.ui.PhotoViewerRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    service: ServiceDao,
    private val store: RecordPhotoStore,
) : ViewModel() {
    private val route = savedStateHandle.route<PhotoViewerRoute>()
    val start: Int = route.start

    /** The record's photo files in the order the grid shows them. Null while loading. */
    val files: StateFlow<List<String>?> = service.observeRecord(route.recordId)
        .map { record -> record?.photos.orEmpty().map { it.fileName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A decrypted copy in the private cache for the share sheet; null if the photo can't be read. */
    suspend fun shareCopy(fileName: String): File? = store.shareCopy(fileName)
}

// The viewer is always dark, like the canvas: a receipt is easier to read against black.

/** Design: the receipt viewer on 6a · Service record. */
@Composable
fun PhotoViewerScreen(onClose: () -> Unit, vm: PhotoViewerViewModel = hiltViewModel(), loader: PhotoLoaderViewModel = hiltViewModel()) {
    val files = vm.files.collectAsStateWithLifecycle().value ?: return // first frame while loading
    if (files.isEmpty()) {
        // The record, or its last photo, went while the viewer was open.
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = vm.start.coerceIn(0, files.lastIndex)) { files.size }
    var zoomed by remember { mutableStateOf(false) }
    var confirmingShare by rememberSaveable { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }

    if (confirmingShare) {
        AlertDialog(
            onDismissRequest = { confirmingShare = false },
            title = { Text(stringResource(R.string.photo_share_title)) },
            text = {
                Text(
                    stringResource(R.string.photo_share_body),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingShare = false
                        val name = files[pager.currentPage.coerceIn(0, files.lastIndex)]
                        scope.launch {
                            val copy = vm.shareCopy(name)
                            if (copy == null || !context.share(copy)) shareFailed = true
                        }
                    },
                ) { Text(stringResource(R.string.share)) }
            },
            dismissButton = { TextButton(onClick = { confirmingShare = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (shareFailed) {
        AlertDialog(
            onDismissRequest = { shareFailed = false },
            text = { Text(stringResource(R.string.photo_share_failed)) },
            confirmButton = { TextButton(onClick = { shareFailed = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    Column(Modifier.fillMaxSize().background(ViewerColors.Backdrop)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close), tint = ViewerColors.OnBackdrop) }
            Text(
                if (files.size == 1) stringResource(R.string.photo) else stringResource(R.string.photo_title_of, (pager.currentPage + 1).asNumber(), files.size.asNumber()),
                Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ViewerColors.OnBackdrop,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { confirmingShare = true }) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.photo_share), tint = ViewerColors.OnBackdrop) }
        }
        // Swiping moves between photos only while the one on screen isn't zoomed in.
        HorizontalPager(state = pager, userScrollEnabled = !zoomed, modifier = Modifier.weight(1f)) { page ->
            ZoomablePhoto(
                fileName = files[page],
                description = stringResource(R.string.photo_of, (page + 1).asNumber(), files.size.asNumber()),
                loader = loader,
                onZoomChange = { if (page == pager.currentPage) zoomed = it },
            )
        }
    }
}

@Composable
private fun ZoomablePhoto(fileName: String, description: String, loader: PhotoLoaderViewModel, onZoomChange: (Boolean) -> Unit) {
    val image by produceState<ImageBitmap?>(null, fileName) { value = loader.full(fileName) }
    ZoomableImage(image, description, key = fileName, onZoomChange = onZoomChange)
}

/**
 * A picture that fills the space it is given: pinch or double-tap to zoom, and one finger pans once
 * zoomed in (otherwise a swipe is left for a pager around it). Blank while [image] is still null.
 * Zoom starts again whenever [key] changes.
 */
@Composable
fun ZoomableImage(image: ImageBitmap?, description: String, key: Any, onZoomChange: (Boolean) -> Unit = {}) {
    var scale by remember(key) { mutableFloatStateOf(1f) }
    var offset by remember(key) { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { _, zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + panChange
        onZoomChange(scale > 1f)
    }
    Box(
        Modifier.fillMaxSize()
            .transformable(transform, canPan = { scale > 1f })
            .pointerInput(key) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                        onZoomChange(scale > 1f)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        image?.let {
            Image(
                it,
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            )
        }
    }
}

/** False if nothing on the phone can take it. */
private fun Context.share(copy: File): Boolean = handOut(copy, "image/jpeg", share = true)

package io.cosgriff.glovebox.ui.photo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.RecordPhotos
import javax.inject.Inject

/** Decrypts photos for whichever screen is showing them. Nothing is kept once the screen closes. */
@HiltViewModel
class PhotoLoaderViewModel @Inject constructor(private val store: RecordPhotoStore) : ViewModel() {
    private val thumbnails = mutableMapOf<String, ImageBitmap>()

    suspend fun thumbnail(fileName: String): ImageBitmap? =
        thumbnails[fileName] ?: store.load(fileName, RecordPhotos.THUMBNAIL_EDGE)?.asImageBitmap()?.also { thumbnails[fileName] = it }

    suspend fun full(fileName: String): ImageBitmap? = store.load(fileName, RecordPhotos.MAX_EDGE)?.asImageBitmap()
}

/**
 * One photo as a square tile, decrypted in memory. [onRemove] adds a remove button in the corner;
 * [onClick] makes the tile itself open the photo.
 */
@Composable
fun PhotoThumb(
    fileName: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    loader: PhotoLoaderViewModel = hiltViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val image by produceState<ImageBitmap?>(null, fileName) { value = loader.thumbnail(fileName) }
    Box(modifier) {
        Surface(
            Modifier.fillMaxSize().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Open photo", onClick = onClick) else Modifier),
            color = scheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            // Plain tile for the moment it takes to decrypt.
            image?.let { Image(it, contentDescription = description, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        if (onRemove != null) {
            Box(
                Modifier.align(Alignment.TopEnd)
                    .size(48.dp)
                    .clickable(role = Role.Button, onClickLabel = "Remove photo", onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove $description",
                    modifier = Modifier.clip(CircleShape).background(scheme.scrim.copy(alpha = 0.6f)).padding(4.dp).size(18.dp),
                    tint = scheme.surface,
                )
            }
        }
    }
}

/**
 * The "Take a photo / Choose photos" menu. Wrap it around whatever opens it: [anchor] gets a
 * function to call. Taking a photo asks for the camera permission first, because an app that
 * declares that permission (as this one does, for VIN scanning) may not hand off to the camera
 * app without it.
 */
@Composable
fun AttachMenu(onTake: () -> Unit, onChoose: () -> Unit, modifier: Modifier = Modifier, anchor: @Composable (open: () -> Unit) -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onTake() else refused = true
    }
    Box(modifier) {
        anchor { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Take a photo") },
                onClick = {
                    open = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        onTake()
                    } else {
                        askCamera.launch(Manifest.permission.CAMERA)
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("Choose photos") },
                onClick = {
                    open = false
                    onChoose()
                },
            )
        }
        DropdownMenu(expanded = refused, onDismissRequest = { refused = false }) {
            DropdownMenuItem(
                text = { Text("Camera access is off. Choose photos instead") },
                onClick = {
                    refused = false
                    onChoose()
                },
            )
        }
    }
}

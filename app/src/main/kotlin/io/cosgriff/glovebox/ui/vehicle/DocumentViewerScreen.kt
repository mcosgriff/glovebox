package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.theme.ViewerColors
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPasswordException
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.rememberPdfViewerState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.domain.DocumentFormat
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.ui.DocumentViewerRoute
import io.cosgriff.glovebox.ui.components.ZoomableImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** The document row, or none once it has been deleted. */
data class ViewedDocument(val row: DocumentEntity?)

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    documents: DocumentDao,
    private val store: DocumentStore,
) : ViewModel() {
    private val id = savedStateHandle.route<DocumentViewerRoute>().documentId

    /** Null while loading. */
    val document: StateFlow<ViewedDocument?> = documents.observeOne(id).map { ViewedDocument(it) }
        .screenState(viewModelScope, null)

    suspend fun pdf(row: DocumentEntity) = store.openPdf(row)

    suspend fun picture(row: DocumentEntity): ImageBitmap? = store.loadPicture(row)?.asImageBitmap()

    suspend fun copy(row: DocumentEntity): File? = store.openCopy(row)
}

// Always dark, as the photo viewer is: a page or a picture of one reads best against black.

/**
 * A document, viewed inside Glovebox (#224): a PDF drawn by Jetpack's PDF viewer, whose renderer runs in
 * an isolated process with no network or files of its own, or a picture. Nothing is written for it to
 * `shared/` and no other app receives it unless "Open in another app" or "Share" is chosen. It is a
 * screen in the nav graph, so it leaves composition when the app locks, which closes the document.
 */
@Composable
fun DocumentViewerScreen(onClose: () -> Unit, vm: DocumentViewerViewModel = hiltViewModel()) {
    val viewed = vm.document.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val row = viewed.row
    if (row == null) {
        // Deleted while it was open.
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf<Int?>(null) }
    val format = VehicleDocuments.format(row.format)

    fun handOut(share: Boolean) = scope.launch {
        failed = null
        val copy = vm.copy(row)
        failed = when {
            copy == null -> R.string.documents_unreadable
            !context.hand(copy, format, share) -> if (share) R.string.documents_no_app_share else R.string.documents_no_app_open
            else -> null
        }
    }

    Column(Modifier.fillMaxSize().background(ViewerColors.Backdrop)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close), tint = ViewerColors.OnBackdrop) }
            Text(
                row.name,
                Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ViewerColors.OnBackdrop,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_for, row.name), tint = ViewerColors.OnBackdrop) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.documents_open_elsewhere)) }, onClick = { menu = false; handOut(share = false) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, onClick = { menu = false; handOut(share = true) })
                }
            }
        }
        failed?.let {
            Text(
                stringResource(it),
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = ViewerColors.OnBackdrop,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (format) {
                DocumentFormat.PDF -> PdfPages(row, vm, onOpenElsewhere = { handOut(share = false) })
                else -> {
                    val image by produceState<ImageBitmap?>(null, row.fileName) { value = vm.picture(row) }
                    ZoomableImage(image, description = row.name, key = row.fileName)
                }
            }
        }
    }
}

private sealed interface PdfState {
    data object Loading : PdfState
    data class Open(val document: PdfDocument) : PdfState
    data class Failed(@StringRes val message: Int) : PdfState
}

// Jetpack's Compose PDF viewer is still marked experimental at 1.0.0-beta01.
@OptIn(ExperimentalPdfApi::class)
@Composable
private fun PdfPages(row: DocumentEntity, vm: DocumentViewerViewModel, onOpenElsewhere: () -> Unit) {
    val context = LocalContext.current
    val loader = remember { SandboxedPdfLoader(context.applicationContext) }
    val state by produceState<PdfState>(PdfState.Loading, row.id, row.fileName) {
        val descriptor = vm.pdf(row)
        if (descriptor == null) {
            value = PdfState.Failed(R.string.documents_unreadable)
            return@produceState
        }
        val document = try {
            // The address only names this document to the renderer; the pages come from the descriptor.
            loader.openDocument("glovebox://document/${row.id}".toUri(), descriptor)
        } catch (e: CancellationException) {
            descriptor.close()
            throw e
        } catch (_: PdfPasswordException) {
            value = PdfState.Failed(R.string.documents_pdf_password)
            return@produceState
        } catch (_: Exception) {
            // The loader has already closed the descriptor for a PDF it couldn't read.
            value = PdfState.Failed(R.string.documents_pdf_unreadable)
            return@produceState
        }
        value = PdfState.Open(document)
        // Leaving the screen, or the app locking, closes it (and its descriptor) in the renderer.
        awaitDispose { document.close() }
    }
    when (val s = state) {
        PdfState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ViewerColors.OnBackdrop) }
        is PdfState.Open -> PdfViewer(pdfDocument = s.document, state = rememberPdfViewerState(), modifier = Modifier.fillMaxSize())
        is PdfState.Failed -> Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(s.message), color = ViewerColors.OnBackdropMuted, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onOpenElsewhere, colors = ButtonDefaults.outlinedButtonColors(contentColor = ViewerColors.OnBackdrop)) { Text(stringResource(R.string.documents_open_elsewhere)) }
        }
    }
}

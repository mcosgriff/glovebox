package io.cosgriff.glovebox.ui.scan

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.ui.theme.ViewerColors
import java.util.concurrent.Executors

/** What reads the camera's frames, one at a time on the analysis thread, and is closed on that thread too. */
internal interface FrameJob {
    fun read(image: ImageProxy)

    fun close() {}
}

/**
 * Runs the [FrameJob] [start] makes on [analysis]'s frames while this is on screen. The job is closed on the
 * thread that uses it, after any frame still being read, since the native readers can't be closed from another.
 */
@Composable
internal fun FrameReader(analysis: ImageAnalysis, start: (Context) -> FrameJob) {
    val context = LocalContext.current.applicationContext
    DisposableEffect(analysis) {
        val executor = Executors.newSingleThreadExecutor()
        val job = start(context)
        analysis.setAnalyzer(executor) { image -> image.use(job::read) }
        onDispose {
            analysis.clearAnalyzer()
            executor.execute { job.close() }
            executor.shutdown()
        }
    }
}

/** Text over the camera picture, on a band of scrim so it reads against anything. [live] is announced as it changes. */
@Composable
internal fun ScrimText(text: String, style: TextStyle, color: Color = ViewerColors.OnBackdrop, live: Boolean = false) {
    Text(
        text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(ViewerColors.Backdrop.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .then(if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
    )
}

/** Where to hold what is to be read. Only a band across the middle of the frame is read; this box sits inside it. */
@Composable
internal fun GuideBox() {
    Box(Modifier.fillMaxWidth().height(150.dp).border(3.dp, Guide, RoundedCornerShape(18.dp)))
}

/**
 * What the camera read, at the foot of the screen while reading pauses: [heading] (what it is, and how sure),
 * [value], then [again] to read again and [use] to take it.
 */
@Composable
internal fun FoundCard(
    heading: @Composable () -> Unit,
    value: String,
    valueStyle: TextStyle,
    again: String,
    onAgain: () -> Unit,
    use: String,
    onUse: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            heading()
            Text(value, style = valueStyle)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAgain,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    // The default padding leaves too little of a third of the row for two words on one line.
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(again, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onUse,
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(use, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

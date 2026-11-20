package pt.isec.amov.cityfix.ui.components

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType

private const val MAX_ZOOM = 5f

/**
 * A ticket photo or video full screen (F-018), opened from a media tile on the ticket detail.
 * A full-screen Dialog on black with a close button; back also closes it ([onDismiss]).
 * Photo: the full file (not the thumbnail) with Coil, fitted to the screen, pinch to zoom 1x-5x.
 * Video: an Android VideoView inside AndroidView (beyond class: the class has no video playback),
 * with the system MediaController (play/pause and seek bar); it starts when ready and stops when
 * the dialog closes. [Attachment.file] is a local path before the upload and a Storage URL after
 * it (F-013); both work for Coil and for VideoView.setVideoPath.
 */
@Composable
fun MediaViewer(
    attachment: Attachment,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            when (attachment.type) {
                AttachmentType.IMAGE -> ZoomablePhoto(attachment.file)
                AttachmentType.VIDEO -> VideoPlayer(attachment.file)
            }
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.media_viewer_close),
                )
            }
        }
    }
}

/** The photo fitted to the screen; two fingers zoom (1x to [MAX_ZOOM]) and move it. */
@Composable
private fun ZoomablePhoto(file: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    AsyncImage(
        model = file,
        contentDescription = stringResource(R.string.ticket_media_photo),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                    if (scale == 1f) {
                        // Back at normal size: centre the photo again
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            ),
    )
}

/** A VideoView with the system controls; starts once prepared, stops when it leaves the screen. */
@Composable
private fun VideoPlayer(file: String) {
    val description = stringResource(R.string.ticket_media_video)
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                contentDescription = description
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setOnPreparedListener { start() }
                setVideoPath(file)
            }
        },
        // No size modifier: the VideoView keeps the video's aspect ratio and the Box centres it
        onRelease = { videoView -> videoView.stopPlayback() },
    )
}

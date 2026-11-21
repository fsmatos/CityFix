package pt.isec.amov.cityfix.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import androidx.core.net.toUri
import java.io.File
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType

/**
 * Full-screen viewer for one attachment (D-017): the photo at full size (Fit, not cropped), or the
 * video in the platform VideoView with a MediaController (play/pause and seek). The close button
 * and system back call [onDismiss]. A file that cannot be opened (no network, or another device's
 * local path while its upload is pending) shows a message instead.
 *
 * VideoView rather than Media3 ExoPlayer: no new dependency (D-017 plan). Black background in both
 * themes, as in a gallery app, so the media keep their own colours.
 */
@Composable
fun AttachmentViewer(
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
                AttachmentType.IMAGE -> PhotoContent(attachment.file)
                AttachmentType.VIDEO -> VideoContent(attachment.file)
            }
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close_viewer),
                )
            }
        }
    }
}

@Composable
private fun PhotoContent(file: String) {
    SubcomposeAsyncImage(
        model = file,
        contentDescription = stringResource(R.string.ticket_media_photo),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
        loading = { Loading() },
        error = { OpenError() },
    )
}

/**
 * The VideoView is created once per [file]; it starts when prepared and is stopped when the
 * viewer leaves the composition (onRelease), so the sound never outlives the dialog.
 */
@Composable
private fun VideoContent(file: String) {
    var prepared by remember(file) { mutableStateOf(false) }
    var failed by remember(file) { mutableStateOf(false) }

    if (failed) {
        OpenError()
        return
    }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setOnPreparedListener {
                    prepared = true
                    start()
                    controller.show()
                }
                setOnErrorListener { _, _, _ ->
                    failed = true
                    true // handled: no system "Can't play this video" dialog
                }
                setVideoURI(file.toVideoUri())
            }
        },
        onRelease = { it.stopPlayback() },
        modifier = Modifier.fillMaxSize(),
    )
    if (!prepared) Loading()
}

/** An https download URL as it is; a local absolute path (before upload) as a file:// URI. */
private fun String.toVideoUri(): Uri =
    if (startsWith("/")) File(this).toUri() else toUri()

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun OpenError() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.ticket_media_open_failed),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

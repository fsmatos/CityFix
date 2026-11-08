package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType

/**
 * Photo, or first video frame with a play badge, cropped to fill [modifier]'s bounds. Shared by
 * the report gallery, the ticket card and the detail (like `ContactAvatar` in Contacts_#3). A
 * blank file (sample tickets) or one that fails to decode shows the placeholder.
 */
@Composable
fun AttachmentImage(
    attachment: Attachment,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val isVideo = attachment.type == AttachmentType.VIDEO
    val context = LocalContext.current
    // Coil decodes images from the path alone; a video needs the frame decoder for this request.
    val model: Any = remember(attachment.file, isVideo) {
        if (isVideo) {
            ImageRequest.Builder(context)
                .data(attachment.file)
                .decoderFactory(VideoFrameDecoder.Factory())
                .build()
        } else {
            attachment.file
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (attachment.file.isBlank()) {
            PlaceholderIcon(contentDescription)
        } else {
            // Loading and error slots draw the same small tinted icon as a blank file; a painter
            // placeholder would be untinted and stretched by Crop.
            SubcomposeAsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { CenteredPlaceholder(contentDescription) },
                error = { CenteredPlaceholder(contentDescription) },
            )
        }
        if (isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

/** Coil's loading/error slots do not apply the description, so the icon carries it. */
@Composable
private fun CenteredPlaceholder(contentDescription: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PlaceholderIcon(contentDescription)
    }
}

@Composable
private fun PlaceholderIcon(contentDescription: String?) {
    Icon(
        painter = painterResource(R.drawable.ic_image_placeholder),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

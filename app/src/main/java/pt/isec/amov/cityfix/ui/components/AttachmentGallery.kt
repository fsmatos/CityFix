package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType

/**
 * Thumbnails of the report attachments, each with a remove button (P09 slide 27). The height cap
 * must stay: an unbounded lazy grid crashes inside the scrolling form. When the rows do not fit
 * under the cap (2 columns in narrow landscape), the grid scrolls on its own.
 */
@Composable
fun AttachmentGallery(
    attachments: List<Attachment>,
    onRemove: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    removeEnabled: Boolean = true,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
    ) {
        itemsIndexed(attachments, key = { _, attachment -> attachment.id }) { index, attachment ->
            AttachmentTile(
                attachment = attachment,
                position = index + 1,
                onRemove = { onRemove(attachment) },
                removeEnabled = removeEnabled,
            )
        }
    }
}

/** [position] counts from 1, so TalkBack can tell the tiles apart. */
@Composable
private fun AttachmentTile(
    attachment: Attachment,
    position: Int,
    onRemove: () -> Unit,
    removeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isVideo = attachment.type == AttachmentType.VIDEO
    val context = LocalContext.current
    val placeholder = painterResource(R.drawable.ic_image_placeholder)
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
    val description = stringResource(
        if (isVideo) R.string.cd_attachment_video else R.string.cd_attachment_photo,
        position,
    )
    val removeDescription = stringResource(
        if (isVideo) R.string.cd_remove_attachment_video else R.string.cd_remove_attachment_photo,
        position,
    )

    Box(modifier = modifier) {
        AsyncImage(
            model = model,
            contentDescription = description,
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        if (isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
        IconButton(
            onClick = onRemove,
            enabled = removeEnabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp),
        ) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = removeDescription)
        }
    }
}

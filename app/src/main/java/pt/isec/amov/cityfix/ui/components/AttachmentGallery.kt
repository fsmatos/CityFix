package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType

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

@Composable
private fun AttachmentTile(
    attachment: Attachment,
    position: Int,
    onRemove: () -> Unit,
    removeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isVideo = attachment.type == AttachmentType.VIDEO
    val description = stringResource(
        if (isVideo) R.string.cd_attachment_video else R.string.cd_attachment_photo,
        position,
    )
    val removeDescription = stringResource(
        if (isVideo) R.string.cd_remove_attachment_video else R.string.cd_remove_attachment_photo,
        position,
    )

    Box(modifier = modifier) {
        AttachmentImage(
            attachment = attachment,
            contentDescription = description,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
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

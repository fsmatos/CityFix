package pt.isec.amov.cityfix.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.SampleTickets
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.components.CoarseLocationMap
import pt.isec.amov.cityfix.ui.components.StatusBadge
import pt.isec.amov.cityfix.ui.components.situationLabelRes
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.formatDateTime
import java.util.Locale

private const val DAY_MILLIS = 86_400_000L

/**
 * Who is looking at the detail; each value is a different view in the brief.
 * PUBLIC: anonymous users (I.1.C), and any registered citizen viewing a public ticket they did not
 * create or follow (I.2.D, "acesso às funcionalidades do utilizador anónimo").
 * REGISTERED: the registered citizen who created or follows this ticket (I.2.E, "ocorrências (do
 * utilizador)").
 * OPERATOR: any municipal operator (I.3.E).
 */
enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }

/**
 * Ticket detail, or "Ticket not found" when [ticket] is null.
 * Every audience gets the header (category, situation, status badge), the photo/video tiles and the
 * approximate location. The header follows the Gate 1 Q2 decision as revised at review: brief I.1.B's
 * list fields plus the status badge already shown on the public list card.
 * REGISTERED and OPERATOR also get "Reported on", the description and the status history. Never reads author ids, follower ids, messages, responsible ids, justifications or
 * uploader ids; the anonymous projection happens upstream (decision 0006).
 * Portrait: one scrolling column. Landscape: two panes side by side, each scrolling on its own.
 */
@Composable
fun ShowScreen(
    ticket: Ticket?,
    audience: DetailAudience,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (ticket == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.ticket_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // Hoisted so both orientations share the same call sites and the position survives rotation
    val detailsScroll = rememberScrollState()
    val locationScroll = rememberScrollState()

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DetailsPane(
                ticket = ticket,
                audience = audience,
                locale = locale,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(detailsScroll),
            )
            LocationPane(
                ticket = ticket,
                audience = audience,
                locale = locale,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(locationScroll),
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(detailsScroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailsPane(ticket, audience, locale)
            LocationPane(ticket, audience, locale)
        }
    }
}

/** Header, then "Reported on" and description (not for PUBLIC), then the photo/video tiles. */
@Composable
private fun DetailsPane(
    ticket: Ticket,
    audience: DetailAudience,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Gate 1 Q2 (revised at review): I.1.B's list fields plus the status badge, for every audience
        TicketHeader(ticket)
        when (audience) {
            DetailAudience.PUBLIC -> Unit
            DetailAudience.REGISTERED, DetailAudience.OPERATOR -> DescriptionSection(ticket, locale)
        }
        MediaSection(ticket.attachments)
    }
}

/** The approximate location, then the status history (not for PUBLIC). */
@Composable
private fun LocationPane(
    ticket: Ticket,
    audience: DetailAudience,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // The only location read: the map shows the approximate area, never coordinates
        CoarseLocationMap(ticket.location)
        when (audience) {
            DetailAudience.PUBLIC -> Unit
            DetailAudience.REGISTERED, DetailAudience.OPERATOR -> {
                HorizontalDivider()
                StatusHistorySection(ticket.statusHistory, locale)
            }
        }
    }
}

/** Category (the page heading), situation and status badge. */
@Composable
private fun TicketHeader(
    ticket: Ticket,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = ticket.categoryName.ifBlank { stringResource(R.string.ticket_no_category) },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(situationLabelRes(ticket.situation)),
            style = MaterialTheme.typography.bodyMedium,
        )
        StatusBadge(ticket.currentStatus)
    }
}

/** "Reported on" and the description; not for anonymous users (brief I.1.C). */
@Composable
private fun DescriptionSection(
    ticket: Ticket,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 0L is the "not set" default; skip it instead of showing 1 Jan 1970
        if (ticket.createdAt > 0L) {
            Text(
                text = stringResource(R.string.ticket_reported_on, formatDateTime(ticket.createdAt, locale)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (ticket.description.isNotBlank()) {
            Text(
                text = stringResource(R.string.ticket_detail_description_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** Status changes in stored order, oldest first; not for anonymous users (brief I.1.C). */
@Composable
private fun StatusHistorySection(
    history: List<StatusHistory>,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ticket_detail_history_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.ticket_detail_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // No sort: currentStatus copies the last entry (decision 0004), so the stored order is shown
            history.forEach { entry -> HistoryRow(entry, locale) }
        }
    }
}

/** One status change: badge and date/time, read by TalkBack as one item. */
@Composable
private fun HistoryRow(
    entry: StatusHistory,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusBadge(entry.status)
        if (entry.dateTime > 0L) {
            Text(
                text = formatDateTime(entry.dateTime, locale),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Photo and video tiles in a horizontally scrolling row, or "No photos or videos.". */
@Composable
private fun MediaSection(
    attachments: List<Attachment>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ticket_media_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (attachments.isEmpty()) {
            Text(
                text = stringResource(R.string.ticket_media_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                attachments.forEach { attachment -> MediaTile(attachment) }
            }
        }
    }
}

/**
 * Type icon and label for one attachment. Phase 1 has no real files, so it says "Not available yet"
 * instead of drawing a fake photo; the media task replaces the icon with the thumbnail.
 */
@Composable
private fun MediaTile(
    attachment: Attachment,
    modifier: Modifier = Modifier,
) {
    val labelRes = when (attachment.type) {
        AttachmentType.IMAGE -> R.string.ticket_media_photo
        AttachmentType.VIDEO -> R.string.ticket_media_video
    }

    Column(
        modifier = modifier
            .width(120.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative: the label below says what it is
            when (attachment.type) {
                AttachmentType.IMAGE -> Icon(
                    painter = painterResource(R.drawable.ic_image_placeholder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AttachmentType.VIDEO -> Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.ticket_media_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 2026-10-15 18:30 UTC. */
private const val PREVIEW_CREATED = 1_792_089_000_000L

/** Already in anonymous form (no author ids, rounded location), like TicketsViewModel.tickets. */
private val previewTicket = Ticket(
    id = "preview-1",
    description = "Three street lamps have been off for a week on the main avenue. " +
        "The pedestrian crossing is completely dark at night.",
    situation = Situation.BREAKDOWN,
    scope = Scope.PUBLIC,
    location = Coordinates(40.21, -8.42),
    currentStatus = TicketStatus.IN_PROGRESS,
    createdAt = PREVIEW_CREATED,
    categoryName = "Public lighting",
    attachments = listOf(
        Attachment(id = "preview-a1", type = AttachmentType.IMAGE),
        Attachment(id = "preview-a2", type = AttachmentType.VIDEO),
    ),
    statusHistory = listOf(
        StatusHistory(status = TicketStatus.OPEN, dateTime = PREVIEW_CREATED),
        StatusHistory(status = TicketStatus.INFO_REQUESTED, dateTime = PREVIEW_CREATED + DAY_MILLIS),
        StatusHistory(status = TicketStatus.IN_PROGRESS, dateTime = PREVIEW_CREATED + 2 * DAY_MILLIS),
    ),
)

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ShowScreenPublicPreview() {
    CityFixTheme {
        ShowScreen(previewTicket, audience = DetailAudience.PUBLIC)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ShowScreenFullPreview() {
    CityFixTheme {
        ShowScreen(previewTicket, audience = DetailAudience.REGISTERED)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ShowScreenNotFoundPreview() {
    CityFixTheme {
        ShowScreen(ticket = null, audience = DetailAudience.PUBLIC)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ShowScreenEmptyHistoryPreview() {
    CityFixTheme {
        ShowScreen(Ticket(id = "empty", categoryName = "Pavement"), audience = DetailAudience.REGISTERED)
    }
}

/**
 * Privacy check: the raw, unprojected sample-2 (author id, a follower, an operator id in the history,
 * exact location and a description) in PUBLIC must show none of them.
 */
@Preview(showBackground = true)
@Composable
private fun ShowScreenPrivacyPreview() {
    CityFixTheme {
        ShowScreen(SampleTickets.all.first { it.id == "sample-2" }, audience = DetailAudience.PUBLIC)
    }
}

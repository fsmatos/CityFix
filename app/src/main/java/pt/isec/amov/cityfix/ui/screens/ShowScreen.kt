package pt.isec.amov.cityfix.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.SampleTickets
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentPhase
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.SYSTEM_RESPONSIBLE_ID
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.effectiveStatus
import pt.isec.amov.cityfix.model.isParticipant
import pt.isec.amov.cityfix.ui.components.AttachmentImage
import pt.isec.amov.cityfix.ui.components.AttachmentViewer
import pt.isec.amov.cityfix.ui.components.CoarseLocationMap
import pt.isec.amov.cityfix.ui.components.EmptyState
import pt.isec.amov.cityfix.ui.components.SectionCard
import pt.isec.amov.cityfix.ui.components.StatusBadge
import pt.isec.amov.cityfix.ui.components.TicketMap
import pt.isec.amov.cityfix.ui.components.situationLabelRes
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens
import pt.isec.amov.cityfix.utils.formatCoordinate
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
 * The audience for [user] on [ticket] (F-014). [user] is the session's tickets user (D-009): null
 * for anonymous, pending and loading sessions, so they always get PUBLIC (brief I.1.C). A citizen
 * gets REGISTERED only on a ticket they created or follow. A projected public ticket has a blank
 * creator and no followers, so a citizen gets PUBLIC on it.
 */
fun detailAudience(user: User?, ticket: Ticket?): DetailAudience = when (user) {
    null -> DetailAudience.PUBLIC
    is Citizen ->
        if (ticket != null && ticket.isParticipant(user.id)) DetailAudience.REGISTERED else DetailAudience.PUBLIC
    is Operator -> DetailAudience.OPERATOR
}

/**
 * Ticket detail, or "Ticket not found" when [ticket] is null.
 * Every audience gets the header (category, situation, status badge for [now], decision 0011) and
 * the photo/video tiles. The header follows the Gate 1 Q2 decision as revised at review: brief
 * I.1.B's list fields plus the status badge already shown on the public list card.
 * PUBLIC gets the approximate location only (brief I.1.C).
 * REGISTERED and OPERATOR (F-014) also get "Reported on", the description, the exact map and the
 * full status history with justifications and "Closed automatically", then [participantContent]
 * (the message thread and actions, see TicketDetailScreen). OPERATOR also gets the coordinates as
 * text under the exact map (brief I.3.E, D-019). Never shows author, follower, responsible or
 * uploader ids; the anonymous projection happens upstream (decision 0006).
 * [participantContent] is a composable parameter (a "slot", beyond class): the caller decides what
 * goes under the history, so this screen stays free of ViewModels.
 * [responsibleNames] (D-013, D-019): id -> name of the people on the ticket, given only by the
 * operator's detail; a history entry whose responsible is in it shows "By <name>", and a photo/video
 * tile whose uploader is in it shows "Added by <name>". Empty (every other caller): no names shown.
 * Portrait: one scrolling column. Landscape: two panes side by side, each scrolling on its own.
 */
@Composable
fun ShowScreen(
    ticket: Ticket?,
    audience: DetailAudience,
    now: Long,
    modifier: Modifier = Modifier,
    responsibleNames: Map<String, String> = emptyMap(),
    participantContent: @Composable ColumnScope.() -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // F-021: the open attachment lives here, not in MediaSection. Portrait and landscape put the
    // tiles at different places in the layout, and saveable state is tied to that place, so a
    // rotation used to close the viewer.
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }

    if (ticket == null) {
        EmptyState(messageRes = R.string.ticket_not_found, modifier = modifier)
        return
    }

    // Hoisted so both orientations share the same call sites and the position survives rotation
    val detailsScroll = rememberScrollState()
    val locationScroll = rememberScrollState()
    // F-018: while a finger is on the exact map the column holding it stops scrolling, so a pinch
    // or a vertical pan goes to the map (TicketMap's onTouchChange)
    var mapTouched by remember { mutableStateOf(false) }
    val onMapTouchChange: (Boolean) -> Unit = { mapTouched = it }

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
                now = now,
                locale = locale,
                onOpenAttachment = { openedId = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(detailsScroll)
                    .padding(bottom = LocalBottomBarPadding.current),
                responsibleNames = responsibleNames,
            )
            LocationPane(
                ticket = ticket,
                audience = audience,
                locale = locale,
                responsibleNames = responsibleNames,
                participantContent = participantContent,
                onMapTouchChange = onMapTouchChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(locationScroll, enabled = !mapTouched)
                    .padding(bottom = LocalBottomBarPadding.current),
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(detailsScroll, enabled = !mapTouched)
                .padding(16.dp)
                // Room to scroll above the floating island (D-016).
                .padding(bottom = LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            DetailsPane(
                ticket,
                audience,
                now,
                locale,
                onOpenAttachment = { openedId = it },
                responsibleNames = responsibleNames,
            )
            LocationPane(
                ticket,
                audience,
                locale,
                responsibleNames = responsibleNames,
                participantContent = participantContent,
                onMapTouchChange = onMapTouchChange,
            )
        }
    }

    // D-017: the opened attachment by id; it closes by itself when that attachment leaves the
    // live ticket.
    val opened = ticket.attachments.firstOrNull { it.id == openedId && it.file.isNotBlank() }
    if (opened != null) {
        AttachmentViewer(attachment = opened, onDismiss = { openedId = null })
    }
}

/** Header, then "Reported on" and description (not for PUBLIC), then the photo/video tiles. */
@Composable
private fun DetailsPane(
    ticket: Ticket,
    audience: DetailAudience,
    now: Long,
    locale: Locale,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
    responsibleNames: Map<String, String> = emptyMap(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
    ) {
        // Gate 1 Q2 (revised at review): I.1.B's list fields plus the status badge, for every audience
        TicketHeader(ticket, now)
        when (audience) {
            DetailAudience.PUBLIC -> Unit
            DetailAudience.REGISTERED, DetailAudience.OPERATOR -> SectionCard {
                DescriptionSection(ticket, locale)
            }
        }
        // D-016: each section is a card.
        SectionCard { MediaSection(ticket.attachments, onOpenAttachment, uploaderNames = responsibleNames) }
    }
}

/**
 * PUBLIC: the approximate location. REGISTERED and OPERATOR: the exact location, the status
 * history and [participantContent] (in landscape the thread scrolls under the history).
 */
@Composable
private fun LocationPane(
    ticket: Ticket,
    audience: DetailAudience,
    locale: Locale,
    modifier: Modifier = Modifier,
    responsibleNames: Map<String, String> = emptyMap(),
    participantContent: @Composable ColumnScope.() -> Unit = {},
    onMapTouchChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
    ) {
        when (audience) {
            // The map shows the approximate area, never coordinates (brief I.1.C)
            DetailAudience.PUBLIC -> SectionCard { CoarseLocationMap(ticket.location) }
            DetailAudience.REGISTERED, DetailAudience.OPERATOR -> {
                // Brief I.1.C hides the exact location only from anonymous users (F-014 Q8)
                SectionCard {
                    ExactLocationSection(
                        ticket.location,
                        showCoordinates = audience == DetailAudience.OPERATOR,
                        onMapTouchChange = onMapTouchChange,
                    )
                }
                SectionCard {
                    StatusHistorySection(ticket.statusHistory, locale, responsibleNames = responsibleNames)
                }
                participantContent()
            }
        }
    }
}

/**
 * "Location": the exact map with a marker (participants and operators). Coordinate text only when
 * [showCoordinates], i.e. for operators (brief I.3.E, D-019).
 */
@Composable
private fun ExactLocationSection(
    location: Coordinates,
    showCoordinates: Boolean,
    modifier: Modifier = Modifier,
    onMapTouchChange: (Boolean) -> Unit = {},
) {
    val mapDescription = stringResource(R.string.cd_ticket_map_exact)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ticket_map_exact_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (location == Coordinates()) {
            Text(
                text = stringResource(R.string.ticket_map_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TicketMap(
                location = location,
                exact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .semantics { contentDescription = mapDescription },
                onTouchChange = onMapTouchChange,
            )
            if (showCoordinates) {
                val lat = formatCoordinate(location.latitude)
                val lng = formatCoordinate(location.longitude)
                val coordinatesDescription = stringResource(R.string.cd_ticket_coordinates, lat, lng)
                Text(
                    text = stringResource(R.string.ticket_coordinates, lat, lng),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { contentDescription = coordinatesDescription },
                )
            }
        }
    }
}

/** Category (the page heading), situation and status badge (an expired RESOLVED shows CLOSED). */
@Composable
private fun TicketHeader(
    ticket: Ticket,
    now: Long,
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBadge(ticket.effectiveStatus(now))
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
    responsibleNames: Map<String, String> = emptyMap(),
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
            history.forEach { entry -> HistoryRow(entry, locale, responsibleNames = responsibleNames) }
        }
    }
}

/**
 * One status change, read by TalkBack as one item: badge and date/time, "Closed automatically" for
 * the entry written by the app (decision 0011), "By <name>" when [responsibleNames] has the
 * responsible (operator view only, D-013), and the justification of a reopen or info request.
 * The responsible id itself is never shown.
 */
@Composable
private fun HistoryRow(
    entry: StatusHistory,
    locale: Locale,
    modifier: Modifier = Modifier,
    responsibleNames: Map<String, String> = emptyMap(),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
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
        if (entry.responsibleId == SYSTEM_RESPONSIBLE_ID) {
            Text(
                text = stringResource(R.string.ticket_history_system),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        responsibleNames[entry.responsibleId]?.let { name ->
            Text(
                text = stringResource(R.string.ticket_history_by, name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (entry.justification != null) {
            Text(
                text = entry.justification,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Photo and video tiles in a horizontally scrolling row, or "No photos or videos.". Tapping a tile
 * with a file calls [onOpen] with its id; ShowScreen then opens it full screen ([AttachmentViewer],
 * D-017; it replaced F-018's MediaViewer at the merge) and keeps it open after a rotation (F-021).
 */
@Composable
private fun MediaSection(
    attachments: List<Attachment>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    uploaderNames: Map<String, String> = emptyMap(),
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
                attachments.forEachIndexed { index, attachment ->
                    MediaTile(
                        attachment = attachment,
                        position = index + 1,
                        onClick = { onOpen(attachment.id) },
                        uploaderName = uploaderNames[attachment.uploadedById],
                    )
                }
            }
        }
    }
}

/**
 * Photo or video frame and its label for one attachment. Without a file (sample tickets) it shows
 * the placeholder and "Not available yet" instead of drawing a fake photo. With a file, a line says
 * when its upload is still pending or has failed (F-013); uploaded media show no line. The
 * operator's proof of the finished work is labelled "Resolution proof" for every audience (D-013).
 * A tile with a file is clickable ([onClick] opens it full screen, F-018); TalkBack reads
 * "Open photo" or "Play video" as the action.
 * "Added by <name>" when [uploaderName] is given (operator view only, D-019); never the id.
 */
@Composable
private fun MediaTile(
    attachment: Attachment,
    position: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    uploaderName: String? = null,
) {
    val labelRes = when (attachment.type) {
        AttachmentType.IMAGE -> R.string.ticket_media_photo
        AttachmentType.VIDEO -> R.string.ticket_media_video
    }
    val uploadRes = when (attachment.uploadState) {
        UploadState.PENDING, UploadState.UPLOADING -> R.string.ticket_media_upload_pending
        UploadState.FAILED -> R.string.ticket_media_upload_failed
        UploadState.UPLOADED -> null
    }
    // D-017: a tile with a file opens the viewer; sample tiles (no file) stay plain.
    val canOpen = attachment.file.isNotBlank()
    val openLabel = stringResource(
        if (attachment.type == AttachmentType.VIDEO) R.string.cd_open_video else R.string.cd_open_photo,
        position,
    )
    val clickModifier = if (canOpen) {
        Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .width(120.dp)
            .clip(MaterialTheme.shapes.small)
            .then(clickModifier)
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
            AttachmentImage(
                attachment = attachment,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (attachment.phase == AttachmentPhase.RESOLUTION) {
            Text(
                text = stringResource(R.string.ticket_media_proof),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (uploaderName != null) {
            Text(
                text = stringResource(R.string.ticket_media_added_by, uploaderName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (attachment.file.isBlank()) {
            Text(
                text = stringResource(R.string.ticket_media_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else if (uploadRes != null) {
            Text(
                text = stringResource(uploadRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 2026-10-15 18:30 UTC. */
private const val PREVIEW_CREATED = 1_792_089_000_000L

/** "Now" for the previews: three days after [PREVIEW_CREATED]. */
private const val PREVIEW_NOW = PREVIEW_CREATED + 3 * DAY_MILLIS

/** Raw ticket with no author ids and a rounded location; the PUBLIC preview hides description and history. */
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
        ShowScreen(previewTicket, audience = DetailAudience.PUBLIC, now = PREVIEW_NOW)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ShowScreenFullPreview() {
    CityFixTheme {
        // One media whose upload failed, to show the "Upload failed" line, and one proof (D-013).
        val failed = Attachment(id = "preview-a3", file = "preview-a3.jpg", uploadState = UploadState.FAILED)
        val proof = Attachment(id = "preview-a4", phase = AttachmentPhase.RESOLUTION)
        // A reopen with its justification and an automatic close, to show the history labels.
        val history = previewTicket.statusHistory + listOf(
            StatusHistory(TicketStatus.RESOLVED, PREVIEW_CREATED + 3 * DAY_MILLIS, null, "op-1"),
            StatusHistory(TicketStatus.OPEN, PREVIEW_CREATED + 4 * DAY_MILLIS, "The lamps are still off.", "c-1"),
            StatusHistory(TicketStatus.RESOLVED, PREVIEW_CREATED + 5 * DAY_MILLIS, null, "op-1"),
            StatusHistory(TicketStatus.CLOSED, PREVIEW_CREATED + 10 * DAY_MILLIS, null, SYSTEM_RESPONSIBLE_ID),
        )
        val ticket = previewTicket.copy(
            attachments = previewTicket.attachments + failed + proof,
            currentStatus = TicketStatus.CLOSED,
            statusHistory = history,
        )
        ShowScreen(ticket, audience = DetailAudience.REGISTERED, now = PREVIEW_CREATED + 11 * DAY_MILLIS)
    }
}

/**
 * D-013: the operator view with names in the history ("By <name>"). D-019: coordinates under the
 * map and "Added by <name>" on the tiles, including a resolution proof by the operator.
 */
@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ShowScreenOperatorPreview() {
    CityFixTheme {
        val history = listOf(
            StatusHistory(TicketStatus.OPEN, PREVIEW_CREATED, null, "c-1"),
            StatusHistory(TicketStatus.INFO_REQUESTED, PREVIEW_CREATED + DAY_MILLIS, "Which lamp posts?", "op-1"),
            StatusHistory(TicketStatus.IN_PROGRESS, PREVIEW_CREATED + 2 * DAY_MILLIS, null, "op-1"),
        )
        val attachments = listOf(
            Attachment(id = "preview-a1", type = AttachmentType.IMAGE, uploadedById = "c-1"),
            Attachment(id = "preview-a2", type = AttachmentType.VIDEO, uploadedById = "c-1"),
            Attachment(id = "preview-op-proof", phase = AttachmentPhase.RESOLUTION, uploadedById = "op-1"),
        )
        ShowScreen(
            previewTicket.copy(statusHistory = history, attachments = attachments),
            audience = DetailAudience.OPERATOR,
            now = PREVIEW_NOW,
            responsibleNames = mapOf("c-1" to "Citizen One", "op-1" to "Operator One"),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ShowScreenNotFoundPreview() {
    CityFixTheme {
        ShowScreen(ticket = null, audience = DetailAudience.PUBLIC, now = PREVIEW_NOW)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ShowScreenEmptyHistoryPreview() {
    CityFixTheme {
        ShowScreen(
            Ticket(id = "empty", categoryName = "Pavement"),
            audience = DetailAudience.REGISTERED,
            now = PREVIEW_NOW,
        )
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
        ShowScreen(
            SampleTickets.all.first { it.id == "sample-2" },
            audience = DetailAudience.PUBLIC,
            now = PREVIEW_NOW,
        )
    }
}

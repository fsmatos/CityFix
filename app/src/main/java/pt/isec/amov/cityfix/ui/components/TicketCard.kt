package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.formatDate

/** Returns the string resource id with the localised label of [situation]. */
fun situationLabelRes(situation: Situation): Int = when (situation) {
    Situation.DAMAGE -> R.string.situation_damage
    Situation.BREAKDOWN -> R.string.situation_breakdown
    Situation.DESTRUCTION -> R.string.situation_destruction
    Situation.THEFT -> R.string.situation_theft
}

/**
 * Clickable ticket summary (brief I.1.B); [isExpanded] also shows the description and date.
 * [status] is the badge; My tickets passes `ticket.effectiveStatus(now)` so an expired RESOLVED
 * ticket shows CLOSED (decision 0011).
 */
@Composable
fun TicketCard(
    ticket: Ticket,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: TicketStatus = ticket.currentStatus,
) {
    val noCategory = stringResource(R.string.ticket_no_category)

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TicketThumbnail(ticket.attachments.firstOrNull())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = ticket.categoryName.ifBlank { noCategory },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(situationLabelRes(ticket.situation)),
                    style = MaterialTheme.typography.bodySmall,
                )
                StatusBadge(status)
                if (isExpanded) {
                    if (ticket.description.isNotBlank()) {
                        Text(
                            text = ticket.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // 0L is the "not set" default; skip it instead of showing 1 Jan 1970
                    if (ticket.createdAt > 0L) {
                        Text(
                            text = stringResource(R.string.ticket_reported_on, formatDate(ticket.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** First attachment of the ticket, or the placeholder icon when it has none. */
@Composable
private fun TicketThumbnail(attachment: Attachment?) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        val contentDescription = stringResource(R.string.ticket_thumbnail_description)
        if (attachment == null) {
            Icon(
                painter = painterResource(R.drawable.ic_image_placeholder),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AttachmentImage(
                attachment = attachment,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val previewTickets = listOf(
    Ticket(
        id = "t1",
        description = "Large pothole in front of the bus stop.",
        categoryName = "Pavement",
        situation = Situation.DAMAGE,
        currentStatus = TicketStatus.OPEN,
        createdAt = 1_790_000_000_000L,
    ),
    Ticket(
        id = "t2",
        description = "Three street lamps have been off for a week. The avenue is completely dark at night, " +
            "which makes crossing at the pedestrian crossing dangerous, especially for older people " +
            "walking home from the market after closing time.",
        categoryName = "Public lighting on the long avenue next to the municipal market",
        situation = Situation.BREAKDOWN,
        currentStatus = TicketStatus.INFO_REQUESTED,
        createdAt = 1_790_100_000_000L,
    ),
    Ticket(
        id = "t3",
        description = "Bench broken in half near the playground.",
        categoryName = "Benches",
        situation = Situation.DESTRUCTION,
        currentStatus = TicketStatus.IN_PROGRESS,
        createdAt = 1_790_200_000_000L,
    ),
    Ticket(
        id = "t4",
        description = "Stop sign missing at the roundabout exit.",
        categoryName = "Traffic signs",
        situation = Situation.THEFT,
        currentStatus = TicketStatus.RESOLVED,
        createdAt = 1_790_300_000_000L,
    ),
    Ticket(
        id = "t5",
        categoryName = "",
        situation = Situation.DAMAGE,
        currentStatus = TicketStatus.CLOSED,
        // No date set: the expanded card skips the date line
        createdAt = 0L,
    ),
)

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun TicketCardListPreview() {
    CityFixTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            previewTickets.forEach { ticket ->
                TicketCard(ticket = ticket, isExpanded = false, onClick = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun TicketCardExpandedPreview() {
    CityFixTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            previewTickets.forEach { ticket ->
                TicketCard(ticket = ticket, isExpanded = true, onClick = {})
            }
        }
    }
}

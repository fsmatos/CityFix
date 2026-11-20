package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.effectiveStatus
import pt.isec.amov.cityfix.ui.components.TicketCard
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * My tickets (brief I.2.E): the tickets the citizen created or follows, any scope or status, in
 * the order given (newest first upstream). The badge is the effective status at [now], so a
 * RESOLVED ticket whose reopen window ended shows CLOSED (decision 0011). Cards stay collapsed
 * (F-014 Q13). Stateless, like ListScreen.
 */
@Composable
fun MyTicketsScreen(
    tickets: List<Ticket>,
    now: Long,
    onTicketClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tickets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.my_tickets_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            // Bottom padding so the last card clears the citizen report FAB.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tickets, key = { it.id }) { ticket ->
                TicketCard(
                    ticket = ticket,
                    isExpanded = false,
                    onClick = { onTicketClick(ticket.id) },
                    status = ticket.effectiveStatus(now),
                )
            }
        }
    }
}

private const val DAY = 86_400_000L

/** 2026-10-15 18:30 UTC. */
private const val PREVIEW_T0 = 1_792_089_000_000L

private fun previewResolved(id: String, category: String) = Ticket(
    id = id,
    situation = Situation.DAMAGE,
    scope = Scope.PRIVATE,
    currentStatus = TicketStatus.RESOLVED,
    createdAt = PREVIEW_T0 - DAY,
    categoryName = category,
    statusHistory = listOf(
        StatusHistory(TicketStatus.OPEN, PREVIEW_T0 - DAY),
        StatusHistory(TicketStatus.RESOLVED, PREVIEW_T0),
    ),
)

/** Open, resolved 4 days ago (still RESOLVED) and resolved 6 days ago (shown CLOSED) at "now". */
private val previewTickets = listOf(
    Ticket(
        id = "p1",
        situation = Situation.BREAKDOWN,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.OPEN,
        createdAt = PREVIEW_T0 + 5 * DAY,
        categoryName = "Public lighting",
    ),
    previewResolved("p2", "Urban furniture").copy(
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, PREVIEW_T0 + DAY),
            StatusHistory(TicketStatus.RESOLVED, PREVIEW_T0 + 2 * DAY),
        ),
    ),
    previewResolved("p3", "Pavement"),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun MyTicketsScreenPreview() {
    CityFixTheme {
        MyTicketsScreen(previewTickets, now = PREVIEW_T0 + 6 * DAY, onTicketClick = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun MyTicketsScreenEmptyPreview() {
    CityFixTheme {
        MyTicketsScreen(emptyList(), now = PREVIEW_T0, onTicketClick = {})
    }
}

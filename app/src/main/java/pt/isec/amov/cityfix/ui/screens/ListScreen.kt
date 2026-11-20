package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.components.EmptyState
import pt.isec.amov.cityfix.ui.components.TicketCard
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens

/**
 * Public active tickets (brief I.1.B), shown in the order given; filtering, sorting and the
 * anonymous projection happen upstream (TicketsViewModel.tickets). Expanded cards also show the description.
 */
@Composable
fun ListScreen(
    tickets: List<Ticket>,
    isExpanded: Boolean,
    onTicketClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tickets.isEmpty()) {
        EmptyState(messageRes = R.string.ticket_list_empty, modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            // Bottom padding so the last card clears the floating island (D-016), or 88dp.
            contentPadding = PaddingValues(
                start = Dimens.screenPadding,
                top = Dimens.sm,
                end = Dimens.screenPadding,
                bottom = maxOf(Dimens.fabClearance, LocalBottomBarPadding.current),
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(tickets, key = { it.id }) { ticket ->
                TicketCard(
                    ticket = ticket,
                    isExpanded = isExpanded,
                    onClick = { onTicketClick(ticket.id) },
                    // Centred and at most contentMaxWidth wide, so cards do not stretch in landscape.
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .animateItem(),
                )
            }
        }
    }
}

private val previewTickets = listOf(
    Ticket(
        id = "p1",
        description = "Large pothole in front of the bus stop.",
        situation = Situation.DAMAGE,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.OPEN,
        createdAt = 1_792_000_000_000L,
        categoryName = "Pavement",
    ),
    Ticket(
        id = "p2",
        description = "Three street lamps off on the main avenue.",
        situation = Situation.BREAKDOWN,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.INFO_REQUESTED,
        createdAt = 1_791_900_000_000L,
        categoryName = "Public lighting",
    ),
    Ticket(
        id = "p3",
        description = "Bench broken in half near the playground.",
        situation = Situation.DESTRUCTION,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.IN_PROGRESS,
        createdAt = 1_791_800_000_000L,
        categoryName = "Benches",
    ),
    Ticket(
        id = "p4",
        description = "Stop sign missing at the roundabout exit.",
        situation = Situation.THEFT,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.OPEN,
        createdAt = 1_791_700_000_000L,
        categoryName = "Traffic signs",
    ),
    Ticket(
        id = "p5",
        description = "Overflowing bin next to the school gate.",
        situation = Situation.DAMAGE,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.IN_PROGRESS,
        createdAt = 1_791_600_000_000L,
        categoryName = "Waste",
    ),
    Ticket(
        id = "p6",
        description = "Traffic light stuck on red at the crossing.",
        situation = Situation.BREAKDOWN,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.INFO_REQUESTED,
        createdAt = 1_791_500_000_000L,
        categoryName = "Traffic lights",
    ),
)

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ListScreenPreview() {
    CityFixTheme {
        ListScreen(previewTickets, isExpanded = false, onTicketClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ListScreenExpandedPreview() {
    CityFixTheme {
        ListScreen(previewTickets, isExpanded = true, onTicketClick = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ListScreenEmptyPreview() {
    CityFixTheme {
        ListScreen(emptyList(), isExpanded = false, onTicketClick = {})
    }
}

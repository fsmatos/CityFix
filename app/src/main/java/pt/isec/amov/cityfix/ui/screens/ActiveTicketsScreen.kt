package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.components.TicketCard
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/** How many tickets the Active issues tab shows before "See more". */
const val ACTIVE_TICKETS_SHOWN = 5

/**
 * Active issues tab (D-016): the first [ACTIVE_TICKETS_SHOWN] of [tickets] (already the active
 * ones, in the order given) and, when there are more, "See more", which opens the full list.
 * Stateless, like ListScreen.
 */
@Composable
fun ActiveTicketsScreen(
    tickets: List<Ticket>,
    onTicketClick: (String) -> Unit,
    onSeeMoreClick: () -> Unit,
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
                text = stringResource(R.string.ticket_list_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // Centred and at most 600dp wide, so the cards do not stretch in landscape.
    val itemModifier = Modifier
        .widthIn(max = 600.dp)
        .fillMaxWidth()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(tickets.take(ACTIVE_TICKETS_SHOWN), key = { it.id }) { ticket ->
            TicketCard(
                ticket = ticket,
                isExpanded = false,
                onClick = { onTicketClick(ticket.id) },
                modifier = itemModifier,
            )
        }
        if (tickets.size > ACTIVE_TICKETS_SHOWN) {
            item {
                OutlinedButton(onClick = onSeeMoreClick, modifier = itemModifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.active_see_more))
                }
            }
        }
    }
}

private val previewTickets = List(7) { index ->
    Ticket(
        id = "a$index",
        situation = Situation.entries[index % Situation.entries.size],
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.OPEN,
        createdAt = 1_792_000_000_000L - index * 3_600_000L,
        categoryName = "Category $index",
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ActiveTicketsPreview() {
    CityFixTheme {
        ActiveTicketsScreen(previewTickets, onTicketClick = {}, onSeeMoreClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveTicketsEmptyPreview() {
    CityFixTheme {
        ActiveTicketsScreen(emptyList(), onTicketClick = {}, onSeeMoreClick = {})
    }
}

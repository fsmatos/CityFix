package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

const val ACTIVE_TICKETS_SHOWN = 5

@Composable
fun ActiveTicketsScreen(
    tickets: List<Ticket>,
    onTicketClick: (String) -> Unit,
    onSeeMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    // D-020: hoisted so MainActivity can scroll it to the top when the tab is tapped again.
    listState: LazyListState = rememberLazyListState(),
) {
    if (tickets.isEmpty()) {
        EmptyState(messageRes = R.string.ticket_list_empty, modifier = modifier)
        return
    }

    // Centred and at most contentMaxWidth wide, so the cards do not stretch in landscape.
    val itemModifier = Modifier
        .widthIn(max = Dimens.contentMaxWidth)
        .fillMaxWidth()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            top = Dimens.sm,
            end = Dimens.screenPadding,
            // Clears the floating island (0.dp when there is none).
            bottom = maxOf(Dimens.lg, LocalBottomBarPadding.current),
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(tickets.take(ACTIVE_TICKETS_SHOWN), key = { it.id }) { ticket ->
            TicketCard(
                ticket = ticket,
                isExpanded = false,
                onClick = { onTicketClick(ticket.id) },
                modifier = itemModifier.animateItem(),
            )
        }
        if (tickets.size > ACTIVE_TICKETS_SHOWN) {
            item {
                // Discreet: a neutral text button, no outline (D-016).
                TextButton(
                    onClick = onSeeMoreClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.padding(top = Dimens.xs),
                ) {
                    Text(
                        text = stringResource(R.string.active_see_more),
                        style = MaterialTheme.typography.bodyLarge,
                    )
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

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActiveTicketsDarkPreview() {
    CityFixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ActiveTicketsScreen(previewTickets, onTicketClick = {}, onSeeMoreClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveTicketsEmptyPreview() {
    CityFixTheme {
        ActiveTicketsScreen(emptyList(), onTicketClick = {}, onSeeMoreClick = {})
    }
}

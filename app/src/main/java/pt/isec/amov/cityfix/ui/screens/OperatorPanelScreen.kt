package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketFilter
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.effectiveStatus
import pt.isec.amov.cityfix.model.filteredFor
import pt.isec.amov.cityfix.ui.components.TicketCard
import pt.isec.amov.cityfix.ui.components.statusLabelRes
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.OperatorPanelUiState
import pt.isec.amov.cityfix.viewmodel.OperatorPanelViewModel

/**
 * The operator's home (brief I.3.E): every ticket, PUBLIC and PRIVATE, any status, with status
 * and category filters and a date sort. [tickets] is the shared list (allTickets() for an
 * operator); the filter lives in [viewModel] so it survives rotation. Stateful half.
 */
@Composable
fun OperatorPanelScreen(
    tickets: List<Ticket>,
    viewModel: OperatorPanelViewModel,
    onTicketClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Read once per composition: the badges and the status filter use the same instant.
    val now = System.currentTimeMillis()
    OperatorPanelContent(
        tickets = tickets,
        now = now,
        state = state,
        onStatusFilter = viewModel::onStatusFilter,
        onCategoryFilter = viewModel::onCategoryFilter,
        onToggleSort = viewModel::onToggleSort,
        onTicketClick = onTicketClick,
        modifier = modifier,
    )
}

/**
 * Stateless panel: the filter header scrolls with the list (first item), so in landscape it never
 * takes the whole height. Width capped at 600dp and centred. The badge is the effective status at
 * [now] (decision 0011). PRIVATE tickets get a "Private" label under the card.
 */
@Composable
fun OperatorPanelContent(
    tickets: List<Ticket>,
    now: Long,
    state: OperatorPanelUiState,
    onStatusFilter: (TicketStatus?) -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onToggleSort: () -> Unit,
    onTicketClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = tickets.filteredFor(state.filter, now)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "filters") {
                FilterHeader(
                    state = state,
                    onStatusFilter = onStatusFilter,
                    onCategoryFilter = onCategoryFilter,
                    onToggleSort = onToggleSort,
                )
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(
                            if (tickets.isEmpty()) R.string.operator_panel_empty else R.string.operator_panel_no_match,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { ticket ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TicketCard(
                        ticket = ticket,
                        isExpanded = true,
                        onClick = { onTicketClick(ticket.id) },
                        status = ticket.effectiveStatus(now),
                    )
                    if (ticket.scope == Scope.PRIVATE) {
                        Text(
                            text = stringResource(R.string.operator_panel_private),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Status chips (one row, scrolls sideways), category dropdown and the sort chip. */
@Composable
private fun FilterHeader(
    state: OperatorPanelUiState,
    onStatusFilter: (TicketStatus?) -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onToggleSort: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.filter.status == null,
                onClick = { onStatusFilter(null) },
                label = { Text(stringResource(R.string.operator_panel_filter_all)) },
            )
            TicketStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.filter.status == status,
                    onClick = { onStatusFilter(status) },
                    label = { Text(stringResource(statusLabelRes(status))) },
                )
            }
        }
        CategoryFilter(
            categories = state.categories,
            selectedId = state.filter.categoryId,
            onSelect = onCategoryFilter,
        )
        val sortLabel = stringResource(
            if (state.filter.newestFirst) R.string.operator_panel_sort_newest else R.string.operator_panel_sort_oldest,
        )
        val sortDescription = stringResource(R.string.cd_operator_panel_sort, sortLabel)
        AssistChip(
            onClick = onToggleSort,
            label = { Text(sortLabel) },
            leadingIcon = {
                Icon(
                    imageVector = if (state.filter.newestFirst) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                )
            },
            modifier = Modifier.semantics { contentDescription = sortDescription },
        )
    }
}

/**
 * Read-only dropdown: "All categories" plus every category, inactive ones marked, so tickets of a
 * deactivated category can still be found. A selected id that is no longer listed shows "All".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilter(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val allLabel = stringResource(R.string.operator_panel_category_all)
    val selected = categories.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.let { categoryLabel(it) } ?: allLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.operator_panel_category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(categoryLabel(category)) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun categoryLabel(category: Category): String =
    if (category.active) {
        category.name
    } else {
        stringResource(R.string.operator_panel_category_inactive, category.name)
    }

private const val DAY = 86_400_000L

/** 2026-10-15 18:30 UTC. */
private const val PREVIEW_T0 = 1_792_089_000_000L

private val previewCategories = listOf(
    Category(id = "c1", name = "Pavement"),
    Category(id = "c2", name = "Public lighting"),
    Category(id = "c3", name = "Old signs", active = false),
)

/** OPEN PUBLIC, INFO_REQUESTED PRIVATE, IN_PROGRESS, and RESOLVED at t0 (shown CLOSED at t0 + 6 d). */
private val previewTickets = listOf(
    Ticket(
        id = "p1",
        description = "Broken pavement next to the bus stop.",
        situation = Situation.DAMAGE,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.OPEN,
        createdAt = PREVIEW_T0 + 5 * DAY,
        categoryId = "c1",
        categoryName = "Pavement",
        statusHistory = listOf(StatusHistory(TicketStatus.OPEN, PREVIEW_T0 + 5 * DAY)),
    ),
    Ticket(
        id = "p2",
        description = "Street light off at night.",
        situation = Situation.BREAKDOWN,
        scope = Scope.PRIVATE,
        currentStatus = TicketStatus.INFO_REQUESTED,
        createdAt = PREVIEW_T0 + 4 * DAY,
        categoryId = "c2",
        categoryName = "Public lighting",
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, PREVIEW_T0 + 4 * DAY),
            StatusHistory(TicketStatus.INFO_REQUESTED, PREVIEW_T0 + 5 * DAY, "Which side of the street?"),
        ),
    ),
    Ticket(
        id = "p3",
        description = "Hole in the road.",
        situation = Situation.DAMAGE,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.IN_PROGRESS,
        createdAt = PREVIEW_T0 + 2 * DAY,
        categoryId = "c1",
        categoryName = "Pavement",
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, PREVIEW_T0 + 2 * DAY),
            StatusHistory(TicketStatus.IN_PROGRESS, PREVIEW_T0 + 3 * DAY),
        ),
    ),
    Ticket(
        id = "p4",
        description = "Faded sign.",
        situation = Situation.DAMAGE,
        scope = Scope.PRIVATE,
        currentStatus = TicketStatus.RESOLVED,
        createdAt = PREVIEW_T0 - DAY,
        categoryId = "c3",
        categoryName = "Old signs",
        statusHistory = listOf(
            StatusHistory(TicketStatus.OPEN, PREVIEW_T0 - DAY),
            StatusHistory(TicketStatus.RESOLVED, PREVIEW_T0),
        ),
    ),
)

@Composable
private fun PanelPreview(filter: TicketFilter, tickets: List<Ticket> = previewTickets) {
    CityFixTheme {
        OperatorPanelContent(
            tickets = tickets,
            now = PREVIEW_T0 + 6 * DAY,
            state = OperatorPanelUiState(categories = previewCategories, filter = filter),
            onStatusFilter = {},
            onCategoryFilter = {},
            onToggleSort = {},
            onTicketClick = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun OperatorPanelAllPreview() {
    PanelPreview(TicketFilter())
}

@Preview(showBackground = true)
@Composable
private fun OperatorPanelClosedPreview() {
    PanelPreview(TicketFilter(status = TicketStatus.CLOSED, newestFirst = false))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorPanelNoMatchPreview() {
    PanelPreview(TicketFilter(status = TicketStatus.RESOLVED, categoryId = "c2"))
}

@Preview(showBackground = true)
@Composable
private fun OperatorPanelEmptyPreview() {
    PanelPreview(TicketFilter(), tickets = emptyList())
}

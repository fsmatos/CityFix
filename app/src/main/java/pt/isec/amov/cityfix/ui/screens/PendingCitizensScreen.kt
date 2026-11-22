package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.ui.components.CityCard
import pt.isec.amov.cityfix.ui.components.EmptyState
import pt.isec.amov.cityfix.ui.components.LoadingState
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens
import pt.isec.amov.cityfix.viewmodel.PendingCitizensUiState
import pt.isec.amov.cityfix.viewmodel.PendingCitizensViewModel

/** Registration approvals (brief I.3.D), the stateful half: collects the ViewModel state. */
@Composable
fun PendingCitizensScreen(
    viewModel: PendingCitizensViewModel,
    onCitizenClick: (String) -> Unit,
    // D-020: hoisted so MainActivity can scroll it to the top when the tab is tapped again.
    listState: LazyListState = rememberLazyListState(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PendingCitizensContent(state = state, onCitizenClick = onCitizenClick, listState = listState)
}

/**
 * The citizens waiting for validation, one card each with name and NIF; tapping a card opens
 * that registration. Same single column in both orientations, width capped and centred.
 */
@Composable
fun PendingCitizensContent(
    state: PendingCitizensUiState,
    onCitizenClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when {
        state.isLoading -> LoadingState(descriptionRes = R.string.cd_approvals_loading, modifier = modifier)
        state.citizens.isEmpty() -> EmptyState(messageRes = R.string.approvals_empty, modifier = modifier)
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    state = listState,
                    // The last item scrolls above the floating island (D-016).
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = maxOf(16.dp, LocalBottomBarPadding.current),
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.md),
                ) {
                    items(state.citizens, key = { it.id }) { citizen ->
                        PendingCitizenCard(citizen = citizen, onClick = { onCitizenClick(citizen.id) })
                    }
                }
            }
        }
    }
}

/** One pending citizen: name and NIF, read by TalkBack as one item (a clickable Card merges them). */
@Composable
private fun PendingCitizenCard(citizen: Citizen, onClick: () -> Unit) {
    CityCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.lg),
        ) {
            // Decorative leading thumbnail, like the ticket cards' (D-016): the profile photo
            // (a download URL, as in CitizenApprovalScreen), or a person icon without one (F-021).
            // A photo that fails to load leaves the grey box.
            Box(
                modifier = Modifier
                    .size(Dimens.thumbnail)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val photo = citizen.photo
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column {
                Text(text = citizen.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.approvals_nif, citizen.nif),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Synthetic preview data only. */
private val previewCitizens = listOf(
    Citizen(id = "c1", name = "Ana Exemplo", nif = "123456789"),
    Citizen(id = "c2", name = "Bruno Teste", nif = "987654321"),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun PendingCitizensContentPreview() {
    CityFixTheme {
        PendingCitizensContent(
            state = PendingCitizensUiState(citizens = previewCitizens, isLoading = false),
            onCitizenClick = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun PendingCitizensContentEmptyPreview() {
    CityFixTheme {
        PendingCitizensContent(state = PendingCitizensUiState(isLoading = false), onCitizenClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PendingCitizensContentLoadingPreview() {
    CityFixTheme {
        PendingCitizensContent(state = PendingCitizensUiState(), onCitizenClick = {})
    }
}

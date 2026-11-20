package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.PendingCitizensUiState
import pt.isec.amov.cityfix.viewmodel.PendingCitizensViewModel

/** Registration approvals (brief I.3.D), the stateful half: collects the ViewModel state. */
@Composable
fun PendingCitizensScreen(
    viewModel: PendingCitizensViewModel,
    onCitizenClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PendingCitizensContent(state = state, onCitizenClick = onCitizenClick)
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
) {
    when {
        state.isLoading -> {
            val loadingDescription = stringResource(R.string.cd_approvals_loading)
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }
        state.citizens.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.approvals_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = citizen.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.approvals_nif, citizen.nif),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

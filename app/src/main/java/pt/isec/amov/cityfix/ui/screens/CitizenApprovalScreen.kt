package pt.isec.amov.cityfix.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.ui.components.TicketMap
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.CitizenApprovalUiState
import pt.isec.amov.cityfix.viewmodel.CitizenApprovalViewModel

/**
 * A pending registration (brief I.3.D), the stateful half: collects the ViewModel state and
 * wires the events. [onActivated] runs after the activation is written (snackbar, back).
 */
@Composable
fun CitizenApprovalScreen(
    viewModel: CitizenApprovalViewModel,
    onActivated: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CitizenApprovalContent(
        state = state,
        onActivateClick = viewModel::onActivateClick,
        onConfirm = { viewModel.activate(onActivated) },
        onDismissConfirm = viewModel::onDismissConfirm,
    )
}

/**
 * The citizen's name, NIF, e-mail, phone, photo and registered location, with Activate behind a
 * confirmation dialog. Nothing is editable. Portrait: one scrolling column. Landscape: details
 * and the button on the left, the map on the right (P07).
 */
@Composable
fun CitizenApprovalContent(
    state: CitizenApprovalUiState,
    onActivateClick: () -> Unit,
    onConfirm: () -> Unit,
    onDismissConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val citizen = state.citizen
    if (state.isLoading) {
        val loadingDescription = stringResource(R.string.cd_citizen_approval_loading)
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = loadingDescription },
            )
        }
        return
    }
    if (state.notFound || citizen == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.citizen_approval_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Hoisted so the positions survive rotation (as ShowScreen).
    val detailsScroll = rememberScrollState()
    val locationScroll = rememberScrollState()

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CitizenDetails(
                citizen = citizen,
                canActivate = state.canActivate,
                onActivateClick = onActivateClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(detailsScroll),
            )
            CitizenLocation(
                location = citizen.georeferencing,
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
            CitizenDetails(citizen, state.canActivate, onActivateClick)
            CitizenLocation(citizen.georeferencing)
        }
    }

    if (state.confirming) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(stringResource(R.string.citizen_approval_confirm_title)) },
            text = { Text(stringResource(R.string.citizen_approval_confirm_text, citizen.name)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.citizen_approval_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirm) {
                    Text(stringResource(R.string.citizen_approval_cancel))
                }
            },
        )
    }
}

/** Photo, name (the page heading), NIF, e-mail, phone, then Activate or "already active". */
@Composable
private fun CitizenDetails(
    citizen: Citizen,
    canActivate: Boolean,
    onActivateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        citizen.photo?.let { photo ->
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.cd_citizen_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
            )
        }
        Text(
            text = citizen.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        LabelledValue(stringResource(R.string.citizen_approval_nif), citizen.nif)
        LabelledValue(stringResource(R.string.citizen_approval_email), citizen.email)
        if (citizen.phone.isNotBlank()) {
            LabelledValue(
                stringResource(R.string.citizen_approval_phone),
                stringResource(R.string.setup_phone_display, citizen.phone),
            )
        }
        if (canActivate) {
            Button(onClick = onActivateClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.citizen_approval_activate))
            }
        } else {
            Text(
                text = stringResource(R.string.citizen_approval_already_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A read-only label over its value, read by TalkBack as one item. */
@Composable
private fun LabelledValue(label: String, value: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The registered location on the exact map (F-012), or "Location not available" for (0, 0). */
@Composable
private fun CitizenLocation(location: Coordinates, modifier: Modifier = Modifier) {
    val mapDescription = stringResource(R.string.cd_citizen_map)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.citizen_approval_location_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (location == Coordinates()) {
            Text(
                text = stringResource(R.string.citizen_approval_no_location),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TicketMap(
                location = location,
                exact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .semantics { contentDescription = mapDescription },
            )
        }
    }
}

/** Synthetic preview data only. */
private val previewCitizen = Citizen(
    id = "c1",
    name = "Ana Exemplo",
    nif = "123456789",
    email = "ana@example.com",
    phone = "912345678",
    georeferencing = Coordinates(40.2033, -8.4103),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun CitizenApprovalContentPreview() {
    CityFixTheme {
        CitizenApprovalContent(
            state = CitizenApprovalUiState(citizen = previewCitizen, isLoading = false),
            onActivateClick = {},
            onConfirm = {},
            onDismissConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CitizenApprovalContentNoLocationPreview() {
    CityFixTheme {
        CitizenApprovalContent(
            state = CitizenApprovalUiState(
                citizen = previewCitizen.copy(georeferencing = Coordinates()),
                isLoading = false,
            ),
            onActivateClick = {},
            onConfirm = {},
            onDismissConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CitizenApprovalContentAlreadyActivePreview() {
    CityFixTheme {
        CitizenApprovalContent(
            state = CitizenApprovalUiState(
                citizen = previewCitizen.copy(registrationStatus = RegistrationStatus.ACTIVE),
                isLoading = false,
            ),
            onActivateClick = {},
            onConfirm = {},
            onDismissConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CitizenApprovalContentNotFoundPreview() {
    CityFixTheme {
        CitizenApprovalContent(
            state = CitizenApprovalUiState(isLoading = false, notFound = true),
            onActivateClick = {},
            onConfirm = {},
            onDismissConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CitizenApprovalContentConfirmPreview() {
    CityFixTheme {
        CitizenApprovalContent(
            state = CitizenApprovalUiState(citizen = previewCitizen, isLoading = false, confirming = true),
            onActivateClick = {},
            onConfirm = {},
            onDismissConfirm = {},
        )
    }
}

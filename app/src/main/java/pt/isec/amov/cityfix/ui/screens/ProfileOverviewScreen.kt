package pt.isec.amov.cityfix.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.effectiveStatus
import pt.isec.amov.cityfix.ui.components.TicketCard
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.ProfileViewModel

/**
 * Stateful Profile tab (D-016): reads the profile from [viewModel] (the same ProfileViewModel as
 * Edit profile, used here read-only) and shows [ProfileOverviewContent]. [myTickets] are the
 * tickets a citizen created or follows (the former My tickets screen, F-014), null for an operator.
 */
@Composable
fun ProfileOverviewScreen(
    viewModel: ProfileViewModel,
    myTickets: List<Ticket>?,
    now: Long,
    onEditDetails: () -> Unit,
    onTicketClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileOverviewContent(
        profile = state.profile,
        unavailable = state.unavailable,
        email = state.email,
        photo = rememberProfilePhotoModel(state.profile?.photo, state.photoUpdatedAt),
        myTickets = myTickets,
        now = now,
        onEditDetails = onEditDetails,
        onTicketClick = onTicketClick,
        modifier = modifier,
    )
}

/**
 * The user's details, "Edit details" and, for citizens, their tickets, in one scrolling list.
 * The cards' badge is the effective status at [now] (decision 0011), as on My tickets.
 * Stateless and previewable.
 */
@Composable
fun ProfileOverviewContent(
    profile: User?,
    unavailable: Boolean,
    email: String,
    photo: Any?,
    myTickets: List<Ticket>?,
    now: Long,
    onEditDetails: () -> Unit,
    onTicketClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (profile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (unavailable) {
                Text(
                    text = stringResource(R.string.profile_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            } else {
                val loadingDescription = stringResource(R.string.cd_profile_loading)
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }
        return
    }

    // Centred and at most 600dp wide, so the rows do not stretch in landscape.
    val itemModifier = Modifier
        .widthIn(max = 600.dp)
        .fillMaxWidth()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // The last item scrolls above the floating island (D-016).
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = maxOf(16.dp, LocalBottomBarPadding.current),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ProfileHeader(profile, photo, itemModifier) }
        item { ProfileDetails(profile, email, itemModifier) }
        item {
            Button(onClick = onEditDetails, modifier = itemModifier) {
                Text(stringResource(R.string.profile_edit_details))
            }
        }
        if (myTickets != null) {
            item {
                Text(
                    text = stringResource(R.string.profile_my_tickets),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = itemModifier.padding(top = 8.dp),
                )
            }
            if (myTickets.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.my_tickets_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = itemModifier,
                    )
                }
            } else {
                items(myTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        isExpanded = false,
                        onClick = { onTicketClick(ticket.id) },
                        modifier = itemModifier,
                        status = ticket.effectiveStatus(now),
                    )
                }
            }
        }
    }
}

/** Photo (or a person icon), name and role of the user. */
@Composable
private fun ProfileHeader(profile: User, photo: Any?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val photoModifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.cd_register_photo),
                contentScale = ContentScale.Crop,
                modifier = photoModifier,
            )
        } else {
            Box(
                modifier = photoModifier.background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = profile.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(
                    if (profile is Citizen) R.string.register_role_citizen else R.string.register_role_operator,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The read-only details, one label/value pair per line. */
@Composable
private fun ProfileDetails(profile: User, email: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailRow(R.string.login_email_label, email)
            if (profile.phone.isNotBlank()) {
                DetailRow(
                    R.string.register_phone_label,
                    stringResource(R.string.setup_phone_prefix) + profile.phone,
                )
            }
            DetailRow(R.string.register_nif_label, profile.nif)
            if (profile is Citizen) {
                DetailRow(
                    R.string.profile_status_label,
                    stringResource(
                        when (profile.registrationStatus) {
                            RegistrationStatus.PENDING -> R.string.profile_status_pending
                            RegistrationStatus.ACTIVE -> R.string.profile_status_active
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(@StringRes labelRes: Int, value: String) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private val previewCitizen = Citizen(
    id = "c1",
    name = "Ana Silva",
    nif = "123456789",
    email = "ana@example.com",
    phone = "912345678",
    georeferencing = Coordinates(40.21, -8.419),
    registrationStatus = RegistrationStatus.ACTIVE,
)

private val previewTickets = listOf(
    Ticket(
        id = "p1",
        situation = Situation.BREAKDOWN,
        scope = Scope.PUBLIC,
        currentStatus = TicketStatus.OPEN,
        createdAt = 1_792_000_000_000L,
        categoryName = "Public lighting",
    ),
    Ticket(
        id = "p2",
        situation = Situation.DAMAGE,
        scope = Scope.PRIVATE,
        currentStatus = TicketStatus.IN_PROGRESS,
        createdAt = 1_791_000_000_000L,
        categoryName = "Pavement",
    ),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ProfileOverviewCitizenPreview() {
    CityFixTheme {
        ProfileOverviewContent(
            profile = previewCitizen,
            unavailable = false,
            email = previewCitizen.email,
            photo = null,
            myTickets = previewTickets,
            now = 1_792_100_000_000L,
            onEditDetails = {},
            onTicketClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileOverviewOperatorPreview() {
    CityFixTheme {
        ProfileOverviewContent(
            profile = Operator(id = "o1", name = "Rui Costa", nif = "987654321", email = "rui@example.com", phone = "913456789"),
            unavailable = false,
            email = "rui@example.com",
            photo = null,
            myTickets = null,
            now = 0L,
            onEditDetails = {},
            onTicketClick = {},
        )
    }
}

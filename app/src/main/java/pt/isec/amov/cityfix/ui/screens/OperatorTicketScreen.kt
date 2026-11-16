package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.allowedTransitions
import pt.isec.amov.cityfix.model.isLocked
import pt.isec.amov.cityfix.ui.components.MessageThread
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.OperatorDialog
import pt.isec.amov.cityfix.viewmodel.OperatorTicketUiState
import pt.isec.amov.cityfix.viewmodel.OperatorTicketViewModel
import pt.isec.amov.cityfix.viewmodel.PersonContact

/**
 * Stateful operator ticket detail (brief I.3.E, D-013): collects [viewModel] and passes everything
 * to [OperatorTicketContent]. Only for an operator (the operator branch of the detail destination).
 *
 * Writes an expired RESOLVED ticket as CLOSED once (as F-014), and keeps one profile listener per
 * person on the ticket (creator, followers, history responsibles).
 */
@Composable
fun OperatorTicketScreen(
    ticket: Ticket?,
    operator: Operator,
    viewModel: OperatorTicketViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    LaunchedEffect(ticket) {
        ticket?.let(viewModel::closeIfExpired)
    }
    val ids = ticket?.let { t ->
        setOf(t.creatorId) + t.followerIds + t.statusHistory.map { it.responsibleId }
    }.orEmpty()
    LaunchedEffect(ids) {
        viewModel.watchPeople(ids)
    }

    OperatorTicketContent(
        ticket = ticket,
        operator = operator,
        now = now,
        state = state,
        onDraftChange = viewModel::onDraftChange,
        onSendClick = viewModel::sendMessage,
        onRequestInfoClick = viewModel::onRequestInfoClick,
        onStartWorkClick = viewModel::onStartWorkClick,
        onDialogTextChange = viewModel::onDialogTextChange,
        onDialogConfirm = { ticket?.let(viewModel::confirmDialog) },
        onDialogDismiss = viewModel::onDialogDismiss,
        modifier = modifier,
    )
}

/**
 * Stateless operator detail: ShowScreen with the OPERATOR audience (description, exact map, full
 * history, all media) and, in its slot, the actions, the contacts and the message thread.
 *
 * The actions are exactly allowedTransitions(operator, now) (D-008): INFO_REQUESTED -> "Request
 * information", IN_PROGRESS -> "Start work". A closed ticket offers none and takes no new
 * messages (brief II).
 */
@Composable
fun OperatorTicketContent(
    ticket: Ticket?,
    operator: Operator,
    now: Long,
    state: OperatorTicketUiState,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onRequestInfoClick: () -> Unit,
    onStartWorkClick: () -> Unit,
    onDialogTextChange: (String) -> Unit,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moves = ticket?.allowedTransitions(operator, now).orEmpty()
    val canSend = ticket != null && !ticket.isLocked(now)
    // "By <name>" in the history (D-013 Q3): only the people whose profile could be read.
    val responsibleNames = state.people.mapNotNull { (id, contact) -> contact?.let { id to it.name } }.toMap()

    // imePadding: the message input stays above the keyboard.
    ShowScreen(
        ticket = ticket,
        audience = DetailAudience.OPERATOR,
        now = now,
        modifier = modifier.imePadding(),
        responsibleNames = responsibleNames,
    ) {
        if (moves.isNotEmpty()) {
            ActionsRow(
                moves = moves,
                onRequestInfoClick = onRequestInfoClick,
                onStartWorkClick = onStartWorkClick,
            )
        }
        if (ticket != null) {
            HorizontalDivider()
            ContactsSection(ticket = ticket, people = state.people)
        }
        HorizontalDivider()
        MessageThread(
            messages = state.messages,
            currentUserId = operator.id,
            draft = state.draft,
            canSend = canSend,
            onDraftChange = onDraftChange,
            onSendClick = onSendClick,
        )
    }

    if (state.openDialog != null) {
        OperatorTextDialog(
            dialog = state.openDialog,
            text = state.dialogText,
            errorRes = state.dialogError,
            onTextChange = onDialogTextChange,
            onConfirm = onDialogConfirm,
            onDismiss = onDialogDismiss,
        )
    }
}

/** One button per operator move offered now; wraps to a second line when the pane is narrow. */
@Composable
private fun ActionsRow(
    moves: Set<TicketStatus>,
    onRequestInfoClick: () -> Unit,
    onStartWorkClick: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (TicketStatus.IN_PROGRESS in moves) {
            Button(onClick = onStartWorkClick) {
                Text(stringResource(R.string.operator_action_start_work))
            }
        }
        if (TicketStatus.INFO_REQUESTED in moves) {
            OutlinedButton(onClick = onRequestInfoClick) {
                Text(stringResource(R.string.operator_action_request_info))
            }
        }
    }
}

/**
 * The request-information or start-work dialog (AlertDialog, as F-014's TextDialog) with one
 * text field: the required request or the optional note. The error, if any, is under the field.
 */
@Composable
private fun OperatorTextDialog(
    dialog: OperatorDialog,
    text: String,
    errorRes: Int?,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes: Int
    val messageRes: Int
    val labelRes: Int
    val confirmRes: Int
    when (dialog) {
        OperatorDialog.REQUEST_INFO -> {
            titleRes = R.string.operator_request_info_title
            messageRes = R.string.operator_request_info_text
            labelRes = R.string.operator_request_info_label
            confirmRes = R.string.operator_request_info_confirm
        }
        OperatorDialog.START_WORK -> {
            titleRes = R.string.operator_start_work_title
            messageRes = R.string.operator_start_work_text
            labelRes = R.string.operator_start_work_label
            confirmRes = R.string.operator_start_work_confirm
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(messageRes))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(labelRes)) },
                    isError = errorRes != null,
                    supportingText = errorRes?.let { { Text(stringResource(it)) } },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(confirmRes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** "Contacts": the author and each follower (brief I.3.E). Operators only. */
@Composable
private fun ContactsSection(
    ticket: Ticket,
    people: Map<String, PersonContact?>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.operator_ticket_contacts_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.operator_ticket_author),
            style = MaterialTheme.typography.titleSmall,
        )
        ContactRow(id = ticket.creatorId, people = people)
        Text(
            text = stringResource(R.string.operator_ticket_followers),
            style = MaterialTheme.typography.titleSmall,
        )
        if (ticket.followerIds.isEmpty()) {
            Text(
                text = stringResource(R.string.operator_ticket_no_followers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ticket.followerIds.forEach { id -> ContactRow(id = id, people = people) }
        }
    }
}

/** One person's name, e-mail and phone, read by TalkBack as one item. */
@Composable
private fun ContactRow(
    id: String,
    people: Map<String, PersonContact?>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (id !in people) {
            Text(
                text = stringResource(R.string.operator_ticket_contact_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val contact = people[id]
        if (contact == null) {
            Text(
                text = stringResource(R.string.operator_ticket_contact_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
        if (contact.email.isNotBlank()) {
            Text(text = contact.email, style = MaterialTheme.typography.bodyMedium)
        }
        if (contact.phone.isNotBlank()) {
            Text(
                text = stringResource(R.string.operator_ticket_phone, contact.phone),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val DAY = 86_400_000L

/** 2026-10-15 18:30 UTC. */
private const val PREVIEW_T0 = 1_792_089_000_000L

private val previewOperator = Operator(id = "op-1", name = "Operator One")

/** Synthetic data only: no real names, e-mails or phones. */
private val previewPeople: Map<String, PersonContact?> = mapOf(
    "c-1" to PersonContact("Citizen One", "citizen.one@example.com", "900000001", false),
    "c-2" to null,
    "op-1" to PersonContact("Operator One", "operator.one@example.org", "900000002", true),
)

private val previewOpen = Ticket(
    id = "preview-1",
    description = "The bench in front of the school is broken.",
    situation = Situation.DAMAGE,
    scope = Scope.PRIVATE,
    location = Coordinates(40.2100, -8.4190),
    currentStatus = TicketStatus.OPEN,
    createdAt = PREVIEW_T0,
    categoryName = "Urban furniture",
    creatorId = "c-1",
    followerIds = listOf("c-2", "c-3"),
    statusHistory = listOf(StatusHistory(TicketStatus.OPEN, PREVIEW_T0, null, "c-1")),
)

private val previewMessages = listOf(
    Message(id = "m1", text = "Is anyone coming?", sentAt = PREVIEW_T0 + DAY, authorId = "c-1"),
)

@Composable
private fun PreviewContent(ticket: Ticket?, now: Long, state: OperatorTicketUiState) {
    CityFixTheme {
        OperatorTicketContent(
            ticket = ticket,
            operator = previewOperator,
            now = now,
            state = state,
            onDraftChange = {},
            onSendClick = {},
            onRequestInfoClick = {},
            onStartWorkClick = {},
            onDialogTextChange = {},
            onDialogConfirm = {},
            onDialogDismiss = {},
        )
    }
}

/** OPEN, with one contact loaded, one unavailable and one still loading. */
@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun OperatorTicketOpenPreview() {
    PreviewContent(
        previewOpen,
        PREVIEW_T0 + DAY,
        OperatorTicketUiState(messages = previewMessages, people = previewPeople),
    )
}

package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.allowedTransitions
import pt.isec.amov.cityfix.model.isLocked
import pt.isec.amov.cityfix.model.isAnswered
import pt.isec.amov.cityfix.model.pendingInfoRequest
import pt.isec.amov.cityfix.ui.components.MessageThread
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.components.SectionCard
import pt.isec.amov.cityfix.ui.components.InfoRequestCard
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.DetailDialog
import pt.isec.amov.cityfix.viewmodel.TicketDetailUiState
import pt.isec.amov.cityfix.viewmodel.TicketDetailViewModel

@Composable
fun TicketDetailScreen(
    ticket: Ticket?,
    user: User,
    viewModel: TicketDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    LaunchedEffect(ticket) {
        ticket?.let(viewModel::closeIfExpired)
    }

    TicketDetailContent(
        ticket = ticket,
        user = user,
        now = now,
        state = state,
        onDraftChange = viewModel::onDraftChange,
        onSendClick = viewModel::sendMessage,
        onReopenClick = viewModel::onReopenClick,
        onAskClarificationClick = viewModel::onAskClarificationClick,
        onDialogTextChange = viewModel::onDialogTextChange,
        onDialogConfirm = { ticket?.let(viewModel::confirmDialog) },
        onDialogDismiss = viewModel::onDialogDismiss,
        modifier = modifier,
    )
}

@Composable
fun TicketDetailContent(
    ticket: Ticket?,
    user: User,
    now: Long,
    state: TicketDetailUiState,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onReopenClick: () -> Unit,
    onAskClarificationClick: () -> Unit,
    onDialogTextChange: (String) -> Unit,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moves = ticket?.allowedTransitions(user, now) ?: emptySet()
    val canReopen = TicketStatus.OPEN in moves
    val canAskClarification = user is Citizen && TicketStatus.INFO_REQUESTED in moves
    val canSend = ticket != null && !ticket.isLocked(now)

    // imePadding: the message input stays above the keyboard.
    ShowScreen(
        ticket = ticket,
        audience = detailAudience(user, ticket),
        now = now,
        modifier = modifier.imePadding(),
    ) {
        // D-018: the open information request, for either side's question.
        val infoRequest = ticket?.pendingInfoRequest()
        if (infoRequest != null) {
            InfoRequestCard(request = infoRequest, answered = infoRequest.isAnswered(state.messages))
        }
        if (canReopen || canAskClarification) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canReopen) {
                    PrimaryButton(onClick = onReopenClick) {
                        Text(stringResource(R.string.reopen_button))
                    }
                }
                if (canAskClarification) {
                    FilledTonalButton(onClick = onAskClarificationClick) {
                        Text(stringResource(R.string.clarification_button))
                    }
                }
            }
        }
        // D-016: the thread is its own section card.
        SectionCard {
            MessageThread(
                messages = state.messages,
                currentUserId = user.id,
                draft = state.draft,
                canSend = canSend,
                onDraftChange = onDraftChange,
                onSendClick = onSendClick,
            )
        }
    }

    if (state.openDialog != null) {
        TextDialog(
            dialog = state.openDialog,
            text = state.dialogText,
            errorRes = state.dialogError,
            onTextChange = onDialogTextChange,
            onConfirm = onDialogConfirm,
            onDismiss = onDialogDismiss,
        )
    }
}

@Composable
private fun TextDialog(
    dialog: DetailDialog,
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
        DetailDialog.REOPEN -> {
            titleRes = R.string.reopen_dialog_title
            messageRes = R.string.reopen_dialog_text
            labelRes = R.string.reopen_justification_label
            confirmRes = R.string.reopen_confirm
        }
        DetailDialog.ASK_CLARIFICATION -> {
            titleRes = R.string.clarification_dialog_title
            messageRes = R.string.clarification_dialog_text
            labelRes = R.string.clarification_question_label
            confirmRes = R.string.clarification_confirm
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

private const val DAY = 86_400_000L

private const val PREVIEW_RESOLVED = 1_792_089_000_000L

private val previewUser = Citizen(id = "c-1", registrationStatus = RegistrationStatus.ACTIVE)

private val previewResolved = Ticket(
    id = "preview-1",
    description = "The bench in front of the school is broken.",
    situation = Situation.DAMAGE,
    scope = Scope.PUBLIC,
    location = Coordinates(40.2100, -8.4190),
    currentStatus = TicketStatus.RESOLVED,
    createdAt = PREVIEW_RESOLVED - 3 * DAY,
    categoryName = "Urban furniture",
    creatorId = "c-1",
    statusHistory = listOf(
        StatusHistory(TicketStatus.OPEN, PREVIEW_RESOLVED - 3 * DAY, null, "c-1"),
        StatusHistory(TicketStatus.IN_PROGRESS, PREVIEW_RESOLVED - 2 * DAY, null, "op-1"),
        StatusHistory(TicketStatus.RESOLVED, PREVIEW_RESOLVED, null, "op-1"),
    ),
)

private val previewMessages = listOf(
    Message(id = "m1", text = "Is anyone coming?", sentAt = PREVIEW_RESOLVED - 2 * DAY, authorId = "c-1"),
    Message(
        id = "m2",
        text = "A team is on its way.",
        sentAt = PREVIEW_RESOLVED - 2 * DAY + 3_600_000L,
        authorId = "op-1",
        byOperator = true,
    ),
)

@Composable
private fun PreviewContent(ticket: Ticket, now: Long, state: TicketDetailUiState) {
    CityFixTheme {
        TicketDetailContent(
            ticket = ticket,
            user = previewUser,
            now = now,
            state = state,
            onDraftChange = {},
            onSendClick = {},
            onReopenClick = {},
            onAskClarificationClick = {},
            onDialogTextChange = {},
            onDialogConfirm = {},
            onDialogDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun TicketDetailDay4Preview() {
    PreviewContent(previewResolved, PREVIEW_RESOLVED + 4 * DAY, TicketDetailUiState(messages = previewMessages))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun TicketDetailDay6Preview() {
    PreviewContent(previewResolved, PREVIEW_RESOLVED + 6 * DAY, TicketDetailUiState(messages = previewMessages))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun TicketDetailInProgressPreview() {
    val inProgress = previewResolved.copy(
        currentStatus = TicketStatus.IN_PROGRESS,
        statusHistory = previewResolved.statusHistory.dropLast(1),
    )
    PreviewContent(inProgress, PREVIEW_RESOLVED, TicketDetailUiState(messages = previewMessages, draft = "Thanks"))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun TicketDetailReopenErrorPreview() {
    PreviewContent(
        previewResolved,
        PREVIEW_RESOLVED + 4 * DAY,
        TicketDetailUiState(openDialog = DetailDialog.REOPEN, dialogError = R.string.reopen_error_blank),
    )
}

@Preview(showBackground = true)
@Composable
private fun TicketDetailClarificationPreview() {
    val inProgress = previewResolved.copy(
        currentStatus = TicketStatus.IN_PROGRESS,
        statusHistory = previewResolved.statusHistory.dropLast(1),
    )
    PreviewContent(
        inProgress,
        PREVIEW_RESOLVED,
        TicketDetailUiState(openDialog = DetailDialog.ASK_CLARIFICATION, dialogText = "When will it be fixed?"),
    )
}

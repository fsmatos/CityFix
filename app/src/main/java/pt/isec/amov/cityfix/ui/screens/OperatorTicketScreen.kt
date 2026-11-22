package pt.isec.amov.cityfix.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
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
import pt.isec.amov.cityfix.model.isAnswered
import pt.isec.amov.cityfix.model.pendingInfoRequest
import pt.isec.amov.cityfix.ui.components.AttachmentGallery
import pt.isec.amov.cityfix.ui.components.MessageThread
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.components.SectionCard
import pt.isec.amov.cityfix.ui.components.InfoRequestCard
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.viewmodel.MAX_PROOF_ATTACHMENTS
import pt.isec.amov.cityfix.viewmodel.OperatorDialog
import pt.isec.amov.cityfix.viewmodel.OperatorTicketUiState
import pt.isec.amov.cityfix.viewmodel.OperatorTicketViewModel
import pt.isec.amov.cityfix.viewmodel.PersonContact
import java.io.File

@Composable
fun OperatorTicketScreen(
    ticket: Ticket?,
    operator: Operator,
    viewModel: OperatorTicketViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    val context = LocalContext.current

    // The pending photo survives rotation and process death while the camera app is open.
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    // Launchers stay here, above ShowScreen's orientation branch: their saved key is
    // position-based, so inside a pane a rotation with the picker or camera open would lose the result.
    val pickSeveral = rememberLauncherForActivityResult(
        remember { PickMultipleVisualMedia() },
    ) { uris -> viewModel.onMediaPicked(uris) }
    val pickOne = rememberLauncherForActivityResult(
        remember { PickVisualMedia() },
    ) { uri -> viewModel.onMediaPicked(listOfNotNull(uri)) }
    val takePhoto = rememberLauncherForActivityResult(
        remember { TakePicture() },
    ) { success ->
        val path = tempCameraPath
        tempCameraPath = null
        if (path != null) attachCaptured(viewModel, path, success)
    }

    LaunchedEffect(ticket) {
        ticket?.let(viewModel::closeIfExpired)
    }
    // Review N1: a dialog whose move is no longer offered (another user changed the status) closes.
    val openDialog = state.openDialog
    val dialogGone = openDialog != null &&
        openDialog.target !in ticket?.allowedTransitions(operator, now).orEmpty()
    LaunchedEffect(dialogGone) {
        if (dialogGone) viewModel.onDialogDismiss()
    }
    val ids = ticket?.let { t ->
        setOf(t.creatorId) + t.followerIds + t.statusHistory.map { it.responsibleId } +
            t.attachments.map { it.uploadedById }
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
        onResolveClick = viewModel::onResolveClick,
        onAddFromGalleryClick = {
            val remaining = MAX_PROOF_ATTACHMENTS - state.proof.size
            // maxItems must be above 1, so the last free slot uses the single picker.
            if (remaining > 1) {
                pickSeveral.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo, maxItems = remaining))
            } else if (remaining == 1) {
                pickOne.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
            }
        },
        onTakePhotoClick = {
            // A second tap before the camera opens would overwrite the path and orphan its file.
            if (tempCameraPath == null) {
                val (file, uri) = FileUtils.createTempImageFile(context)
                tempCameraPath = file.absolutePath
                if (!launchCamera(takePhoto, uri)) {
                    tempCameraPath = null
                    file.delete()
                    viewModel.onCameraUnavailable()
                }
            }
        },
        onRemoveProof = viewModel::onRemoveProof,
        onResolveNoteChange = viewModel::onResolveNoteChange,
        onCancelResolve = viewModel::onCancelResolve,
        onConfirmResolve = { ticket?.let(viewModel::resolve) },
        modifier = modifier,
    )
}

private fun launchCamera(
    takePhoto: ManagedActivityResultLauncher<Uri, Boolean>,
    uri: Uri,
): Boolean = try {
    takePhoto.launch(uri)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

private fun attachCaptured(viewModel: OperatorTicketViewModel, path: String, success: Boolean) {
    val file = File(path)
    if (success && file.length() > 0) {
        viewModel.onPhotoTaken(path)
    } else {
        file.delete()
        if (success) viewModel.onCameraUnavailable()
    }
}

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
    onResolveClick: () -> Unit,
    onAddFromGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemoveProof: (Attachment) -> Unit,
    onResolveNoteChange: (String) -> Unit,
    onCancelResolve: () -> Unit,
    onConfirmResolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moves = ticket?.allowedTransitions(operator, now).orEmpty()
    val canSend = ticket != null && !ticket.isLocked(now)
    // D-018: the open information request, and "Resume work" when it paused work in progress.
    val infoRequest = ticket?.pendingInfoRequest()
    val resumesWork = infoRequest?.statusBefore == TicketStatus.IN_PROGRESS
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
        // Only while Resolve is still offered (review S1): the ticket updates live, so another
        // user's move hides the section and brings back the remaining actions. isResolving keeps
        // it during this operator's own save, when the cached ticket is already RESOLVED. The
        // hidden draft is kept (not deleted here); onCleared removes it when the screen leaves.
        val showResolve = state.resolving && (TicketStatus.RESOLVED in moves || state.isResolving)
        if (infoRequest != null) {
            InfoRequestCard(request = infoRequest, answered = infoRequest.isAnswered(state.messages))
        }
        if (showResolve) {
            ResolveSection(
                state = state,
                onAddFromGalleryClick = onAddFromGalleryClick,
                onTakePhotoClick = onTakePhotoClick,
                onRemoveProof = onRemoveProof,
                onNoteChange = onResolveNoteChange,
                onCancel = onCancelResolve,
                onConfirm = onConfirmResolve,
            )
        } else if (moves.isNotEmpty()) {
            ActionsRow(
                moves = moves,
                resumesWork = resumesWork,
                onRequestInfoClick = onRequestInfoClick,
                onStartWorkClick = onStartWorkClick,
                onResolveClick = onResolveClick,
            )
        }
        // D-016: contacts and the thread are section cards.
        if (ticket != null) {
            SectionCard { ContactsSection(ticket = ticket, people = state.people) }
        }
        SectionCard {
            MessageThread(
                messages = state.messages,
                currentUserId = operator.id,
                draft = state.draft,
                canSend = canSend,
                onDraftChange = onDraftChange,
                onSendClick = onSendClick,
            )
        }
    }

    // Only while its move is still offered (review N1); the stateful half then dismisses it.
    if (state.openDialog != null && state.openDialog.target in moves) {
        OperatorTextDialog(
            dialog = state.openDialog,
            resumesWork = resumesWork,
            text = state.dialogText,
            errorRes = state.dialogError,
            onTextChange = onDialogTextChange,
            onConfirm = onDialogConfirm,
            onDismiss = onDialogDismiss,
        )
    }
}

@Composable
private fun ActionsRow(
    moves: Set<TicketStatus>,
    resumesWork: Boolean,
    onRequestInfoClick: () -> Unit,
    onStartWorkClick: () -> Unit,
    onResolveClick: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (TicketStatus.IN_PROGRESS in moves) {
            PrimaryButton(onClick = onStartWorkClick) {
                Text(
                    stringResource(
                        if (resumesWork) R.string.operator_action_resume_work else R.string.operator_action_start_work,
                    ),
                )
            }
        }
        if (TicketStatus.RESOLVED in moves) {
            PrimaryButton(onClick = onResolveClick) {
                Text(stringResource(R.string.operator_action_resolve))
            }
        }
        if (TicketStatus.INFO_REQUESTED in moves) {
            FilledTonalButton(onClick = onRequestInfoClick) {
                Text(stringResource(R.string.operator_action_request_info))
            }
        }
    }
}

@Composable
private fun ResolveSection(
    state: OperatorTicketUiState,
    onAddFromGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemoveProof: (Attachment) -> Unit,
    onNoteChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val busyDescription = stringResource(R.string.cd_resolve_busy)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.resolve_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.resolve_hint, MAX_PROOF_ATTACHMENTS),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.proof.isNotEmpty()) {
            AttachmentGallery(
                attachments = state.proof,
                onRemove = onRemoveProof,
                removeEnabled = !state.isResolving,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = onAddFromGalleryClick, enabled = state.canAddProof) {
                Text(stringResource(R.string.resolve_add_gallery))
            }
            FilledTonalButton(onClick = onTakePhotoClick, enabled = state.canAddProof) {
                Text(stringResource(R.string.resolve_take_photo))
            }
        }
        OutlinedTextField(
            value = state.resolveNote,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.resolve_note_label)) },
            enabled = !state.isResolving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.resolveError != null) {
            Text(
                text = stringResource(state.resolveError),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onCancel, enabled = !state.isCopying && !state.isResolving) {
                Text(stringResource(R.string.resolve_cancel))
            }
            PrimaryButton(onClick = onConfirm, enabled = state.canConfirmResolve) {
                if (state.isResolving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = busyDescription },
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.resolve_confirm))
                }
            }
        }
    }
}

@Composable
private fun OperatorTextDialog(
    dialog: OperatorDialog,
    resumesWork: Boolean,
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
        // D-018: same move (INFO_REQUESTED -> IN_PROGRESS), worded as resuming when work had started.
        OperatorDialog.START_WORK -> if (resumesWork) {
            titleRes = R.string.operator_resume_work_title
            messageRes = R.string.operator_resume_work_text
            labelRes = R.string.operator_start_work_label
            confirmRes = R.string.operator_action_resume_work
        } else {
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
            modifier = Modifier.semantics { heading() },
        )
        ContactRow(id = ticket.creatorId, people = people)
        Text(
            text = stringResource(R.string.operator_ticket_followers),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
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

private const val PREVIEW_T0 = 1_792_089_000_000L

private val previewOperator = Operator(id = "op-1", name = "Operator One")

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
            onResolveClick = {},
            onAddFromGalleryClick = {},
            onTakePhotoClick = {},
            onRemoveProof = {},
            onResolveNoteChange = {},
            onCancelResolve = {},
            onConfirmResolve = {},
        )
    }
}

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

private val previewInfoRequested = previewOpen.copy(
    currentStatus = TicketStatus.INFO_REQUESTED,
    statusHistory = previewOpen.statusHistory +
        StatusHistory(TicketStatus.INFO_REQUESTED, PREVIEW_T0 + DAY, "Which bench, the left or the right one?", "op-1"),
)

private val previewInProgress = previewOpen.copy(
    currentStatus = TicketStatus.IN_PROGRESS,
    statusHistory = previewOpen.statusHistory +
        StatusHistory(TicketStatus.IN_PROGRESS, PREVIEW_T0 + DAY, null, "op-1"),
)

private val previewResolved = previewInProgress.copy(
    currentStatus = TicketStatus.RESOLVED,
    statusHistory = previewInProgress.statusHistory +
        StatusHistory(TicketStatus.RESOLVED, PREVIEW_T0 + 2 * DAY, null, "op-1"),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketInfoRequestedPreview() {
    PreviewContent(previewInfoRequested, PREVIEW_T0 + 2 * DAY, OperatorTicketUiState(people = previewPeople))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun OperatorTicketInProgressPreview() {
    PreviewContent(previewInProgress, PREVIEW_T0 + 2 * DAY, OperatorTicketUiState(people = previewPeople))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketResolveEmptyPreview() {
    PreviewContent(
        previewInProgress,
        PREVIEW_T0 + 2 * DAY,
        OperatorTicketUiState(people = previewPeople, resolving = true),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun OperatorTicketResolveWithProofPreview() {
    PreviewContent(
        previewInProgress,
        PREVIEW_T0 + 2 * DAY,
        OperatorTicketUiState(
            people = previewPeople,
            resolving = true,
            proof = listOf(
                Attachment(id = "proof-1"),
                Attachment(id = "proof-2", type = AttachmentType.VIDEO),
            ),
            resolveNote = "Bench replaced.",
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun OperatorTicketResolveErrorPreview() {
    PreviewContent(
        previewInProgress,
        PREVIEW_T0 + 2 * DAY,
        OperatorTicketUiState(
            people = previewPeople,
            resolving = true,
            proof = listOf(Attachment(id = "proof-1")),
            resolveError = R.string.resolve_error_failed,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketRequestInfoErrorPreview() {
    PreviewContent(
        previewOpen,
        PREVIEW_T0 + DAY,
        OperatorTicketUiState(
            people = previewPeople,
            openDialog = OperatorDialog.REQUEST_INFO,
            dialogError = R.string.operator_request_info_error_blank,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun OperatorTicketStartWorkPreview() {
    PreviewContent(
        previewOpen,
        PREVIEW_T0 + DAY,
        OperatorTicketUiState(people = previewPeople, openDialog = OperatorDialog.START_WORK, dialogText = "Team on site Monday."),
    )
}

@Preview(showBackground = true)
@Composable
private fun OperatorTicketResolvedPreview() {
    PreviewContent(previewResolved, PREVIEW_T0 + 4 * DAY, OperatorTicketUiState(messages = previewMessages, people = previewPeople))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketClosedPreview() {
    PreviewContent(previewResolved, PREVIEW_T0 + 8 * DAY, OperatorTicketUiState(messages = previewMessages, people = previewPeople))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketContactsUnavailablePreview() {
    PreviewContent(
        previewOpen,
        PREVIEW_T0 + DAY,
        OperatorTicketUiState(people = mapOf("c-1" to null, "c-2" to null, "c-3" to null)),
    )
}

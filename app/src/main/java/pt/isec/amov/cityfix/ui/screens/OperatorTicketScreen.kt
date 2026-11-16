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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import pt.isec.amov.cityfix.ui.components.AttachmentGallery
import pt.isec.amov.cityfix.ui.components.MessageThread
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.viewmodel.MAX_PROOF_ATTACHMENTS
import pt.isec.amov.cityfix.viewmodel.OperatorDialog
import pt.isec.amov.cityfix.viewmodel.OperatorTicketUiState
import pt.isec.amov.cityfix.viewmodel.OperatorTicketViewModel
import pt.isec.amov.cityfix.viewmodel.PersonContact
import java.io.File

/**
 * Stateful operator ticket detail (brief I.3.E, D-013): collects [viewModel] and passes everything
 * to [OperatorTicketContent]. Only for an operator (the operator branch of the detail destination).
 *
 * Writes an expired RESOLVED ticket as CLOSED once (as F-014), and keeps one profile listener per
 * person on the ticket (creator, followers, history responsibles). Holds the gallery and camera
 * launchers for the resolution proof, as ReportScreen does.
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

/** False when there is no camera app, or Android refuses the launch (copied from ReportScreen). */
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

/** A cancelled capture deletes its file silently; an empty "success" also shows the notice. */
private fun attachCaptured(viewModel: OperatorTicketViewModel, path: String, success: Boolean) {
    val file = File(path)
    if (success && file.length() > 0) {
        viewModel.onPhotoTaken(path)
    } else {
        file.delete()
        if (success) viewModel.onCameraUnavailable()
    }
}

/**
 * Stateless operator detail: ShowScreen with the OPERATOR audience (description, exact map, full
 * history, all media) and, in its slot, the actions, the contacts and the message thread.
 *
 * The actions are exactly allowedTransitions(operator, now) (D-008): INFO_REQUESTED -> "Request
 * information", IN_PROGRESS -> "Start work", RESOLVED -> "Resolve", which opens the proof section
 * in their place. A closed ticket offers none and takes no new messages (brief II).
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
        if (state.resolving) {
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
                onRequestInfoClick = onRequestInfoClick,
                onStartWorkClick = onStartWorkClick,
                onResolveClick = onResolveClick,
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
    onResolveClick: () -> Unit,
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
        if (TicketStatus.RESOLVED in moves) {
            Button(onClick = onResolveClick) {
                Text(stringResource(R.string.operator_action_resolve))
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
 * "Proof of the finished work" (brief I.3.E): 1 to 3 photos or videos from the gallery or the
 * camera, an optional note, and Confirm, enabled only with at least one proof (no resolution
 * without proof). The gallery has its own height cap, so it is safe inside the scrolling detail.
 */
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
            OutlinedButton(onClick = onAddFromGalleryClick, enabled = state.canAddProof) {
                Text(stringResource(R.string.resolve_add_gallery))
            }
            OutlinedButton(onClick = onTakePhotoClick, enabled = state.canAddProof) {
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
            Button(onClick = onConfirm, enabled = state.canConfirmResolve) {
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

/** RESOLVED at t0 + 2 d: shown CLOSED from t0 + 7 d (5-day window). */
private val previewResolved = previewInProgress.copy(
    currentStatus = TicketStatus.RESOLVED,
    statusHistory = previewInProgress.statusHistory +
        StatusHistory(TicketStatus.RESOLVED, PREVIEW_T0 + 2 * DAY, null, "op-1"),
)

/** INFO_REQUESTED: only Start work. */
@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketInfoRequestedPreview() {
    PreviewContent(previewInfoRequested, PREVIEW_T0 + 2 * DAY, OperatorTicketUiState(people = previewPeople))
}

/** IN_PROGRESS: Resolve and Request information. */
@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun OperatorTicketInProgressPreview() {
    PreviewContent(previewInProgress, PREVIEW_T0 + 2 * DAY, OperatorTicketUiState(people = previewPeople))
}

/** Resolve section with no proof: Confirm disabled. */
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

/** Resolve section with two proofs and a note: Confirm enabled. */
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

/** Resolve section after a failed confirm. */
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

/** Request information dialog after Send with a blank text. */
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

/** Start work dialog with a note typed. */
@Preview(showBackground = true)
@Composable
private fun OperatorTicketStartWorkPreview() {
    PreviewContent(
        previewOpen,
        PREVIEW_T0 + DAY,
        OperatorTicketUiState(people = previewPeople, openDialog = OperatorDialog.START_WORK, dialogText = "Team on site Monday."),
    )
}

/** RESOLVED inside the window: no actions, thread still open. */
@Preview(showBackground = true)
@Composable
private fun OperatorTicketResolvedPreview() {
    PreviewContent(previewResolved, PREVIEW_T0 + 4 * DAY, OperatorTicketUiState(messages = previewMessages, people = previewPeople))
}

/** Six days after RESOLVED: shown CLOSED, no actions, locked thread. */
@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorTicketClosedPreview() {
    PreviewContent(previewResolved, PREVIEW_T0 + 8 * DAY, OperatorTicketUiState(messages = previewMessages, people = previewPeople))
}

/** Offline with nothing cached: every contact unavailable. */
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

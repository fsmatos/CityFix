package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.formatDateTime
import java.util.Locale

/**
 * The message thread of a ticket (brief I.2.E, II.2), stateless: the caller owns [messages] and
 * [draft]. Each message is labelled "You" (sent by [currentUserId]), "Municipality" (an operator)
 * or "Citizen" (another participant); author ids are never shown. With [canSend] false (a closed
 * ticket, brief II) the input is replaced by a "locked" note.
 *
 * A plain Column, not a LazyColumn: it sits inside the detail's verticalScroll, and a LazyColumn
 * inside a scrolling Column crashes. D-013 reuses it for the operator's view.
 */
@Composable
fun MessageThread(
    messages: List<Message>,
    currentUserId: String,
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.messages_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (messages.isEmpty()) {
            Text(
                text = stringResource(R.string.messages_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            messages.forEach { message ->
                MessageRow(message, isMine = message.authorId == currentUserId, locale = locale)
            }
        }
        if (canSend) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    label = { Text(stringResource(R.string.message_input_label)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSendClick,
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send_message),
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.messages_locked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One message bubble: mine on the end side, the others on the start side; read as one item. */
@Composable
private fun MessageRow(
    message: Message,
    isMine: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val authorRes = when {
        isMine -> R.string.message_author_you
        message.byOperator -> R.string.message_author_operator
        else -> R.string.message_author_citizen
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            // D-016: neutral bubbles (the accent is kept for main actions); the alignment says
            // whose message it is.
            color = if (isMine) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .widthIn(max = 320.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(authorRes),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                // 0L is the "not set" default; skip it instead of showing 1 Jan 1970
                if (message.sentAt > 0L) {
                    Text(
                        text = formatDateTime(message.sentAt, locale),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** 2026-10-15 18:30 UTC. */
private const val PREVIEW_SENT = 1_792_089_000_000L

private val previewMessages = listOf(
    Message(id = "m1", text = "The lamp on the corner is also off.", sentAt = PREVIEW_SENT, authorId = "me"),
    Message(
        id = "m2",
        text = "Thank you. A team will check it this week.",
        sentAt = PREVIEW_SENT + 3_600_000L,
        authorId = "op-1",
        byOperator = true,
    ),
    Message(id = "m3", text = "Same problem on my street.", sentAt = PREVIEW_SENT + 7_200_000L, authorId = "c-2"),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun MessageThreadPreview() {
    CityFixTheme {
        MessageThread(
            messages = previewMessages,
            currentUserId = "me",
            draft = "Any news?",
            canSend = true,
            onDraftChange = {},
            onSendClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun MessageThreadEmptyPreview() {
    CityFixTheme {
        MessageThread(
            messages = emptyList(),
            currentUserId = "me",
            draft = "",
            canSend = true,
            onDraftChange = {},
            onSendClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun MessageThreadLockedPreview() {
    CityFixTheme {
        MessageThread(
            messages = previewMessages,
            currentUserId = "me",
            draft = "",
            canSend = false,
            onDraftChange = {},
            onSendClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

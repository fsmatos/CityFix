package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens

/** Returns the string resource id with the localised label of [status]. */
fun statusLabelRes(status: TicketStatus): Int = when (status) {
    TicketStatus.OPEN -> R.string.status_open
    TicketStatus.INFO_REQUESTED -> R.string.status_info_requested
    TicketStatus.IN_PROGRESS -> R.string.status_in_progress
    TicketStatus.RESOLVED -> R.string.status_resolved
    TicketStatus.CLOSED -> R.string.status_closed
}

/**
 * Non-clickable status of a ticket (D-016): a small coloured dot and the localised name in
 * neutral text, no coloured chip. The dot colour comes from the theme (CityFixTheme.statusColors)
 * and is decorative: the text alone says the status.
 */
@Composable
fun StatusBadge(status: TicketStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(CityFixTheme.statusColors.of(status), CircleShape),
        )
        Text(
            text = stringResource(statusLabelRes(status)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AllStatuses() {
    CityFixTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Dimens.lg),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm),
            ) {
                TicketStatus.entries.forEach { StatusBadge(it) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeLightPreview() {
    AllStatuses()
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatusBadgeDarkPreview() {
    AllStatuses()
}

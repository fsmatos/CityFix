package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.theme.CityFixGreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/** Returns the string resource id with the localised label of [status]. */
fun statusLabelRes(status: TicketStatus): Int = when (status) {
    TicketStatus.OPEN -> R.string.status_open
    TicketStatus.INFO_REQUESTED -> R.string.status_info_requested
    TicketStatus.IN_PROGRESS -> R.string.status_in_progress
    TicketStatus.RESOLVED -> R.string.status_resolved
    TicketStatus.CLOSED -> R.string.status_closed
}

/** Small, non-clickable coloured label with the localised name of [status]. */
@Composable
fun StatusBadge(status: TicketStatus, modifier: Modifier = Modifier) {
    val containerColor: Color
    val textColor: Color
    when (status) {
        TicketStatus.OPEN -> {
            containerColor = MaterialTheme.colorScheme.primary
            textColor = MaterialTheme.colorScheme.onPrimary
        }
        TicketStatus.INFO_REQUESTED -> {
            containerColor = MaterialTheme.colorScheme.tertiary
            textColor = MaterialTheme.colorScheme.onTertiary
        }
        TicketStatus.IN_PROGRESS -> {
            containerColor = MaterialTheme.colorScheme.secondary
            textColor = MaterialTheme.colorScheme.onSecondary
        }
        TicketStatus.RESOLVED -> {
            containerColor = CityFixGreen
            textColor = Color.White
        }
        TicketStatus.CLOSED -> {
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = textColor,
    ) {
        Text(
            text = stringResource(statusLabelRes(status)),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun StatusBadgePreview() {
    CityFixTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TicketStatus.entries.forEach { StatusBadge(it) }
        }
    }
}

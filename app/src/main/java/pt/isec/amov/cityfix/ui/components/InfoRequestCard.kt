package pt.isec.amov.cityfix.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.InfoRequest
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

@Composable
fun InfoRequestCard(
    request: InfoRequest,
    answered: Boolean,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.info_request_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(
                if (request.byMunicipality) R.string.info_request_by_municipality else R.string.info_request_by_citizen,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (request.question.isNotBlank()) {
            Text(text = request.question, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = stringResource(if (answered) R.string.info_request_answered else R.string.info_request_waiting),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoRequestCardWaitingPreview() {
    CityFixTheme {
        InfoRequestCard(
            request = InfoRequest("Which bench, the left or the right one?", 0L, true, TicketStatus.IN_PROGRESS),
            answered = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoRequestCardAnsweredPreview() {
    CityFixTheme {
        InfoRequestCard(
            request = InfoRequest("When will the team come?", 0L, false, TicketStatus.OPEN),
            answered = true,
        )
    }
}

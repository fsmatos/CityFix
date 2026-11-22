package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

@Composable
fun CoarseLocationMap(location: Coordinates, modifier: Modifier = Modifier) {
    val mapDescription = stringResource(R.string.cd_ticket_map)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ticket_map_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (location == Coordinates()) {
            Text(
                text = stringResource(R.string.ticket_map_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TicketMap(
                location = location,
                exact = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .semantics { contentDescription = mapDescription },
            )
            Text(
                text = stringResource(R.string.ticket_map_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CoarseLocationMapPreview() {
    CityFixTheme {
        CoarseLocationMap(Coordinates(40.21, -8.42), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CoarseLocationMapUnavailablePreview() {
    CityFixTheme {
        CoarseLocationMap(Coordinates(), modifier = Modifier.padding(16.dp))
    }
}

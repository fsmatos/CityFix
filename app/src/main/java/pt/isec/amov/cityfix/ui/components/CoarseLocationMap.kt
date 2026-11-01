package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/**
 * "Approximate location" section for [location] (already rounded for anonymous users, decision 0006).
 * Gate 1 of F-005 chose a static placeholder (no map library): a location icon and a caption,
 * never coordinate text. Shows "Location not available" for the unset (0, 0) (decision 0003).
 */
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics { contentDescription = mapDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

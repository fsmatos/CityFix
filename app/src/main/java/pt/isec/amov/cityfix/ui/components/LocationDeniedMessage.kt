package pt.isec.amov.cityfix.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * Shown by the gate and the report form when location was denied, with the caller's [message]. "Try again" asks for the
 * permission again; "Open settings" is for "don't ask again", when Android shows no dialog.
 */
@Composable
fun LocationDeniedMessage(
    @StringRes message: Int,
    onTryAgainClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        PrimaryButton(onClick = onTryAgainClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.location_try_again))
        }
        FilledTonalButton(onClick = onOpenSettingsClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.location_open_settings))
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun LocationDeniedMessagePreview() {
    CityFixTheme {
        LocationDeniedMessage(
            message = R.string.location_permission_denied,
            onTryAgainClick = {},
            onOpenSettingsClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

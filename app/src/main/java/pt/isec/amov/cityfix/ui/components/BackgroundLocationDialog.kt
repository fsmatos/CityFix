package pt.isec.amov.cityfix.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

@Composable
fun BackgroundLocationDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.background_location_title)) },
        text = { Text(stringResource(R.string.background_location_text)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.background_location_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.background_location_not_now))
            }
        },
    )
}

@Preview
@Composable
private fun BackgroundLocationDialogPreview() {
    CityFixTheme { BackgroundLocationDialog(onContinue = {}, onDismiss = {}) }
}

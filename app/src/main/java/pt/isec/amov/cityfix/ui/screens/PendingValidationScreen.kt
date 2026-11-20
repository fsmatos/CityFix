package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * Home of a citizen whose registration is still pending (brief I.2.A, D-009 Q2). They have the
 * anonymous features only, so the button opens the public dashboard. When an operator validates
 * the account, the session changes and the redirect moves them to the citizen home. Stateless.
 */
@Composable
fun PendingValidationScreen(
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        titleRes = R.string.pending_title,
        messageRes = R.string.pending_message,
        modifier = modifier,
    ) {
        PrimaryButton(onClick = onBrowseClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pending_browse))
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun PendingValidationPreview() {
    CityFixTheme {
        PendingValidationScreen(onBrowseClick = {})
    }
}

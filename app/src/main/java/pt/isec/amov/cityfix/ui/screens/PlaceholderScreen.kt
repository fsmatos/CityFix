package pt.isec.amov.cityfix.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    FormColumn(modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        actions()
    }
}

@Composable
fun AccountProblemScreen(
    canRetry: Boolean,
    onRetry: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        titleRes = R.string.top_bar_title_account_problem,
        messageRes = if (canRetry) R.string.account_problem_unavailable else R.string.account_problem_missing,
        modifier = modifier,
    ) {
        if (canRetry) {
            PrimaryButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_problem_retry))
            }
        }
        FilledTonalButton(onClick = onLogOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.menu_log_out))
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun PlaceholderPreview() {
    CityFixTheme {
        PlaceholderScreen(titleRes = R.string.pending_title, messageRes = R.string.pending_message)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun AccountUnavailablePreview() {
    CityFixTheme {
        AccountProblemScreen(canRetry = true, onRetry = {}, onLogOut = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountMissingPreview() {
    CityFixTheme {
        AccountProblemScreen(canRetry = false, onRetry = {}, onLogOut = {})
    }
}

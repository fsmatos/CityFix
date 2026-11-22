package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.ForgotPasswordUiState
import pt.isec.amov.cityfix.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSend = viewModel::send,
        modifier = modifier,
    )
}

@Composable
fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormColumn(modifier = modifier) {
        Text(
            text = stringResource(R.string.forgot_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        EmailField(
            value = state.email,
            errorRes = state.emailError,
            enabled = !state.isBusy,
            onValueChange = onEmailChange,
            imeAction = ImeAction.Send,
            onImeAction = onSend,
        )
        FormMessage(state.error, isError = true)
        FormMessage(if (state.sent) R.string.forgot_sent else null, isError = false)
        PrimaryButton(
            onClick = onSend,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.forgot_submit))
        }
    }
}

@Composable
private fun ForgotPreview(state: ForgotPasswordUiState) {
    CityFixTheme {
        ForgotPasswordContent(state = state, onEmailChange = {}, onSend = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ForgotEmptyPreview() {
    ForgotPreview(ForgotPasswordUiState())
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ForgotSentPreview() {
    ForgotPreview(ForgotPasswordUiState(email = "ana@example.com", sent = true))
}

@Preview(showBackground = true)
@Composable
private fun ForgotErrorPreview() {
    ForgotPreview(ForgotPasswordUiState(email = "ana@example.com", error = R.string.auth_error_network))
}

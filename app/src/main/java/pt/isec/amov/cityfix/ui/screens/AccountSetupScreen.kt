package pt.isec.amov.cityfix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.AccountSetupUiState
import pt.isec.amov.cityfix.viewmodel.AccountSetupViewModel
import pt.isec.amov.cityfix.viewmodel.SetupStep

@Composable
fun AccountSetupScreen(
    viewModel: AccountSetupViewModel,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    // The setup screen is the only back stack entry: system Back on the code step goes back to
    // the phone step instead of leaving the app.
    BackHandler(enabled = state.step == SetupStep.CODE, onBack = viewModel::changeNumber)
    AccountSetupContent(
        state = state,
        onResendEmail = viewModel::sendVerificationEmail,
        onCheckVerified = viewModel::checkVerified,
        onPhoneChange = viewModel::onPhoneChange,
        onSendCode = { activity?.let(viewModel::sendCode) },
        onCodeChange = viewModel::onCodeChange,
        onConfirmCode = viewModel::confirmCode,
        onChangeNumber = viewModel::changeNumber,
        onLogOut = onLogOut,
        modifier = modifier,
    )
}

@Composable
fun AccountSetupContent(
    state: AccountSetupUiState,
    onResendEmail: () -> Unit,
    onCheckVerified: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onConfirmCode: () -> Unit,
    onChangeNumber: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.step) {
        SetupStep.VERIFY_EMAIL -> FormColumn(modifier = modifier) {
            SetupHeading(R.string.setup_verify_title)
            Text(
                text = stringResource(R.string.setup_verify_intro, state.email),
                style = MaterialTheme.typography.bodyLarge,
            )
            FormMessage(state.error, isError = true)
            FormMessage(state.notice, isError = false)
            PrimaryButton(onClick = onCheckVerified, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_verified_button))
            }
            TextButton(onClick = onResendEmail, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_resend_email))
            }
            FilledTonalButton(onClick = onLogOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.menu_log_out))
            }
        }
        SetupStep.PHONE -> FormColumn(modifier = modifier) {
            SetupHeading(R.string.setup_phone_title)
            Text(
                text = stringResource(R.string.setup_phone_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            val phoneError = state.phoneError?.let { stringResource(it) }
            OutlinedTextField(
                value = state.phoneDigits,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.setup_phone_label)) },
                prefix = { Text(stringResource(R.string.setup_phone_prefix)) },
                singleLine = true,
                enabled = !state.isBusy,
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendCode() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { if (phoneError != null) error(phoneError) },
            )
            FormMessage(state.error, isError = true)
            PrimaryButton(onClick = onSendCode, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_send_code))
            }
            FilledTonalButton(onClick = onLogOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.menu_log_out))
            }
        }
        SetupStep.CODE -> MfaCodeContent(
            phone = stringResource(R.string.setup_phone_display, state.phoneDigits),
            code = state.code,
            errorRes = state.codeError,
            messageRes = state.error,
            noticeRes = state.notice,
            isBusy = state.isBusy,
            backLabel = R.string.setup_change_number,
            // changeNumber() ignores taps while busy, so the button is disabled then too.
            backEnabled = !state.isBusy,
            onCodeChange = onCodeChange,
            onConfirm = onConfirmCode,
            onResend = onSendCode,
            onBack = onChangeNumber,
            modifier = modifier,
        )
    }
}

@Composable
private fun SetupHeading(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SetupPreview(state: AccountSetupUiState) {
    CityFixTheme {
        AccountSetupContent(
            state = state,
            onResendEmail = {},
            onCheckVerified = {},
            onPhoneChange = {},
            onSendCode = {},
            onCodeChange = {},
            onConfirmCode = {},
            onChangeNumber = {},
            onLogOut = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun SetupVerifyPreview() {
    SetupPreview(
        AccountSetupUiState(email = "ana@example.com", notice = R.string.setup_verification_sent),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun SetupNotVerifiedPreview() {
    SetupPreview(AccountSetupUiState(email = "ana@example.com", error = R.string.setup_not_verified))
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun SetupPhonePreview() {
    SetupPreview(
        AccountSetupUiState(
            step = SetupStep.PHONE,
            phoneDigits = "8123",
            phoneError = R.string.setup_error_phone,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun SetupCodePreview() {
    SetupPreview(AccountSetupUiState(step = SetupStep.CODE, phoneDigits = "9xxxxxxxx", code = "1234"))
}

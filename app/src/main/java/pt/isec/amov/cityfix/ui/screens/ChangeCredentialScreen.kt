package pt.isec.amov.cityfix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.PasswordField
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.CredentialChange
import pt.isec.amov.cityfix.viewmodel.CredentialChangeUiState
import pt.isec.amov.cityfix.viewmodel.CredentialChangeViewModel
import pt.isec.amov.cityfix.viewmodel.CredentialStep

/**
 * Stateful Change password / Change e-mail screen (D-011): the form, then the SMS code of the
 * re-authentication ([MfaCodeContent], as on Login), then the result. Firebase needs the Activity
 * for the reCAPTCHA fallback when it sends the SMS, so it is read here and handed to the
 * ViewModel call, never stored. [onDone] leaves the screen ("Back to profile").
 */
@Composable
fun ChangeCredentialScreen(
    viewModel: CredentialChangeViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    if (state.step == CredentialStep.CODE) {
        // The first SMS of the step goes out once; codeRequested stops a resend on rotation.
        LaunchedEffect(state.step) {
            if (!state.codeRequested && activity != null) viewModel.sendCode(activity)
        }
        BackHandler(onBack = viewModel::backToForm)
        MfaCodeContent(
            phone = state.maskedPhone,
            code = state.code,
            errorRes = state.codeError,
            messageRes = state.error,
            noticeRes = state.notice,
            isBusy = state.isBusy,
            backLabel = R.string.credential_back,
            // Always: the ViewModel drops the answer of a send started before Back.
            backEnabled = true,
            onCodeChange = viewModel::onCodeChange,
            onConfirm = viewModel::confirmCode,
            onResend = { activity?.let(viewModel::sendCode) },
            onBack = viewModel::backToForm,
            modifier = modifier,
        )
        return
    }

    ChangeCredentialContent(
        state = state,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onToggleShowPassword = viewModel::onToggleShowPassword,
        onSubmit = viewModel::submit,
        onDone = onDone,
        modifier = modifier,
    )
}

/** The form step or the result (the code step is [MfaCodeContent]). Stateless and previewable. */
@Composable
fun ChangeCredentialContent(
    state: CredentialChangeUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormColumn(modifier = modifier) {
        if (state.step == CredentialStep.DONE) {
            Text(
                text = stringResource(R.string.credential_password_changed),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.credential_back_to_profile))
            }
            return@FormColumn
        }

        val enabled = !state.isBusy
        PasswordField(
            value = state.currentPassword,
            onValueChange = onCurrentPasswordChange,
            labelRes = R.string.credential_current_password_label,
            error = state.currentPasswordError?.let { stringResource(it) },
            hint = null,
            showPassword = state.showPassword,
            enabled = enabled,
            onToggleShowPassword = onToggleShowPassword,
            imeAction = ImeAction.Next,
        )
        PasswordField(
            value = state.newPassword,
            onValueChange = onNewPasswordChange,
            labelRes = R.string.credential_new_password_label,
            error = state.newPasswordError?.let { stringResource(it) },
            hint = stringResource(R.string.register_password_hint),
            showPassword = state.showPassword,
            enabled = enabled,
            onToggleShowPassword = null,
            imeAction = ImeAction.Next,
        )
        PasswordField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            labelRes = R.string.register_confirm_password_label,
            error = state.confirmError?.let { stringResource(it) },
            hint = null,
            showPassword = state.showPassword,
            enabled = enabled,
            onToggleShowPassword = null,
            imeAction = ImeAction.Done,
        )

        FormMessage(state.error, isError = true)
        val progressDescription = stringResource(R.string.cd_credential_progress)
        Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            if (state.isBusy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .semantics { contentDescription = progressDescription },
                )
            } else {
                Text(stringResource(R.string.credential_continue))
            }
        }
    }
}

@Composable
private fun ChangeCredentialPreview(state: CredentialChangeUiState) {
    CityFixTheme {
        ChangeCredentialContent(
            state = state,
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onToggleShowPassword = {},
            onSubmit = {},
            onDone = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ChangePasswordFormPreview() {
    ChangeCredentialPreview(CredentialChangeUiState(change = CredentialChange.PASSWORD))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ChangePasswordErrorsPreview() {
    ChangeCredentialPreview(
        CredentialChangeUiState(
            change = CredentialChange.PASSWORD,
            currentPassword = "wrong",
            newPassword = "short",
            confirmPassword = "other",
            currentPasswordError = R.string.credential_error_current_wrong,
            newPasswordError = R.string.register_error_password_short,
            confirmError = R.string.register_error_confirm,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ChangePasswordDonePreview() {
    ChangeCredentialPreview(
        CredentialChangeUiState(change = CredentialChange.PASSWORD, step = CredentialStep.DONE),
    )
}

@Preview(showBackground = true)
@Composable
private fun ChangeCredentialCodePreview() {
    CityFixTheme {
        MfaCodeContent(
            phone = "+3519******78",
            code = "",
            errorRes = null,
            messageRes = null,
            noticeRes = R.string.mfa_code_sent,
            isBusy = false,
            backLabel = R.string.credential_back,
            backEnabled = true,
            onCodeChange = {},
            onConfirm = {},
            onResend = {},
            onBack = {},
        )
    }
}

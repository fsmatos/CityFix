package pt.isec.amov.cityfix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.AccountTextField
import pt.isec.amov.cityfix.ui.components.PasswordField
import pt.isec.amov.cityfix.ui.components.PrimaryButton
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
        onNewEmailChange = viewModel::onNewEmailChange,
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
    onNewEmailChange: (String) -> Unit,
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
                text = when (state.change) {
                    CredentialChange.EMAIL ->
                        stringResource(R.string.credential_email_link_sent, state.newEmail.trim().lowercase())
                    CredentialChange.PASSWORD -> stringResource(R.string.credential_password_changed)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            PrimaryButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.credential_back_to_profile))
            }
            return@FormColumn
        }

        val enabled = !state.isBusy
        val isEmail = state.change == CredentialChange.EMAIL
        if (isEmail) {
            AccountTextField(
                value = state.currentEmail,
                onValueChange = {},
                labelRes = R.string.credential_current_email_label,
                error = null,
                enabled = true,
                keyboardType = KeyboardType.Email,
                readOnly = true,
            )
            if (state.isOperator) OperatorDomainLine(state)
            // The domain error names the council's domain.
            val emailError = state.newEmailError?.let {
                if (it == R.string.register_error_email_domain) {
                    stringResource(it, state.operatorDomain.orEmpty())
                } else {
                    stringResource(it)
                }
            }
            AccountTextField(
                value = state.newEmail,
                onValueChange = onNewEmailChange,
                labelRes = R.string.credential_new_email_label,
                error = emailError,
                enabled = enabled,
                keyboardType = KeyboardType.Email,
                autoCorrect = false,
            )
        }
        PasswordField(
            value = state.currentPassword,
            onValueChange = onCurrentPasswordChange,
            labelRes = R.string.credential_current_password_label,
            error = state.currentPasswordError?.let { stringResource(it) },
            hint = null,
            showPassword = state.showPassword,
            enabled = enabled,
            onToggleShowPassword = onToggleShowPassword,
            imeAction = if (isEmail) ImeAction.Done else ImeAction.Next,
        )
        if (!isEmail) NewPasswordFields(state, enabled, onNewPasswordChange, onConfirmPasswordChange)
        FormMessage(state.error, isError = true)
        val progressDescription = stringResource(R.string.cd_credential_progress)
        PrimaryButton(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
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

/** PASSWORD: the new password (the D-010 rule) and its confirmation. */
@Composable
private fun NewPasswordFields(
    state: CredentialChangeUiState,
    enabled: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
) {
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
}

/** EMAIL, operators: the council's domain the new e-mail must use, or why it cannot be checked. */
@Composable
private fun OperatorDomainLine(state: CredentialChangeUiState) {
    when {
        state.isLoadingDomain -> FormMessage(R.string.register_operator_domain_loading, isError = false)
        state.operatorDomain == null -> FormMessage(R.string.register_error_operator_unavailable, isError = true)
        else -> Text(
            text = stringResource(R.string.register_operator_domain_hint, state.operatorDomain),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ChangeCredentialPreview(state: CredentialChangeUiState) {
    CityFixTheme {
        ChangeCredentialContent(
            state = state,
            onCurrentPasswordChange = {},
            onNewEmailChange = {},
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
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ChangeEmailFormPreview() {
    ChangeCredentialPreview(
        CredentialChangeUiState(
            change = CredentialChange.EMAIL,
            currentEmail = "ana@example.com",
            newEmail = "ana@example",
            newEmailError = R.string.login_error_email_invalid,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ChangeEmailOperatorPreview() {
    ChangeCredentialPreview(
        CredentialChangeUiState(
            change = CredentialChange.EMAIL,
            currentEmail = "rui@cm-example.pt",
            newEmail = "rui@example.com",
            isOperator = true,
            operatorDomain = "cm-example.pt",
            newEmailError = R.string.register_error_email_domain,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ChangeEmailDonePreview() {
    ChangeCredentialPreview(
        CredentialChangeUiState(
            change = CredentialChange.EMAIL,
            step = CredentialStep.DONE,
            newEmail = "ana.nova@example.com",
        ),
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

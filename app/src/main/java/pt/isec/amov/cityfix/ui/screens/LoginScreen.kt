package pt.isec.amov.cityfix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.LoginStep
import pt.isec.amov.cityfix.viewmodel.LoginUiState
import pt.isec.amov.cityfix.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onForgotPassword: (email: String) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    if (state.step == LoginStep.SECOND_FACTOR) {
        // The first SMS of the step goes out once; codeRequested stops a resend on rotation.
        LaunchedEffect(state.step) {
            if (!state.codeRequested && activity != null) viewModel.sendCode(activity)
        }
        BackHandler(onBack = viewModel::backToCredentials)
        MfaCodeContent(
            phone = state.maskedPhone,
            code = state.code,
            errorRes = state.codeError,
            messageRes = state.error,
            noticeRes = state.notice,
            isBusy = state.isBusy,
            backLabel = R.string.mfa_back,
            // Always: LoginViewModel drops the answer of a send started before Back (R3: a lost
            // onCodeSent would otherwise keep the user waiting up to 60 s).
            backEnabled = true,
            onCodeChange = viewModel::onCodeChange,
            onConfirm = viewModel::confirmCode,
            onResend = { activity?.let(viewModel::sendCode) },
            onBack = viewModel::backToCredentials,
            modifier = modifier,
        )
        return
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onToggleShowPassword = viewModel::onToggleShowPassword,
        onSignIn = viewModel::signIn,
        onForgotPassword = { onForgotPassword(state.email.trim()) },
        onCreateAccount = onCreateAccount,
        modifier = modifier,
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormColumn(modifier = modifier) {
        EmailField(
            value = state.email,
            errorRes = state.emailError,
            enabled = !state.isBusy,
            onValueChange = onEmailChange,
            imeAction = ImeAction.Next,
        )
        val passwordError = state.passwordError?.let { stringResource(it) }
        val toggleDescription = stringResource(
            if (state.showPassword) R.string.cd_hide_password else R.string.cd_show_password,
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label)) },
            singleLine = true,
            enabled = !state.isBusy,
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = if (state.showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSignIn() }),
            // material-icons-core has no eye icon, so the toggle is a text button.
            trailingIcon = {
                TextButton(
                    onClick = onToggleShowPassword,
                    modifier = Modifier.semantics { contentDescription = toggleDescription },
                ) {
                    Text(
                        stringResource(
                            if (state.showPassword) R.string.login_hide_password else R.string.login_show_password,
                        ),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (passwordError != null) error(passwordError) },
        )
        FormMessage(state.error, isError = true)
        PrimaryButton(
            onClick = onSignIn,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.login_submit))
        }
        TextButton(
            onClick = onForgotPassword,
            enabled = !state.isBusy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.login_forgot_password))
        }
        TextButton(
            onClick = onCreateAccount,
            enabled = !state.isBusy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.register_create_account))
        }
    }
}

@Composable
internal fun FormColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(16.dp)
                // Room to scroll above the floating island where it is shown (D-016).
                .padding(bottom = LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun EmailField(
    value: String,
    @StringRes errorRes: Int?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    onImeAction: (() -> Unit)? = null,
) {
    val error = errorRes?.let { stringResource(it) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.login_email_label)) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = imeAction),
        // Without a handler the keyboard keeps its default (Next moves focus to the next field).
        keyboardActions = if (onImeAction == null) {
            KeyboardActions.Default
        } else {
            KeyboardActions(onAny = { onImeAction() })
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) },
    )
}

@Composable
internal fun FormMessage(@StringRes messageRes: Int?, isError: Boolean) {
    if (messageRes == null) return
    Text(
        text = stringResource(messageRes),
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun LoginPreview(state: LoginUiState) {
    CityFixTheme {
        LoginContent(
            state = state,
            onEmailChange = {},
            onPasswordChange = {},
            onToggleShowPassword = {},
            onSignIn = {},
            onForgotPassword = {},
            onCreateAccount = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun LoginEmptyPreview() {
    LoginPreview(LoginUiState())
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun LoginErrorPreview() {
    LoginPreview(
        LoginUiState(
            email = "ana@example.com",
            password = "secret",
            error = R.string.auth_error_invalid_credentials,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginFieldErrorsPreview() {
    LoginPreview(
        LoginUiState(
            emailError = R.string.login_error_email_blank,
            passwordError = R.string.login_error_password_blank,
        ),
    )
}

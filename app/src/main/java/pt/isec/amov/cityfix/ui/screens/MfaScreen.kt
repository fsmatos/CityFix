package pt.isec.amov.cityfix.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * The SMS code step, shared by the login (second factor) and the account setup (enrolment).
 * Stateless: [phone] is where the code went, [backLabel] names the way back ("Back" on login,
 * "Change number" on setup); [backEnabled] says whether it works while a call is running. [errorRes] belongs to the code field; [messageRes] / [noticeRes]
 * are the call's error and neutral notice.
 */
@Composable
fun MfaCodeContent(
    phone: String,
    code: String,
    @StringRes errorRes: Int?,
    @StringRes messageRes: Int?,
    @StringRes noticeRes: Int?,
    isBusy: Boolean,
    @StringRes backLabel: Int,
    backEnabled: Boolean,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormColumn(modifier = modifier) {
        Text(
            text = stringResource(R.string.mfa_code_intro, phone),
            style = MaterialTheme.typography.bodyLarge,
        )
        val error = errorRes?.let { stringResource(it) }
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.mfa_code_label)) },
            singleLine = true,
            enabled = !isBusy,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    // Lets the autofill service offer the code from the SMS.
                    contentType = ContentType.SmsOtpCode
                    if (error != null) error(error)
                },
        )
        FormMessage(messageRes, isError = true)
        FormMessage(noticeRes, isError = false)
        Button(onClick = onConfirm, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mfa_confirm))
        }
        TextButton(onClick = onResend, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mfa_resend))
        }
        TextButton(onClick = onBack, enabled = backEnabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(backLabel))
        }
    }
}

@Composable
private fun MfaPreview(@StringRes errorRes: Int? = null, @StringRes noticeRes: Int? = null) {
    CityFixTheme {
        MfaCodeContent(
            phone = "+3519******78",
            code = "12345",
            errorRes = errorRes,
            messageRes = null,
            noticeRes = noticeRes,
            isBusy = false,
            backLabel = R.string.mfa_back,
            backEnabled = true,
            onCodeChange = {},
            onConfirm = {},
            onResend = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun MfaCodePreview() {
    MfaPreview(noticeRes = R.string.mfa_code_sent)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun MfaWrongCodePreview() {
    MfaPreview(errorRes = R.string.auth_error_wrong_code)
}

package pt.isec.amov.cityfix.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.RegistrationRole
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.ui.components.AccountTextField
import pt.isec.amov.cityfix.ui.components.ImpactDetectionRow
import pt.isec.amov.cityfix.ui.components.LocationDeniedMessage
import pt.isec.amov.cityfix.ui.components.PasswordField
import pt.isec.amov.cityfix.ui.components.PrimaryButton
import pt.isec.amov.cityfix.ui.components.ProfilePhotoPicker
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.LOCATION_PERMISSIONS
import pt.isec.amov.cityfix.utils.formatCoordinate
import pt.isec.amov.cityfix.utils.hasLocationPermission
import pt.isec.amov.cityfix.utils.openAppSettings
import pt.isec.amov.cityfix.viewmodel.RegisterUiState
import pt.isec.amov.cityfix.viewmodel.RegisterViewModel
import java.io.File

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    accountSignedIn: Boolean,
    onBusyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The pending camera file survives rotation and process death while the camera app is open.
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(
        remember { PickVisualMedia() },
    ) { uri -> if (uri != null) viewModel.onPhotoPicked(uri) }
    val takePhoto = rememberLauncherForActivityResult(
        remember { TakePicture() },
    ) { success ->
        val path = tempCameraPath
        tempCameraPath = null
        if (path != null) {
            val file = File(path)
            if (success && file.length() > 0) {
                viewModel.onPhotoCaptured(path)
            } else {
                // Cancelled (silently) or an empty "success" (with the notice).
                file.delete()
                if (success) viewModel.onCameraUnavailable()
            }
        }
    }

    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Approximate location is enough for the residence.
        viewModel.onLocationPermissionResult(hasLocationPermission(context))
    }

    val currentOnBusyChange by rememberUpdatedState(onBusyChange)
    val holdRedirect = state.isRegistering || (state.failed && accountSignedIn)
    LaunchedEffect(holdRedirect) {
        currentOnBusyChange(holdRedirect)
    }
    DisposableEffect(Unit) {
        onDispose { currentOnBusyChange(false) }
    }
    // Back would clear the ViewModel mid-registration; the result is only a moment away.
    BackHandler(enabled = state.isRegistering) {}

    RegisterContent(
        state = state,
        onRoleSelected = viewModel::onRoleSelected,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onNifChange = viewModel::onNifChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onToggleShowPassword = viewModel::onToggleShowPassword,
        onImpactDetectionChange = viewModel::onImpactDetectionChange,
        onUseMyLocation = {
            if (hasLocationPermission(context)) {
                viewModel.refreshLocation()
            } else {
                askLocation.launch(LOCATION_PERMISSIONS)
            }
        },
        onLocationTryAgain = { askLocation.launch(LOCATION_PERMISSIONS) },
        onOpenSettings = { openAppSettings(context) },
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        onTakePhoto = {
            // A second tap before the camera opens would overwrite the path and orphan its file.
            if (tempCameraPath == null) {
                val (file, uri) = FileUtils.createTempImageFile(context)
                tempCameraPath = file.absolutePath
                if (!launchCamera(takePhoto, uri)) {
                    tempCameraPath = null
                    file.delete()
                    viewModel.onCameraUnavailable()
                }
            }
        },
        onRemovePhoto = viewModel::onRemovePhoto,
        onSubmit = viewModel::submit,
        modifier = modifier,
    )
}

private fun launchCamera(
    takePhoto: ManagedActivityResultLauncher<Uri, Boolean>,
    uri: Uri,
): Boolean = try {
    takePhoto.launch(uri)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    onRoleSelected: (RegistrationRole) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNifChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onImpactDetectionChange: (Boolean) -> Unit,
    onUseMyLocation: () -> Unit,
    onLocationTryAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isRegistering && !state.registered
    val isOperator = state.role == RegistrationRole.OPERATOR

    FormColumn(modifier = modifier) {
        Text(
            text = stringResource(R.string.register_role_label),
            style = MaterialTheme.typography.titleSmall,
        )
        RoleSelector(role = state.role, enabled = enabled, onRoleSelected = onRoleSelected)

        AccountTextField(
            value = state.name,
            onValueChange = onNameChange,
            labelRes = R.string.register_name_label,
            error = state.nameError?.let { stringResource(it) },
            enabled = enabled,
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words,
        )

        if (isOperator) {
            OperatorDomainHint(state)
        }
        // The domain error names the council's domain.
        val emailError = state.emailError?.let {
            if (it == R.string.register_error_email_domain) {
                stringResource(it, state.operatorDomain.orEmpty())
            } else {
                stringResource(it)
            }
        }
        AccountTextField(
            value = state.email,
            onValueChange = onEmailChange,
            labelRes = R.string.login_email_label,
            error = emailError,
            enabled = enabled,
            keyboardType = KeyboardType.Email,
            autoCorrect = false,
        )
        AccountTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            labelRes = R.string.register_phone_label,
            error = state.phoneError?.let { stringResource(it) },
            enabled = enabled,
            keyboardType = KeyboardType.Phone,
            prefix = stringResource(R.string.setup_phone_prefix),
        )
        AccountTextField(
            value = state.nif,
            onValueChange = onNifChange,
            labelRes = R.string.register_nif_label,
            error = state.nifError?.let { stringResource(it) },
            enabled = enabled,
            keyboardType = KeyboardType.Number,
        )
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            labelRes = R.string.register_password_label,
            error = state.passwordError?.let { stringResource(it) },
            hint = stringResource(R.string.register_password_hint),
            showPassword = state.showPassword,
            enabled = enabled,
            onToggleShowPassword = onToggleShowPassword,
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
            imeAction = if (isOperator) ImeAction.Done else ImeAction.Next,
        )

        if (!isOperator) {
            CitizenLocation(
                state = state,
                enabled = enabled,
                onUseMyLocation = onUseMyLocation,
                onTryAgain = onLocationTryAgain,
                onOpenSettings = onOpenSettings,
            )
            ImpactDetectionRow(
                checked = state.impactDetection,
                enabled = enabled,
                onCheckedChange = onImpactDetectionChange,
            )
        }

        ProfilePhotoPicker(
            photo = state.photoPath?.let { File(it) },
            isCopying = state.isCopyingPhoto,
            enabled = enabled && !state.isCopyingPhoto,
            showRemove = state.photoPath != null,
            onPickPhoto = onPickPhoto,
            onTakePhoto = onTakePhoto,
            onRemovePhoto = onRemovePhoto,
        )
        FormMessage(state.photoNotice, isError = true)

        FormMessage(state.error, isError = true)
        if (state.showErrorSummary) {
            FormMessage(R.string.register_error_summary, isError = true)
        }
        val progressDescription = stringResource(R.string.cd_register_progress)
        PrimaryButton(
            onClick = onSubmit,
            enabled = enabled && !state.isCopyingPhoto,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isRegistering) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .semantics { contentDescription = progressDescription },
                )
            } else {
                Text(stringResource(R.string.register_submit))
            }
        }
    }
}

@Composable
private fun RoleSelector(
    role: RegistrationRole,
    enabled: Boolean,
    onRoleSelected: (RegistrationRole) -> Unit,
) {
    val roles = RegistrationRole.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        roles.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == role,
                onClick = { onRoleSelected(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = roles.size),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        when (option) {
                            RegistrationRole.CITIZEN -> R.string.register_role_citizen
                            RegistrationRole.OPERATOR -> R.string.register_role_operator
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun OperatorDomainHint(state: RegisterUiState) {
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
private fun CitizenLocation(
    state: RegisterUiState,
    enabled: Boolean,
    onUseMyLocation: () -> Unit,
    onTryAgain: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.register_location_label),
            style = MaterialTheme.typography.titleSmall,
        )
        if (state.locationPermissionDenied) {
            LocationDeniedMessage(
                message = R.string.register_location_denied,
                onTryAgainClick = onTryAgain,
                onOpenSettingsClick = onOpenSettings,
            )
            return@Column
        }
        Text(
            text = when {
                state.isLocating -> stringResource(R.string.report_location_locating)
                state.location != null ->
                    stringResource(
                        R.string.report_location_value,
                        formatCoordinate(state.location.latitude),
                        formatCoordinate(state.location.longitude),
                    )
                else -> stringResource(R.string.register_location_not_set)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        FormMessage(state.locationError, isError = true)
        FilledTonalButton(
            onClick = onUseMyLocation,
            enabled = enabled && !state.isLocating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.register_location_use))
        }
    }
}

@Composable
private fun RegisterPreview(state: RegisterUiState) {
    CityFixTheme {
        RegisterContent(
            state = state,
            onRoleSelected = {},
            onNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onNifChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onToggleShowPassword = {},
            onImpactDetectionChange = {},
            onUseMyLocation = {},
            onLocationTryAgain = {},
            onOpenSettings = {},
            onPickPhoto = {},
            onTakePhoto = {},
            onRemovePhoto = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun RegisterCitizenEmptyPreview() {
    RegisterPreview(RegisterUiState(isLoadingDomain = false))
}

@Preview(showBackground = true)
@Composable
private fun RegisterCitizenLocatedPreview() {
    RegisterPreview(
        RegisterUiState(
            name = "Ana Silva",
            location = Coordinates(40.21, -8.419),
            impactDetection = true,
            isLoadingDomain = false,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun RegisterCitizenLocationDeniedPreview() {
    RegisterPreview(RegisterUiState(locationPermissionDenied = true, isLoadingDomain = false))
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun RegisterCitizenErrorsPreview() {
    RegisterPreview(
        RegisterUiState(
            name = "A",
            email = "ana@",
            phone = "123",
            nif = "123456780",
            password = "short",
            confirmPassword = "other",
            nameError = R.string.register_error_name,
            emailError = R.string.login_error_email_invalid,
            phoneError = R.string.register_error_phone,
            nifError = R.string.register_error_nif,
            passwordError = R.string.register_error_password_short,
            confirmError = R.string.register_error_confirm,
            locationError = R.string.register_error_location,
            showErrorSummary = true,
            isLoadingDomain = false,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun RegisterOperatorPreview() {
    RegisterPreview(
        RegisterUiState(
            role = RegistrationRole.OPERATOR,
            name = "Ana Operadora",
            email = "ana@example.com",
            phone = "239000000",
            nif = "123456789",
            operatorDomain = "cm-example.pt",
            isLoadingDomain = false,
            emailError = R.string.register_error_email_domain,
        ),
    )
}

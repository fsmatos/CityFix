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
import androidx.compose.foundation.shape.CircleShape
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.RegistrationRole
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.ui.components.LocationDeniedMessage
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.LOCATION_PERMISSIONS
import pt.isec.amov.cityfix.utils.hasLocationPermission
import pt.isec.amov.cityfix.utils.openAppSettings
import pt.isec.amov.cityfix.viewmodel.RegisterUiState
import pt.isec.amov.cityfix.viewmodel.RegisterViewModel
import java.io.File

/**
 * Stateful Register screen: collects [viewModel] and shows [RegisterContent].
 *
 * Firebase signs a new user in as soon as the account is created, which would trigger
 * CityFixApp's redirect before the NIF transaction ends. [onBusyChange] asks CityFixApp to hold
 * the redirect while Submit runs and, after a failure, until [accountSignedIn] is false again
 * (the deleted account has reached the session), so the form keeps its error. Back is ignored
 * while Submit runs; leaving the screen in any other way releases the hold (safety net).
 *
 * The location permission is asked when the citizen taps "Use my location", not before. The
 * photo is picked as on the report form (F-007): PickVisualMedia, or TakePicture into a
 * FileProvider file (no CAMERA permission). Every launcher sits here, above the content.
 */
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

/** False when there is no camera app, or Android refuses the launch (as on the report form). */
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

/** Registration form for both roles (brief I.2.A, I.3.A). Stateless and previewable. */
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

        RegisterTextField(
            value = state.name,
            onValueChange = onNameChange,
            labelRes = R.string.register_name_label,
            error = state.nameError?.let { stringResource(it) },
            enabled = enabled,
            keyboardType = KeyboardType.Text,
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
        RegisterTextField(
            value = state.email,
            onValueChange = onEmailChange,
            labelRes = R.string.login_email_label,
            error = emailError,
            enabled = enabled,
            keyboardType = KeyboardType.Email,
        )
        RegisterTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            labelRes = R.string.register_phone_label,
            error = state.phoneError?.let { stringResource(it) },
            enabled = enabled,
            keyboardType = KeyboardType.Phone,
            prefix = stringResource(R.string.setup_phone_prefix),
        )
        RegisterTextField(
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

        ProfilePhoto(
            state = state,
            enabled = enabled && !state.isCopyingPhoto,
            onPickPhoto = onPickPhoto,
            onTakePhoto = onTakePhoto,
            onRemovePhoto = onRemovePhoto,
        )

        FormMessage(state.error, isError = true)
        if (state.showErrorSummary) {
            FormMessage(R.string.register_error_summary, isError = true)
        }
        val progressDescription = stringResource(R.string.cd_register_progress)
        Button(
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

/** Citizen or operator, as two segmented buttons (48dp high by default). */
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

/** The council's domain the operator e-mail must use, or why operator registration is closed. */
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

/** The citizen's residence (brief I.2.A georeferencing): the fix with 5 decimals and "Use my location". */
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
                    stringResource(R.string.report_location_value, state.location.latitude, state.location.longitude)
                else -> stringResource(R.string.register_location_not_set)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        FormMessage(state.locationError, isError = true)
        OutlinedButton(
            onClick = onUseMyLocation,
            enabled = enabled && !state.isLocating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.register_location_use))
        }
    }
}

/** The optional photo (brief I.2.A, I.3.A): a preview of the local copy, Gallery, Camera and Remove. */
@Composable
private fun ProfilePhoto(
    state: RegisterUiState,
    enabled: Boolean,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.register_photo_label),
            style = MaterialTheme.typography.titleSmall,
        )
        if (state.photoPath != null) {
            AsyncImage(
                model = File(state.photoPath),
                contentDescription = stringResource(R.string.cd_register_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
            )
        }
        if (state.isCopyingPhoto) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickPhoto, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.register_photo_gallery))
            }
            OutlinedButton(onClick = onTakePhoto, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.register_photo_camera))
            }
        }
        if (state.photoPath != null) {
            TextButton(
                onClick = onRemovePhoto,
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.register_photo_remove))
            }
        }
        FormMessage(state.photoNotice, isError = true)
    }
}

/** The impact detection choice (brief I.2.A). The whole row toggles, so the target is large. */
@Composable
private fun ImpactDetectionRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.register_impact_label),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.register_impact_description),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // The row handles the click and announces the label, so the switch itself is passive.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    prefix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        prefix = prefix?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) },
    )
}

/** A password field; [onToggleShowPassword] null means no Show/Hide button (the confirmation). */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    error: String?,
    hint: String?,
    showPassword: Boolean,
    enabled: Boolean,
    onToggleShowPassword: (() -> Unit)?,
    imeAction: ImeAction,
) {
    val toggleDescription = stringResource(
        if (showPassword) R.string.cd_hide_password else R.string.cd_show_password,
    )
    val supporting = error ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        // material-icons-core has no eye icon, so the toggle is a text button (as on Login).
        trailingIcon = onToggleShowPassword?.let { onToggle ->
            {
                TextButton(
                    onClick = onToggle,
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = toggleDescription },
                ) {
                    Text(
                        stringResource(if (showPassword) R.string.login_hide_password else R.string.login_show_password),
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) },
    )
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

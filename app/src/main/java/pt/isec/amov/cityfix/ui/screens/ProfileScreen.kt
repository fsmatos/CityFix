package pt.isec.amov.cityfix.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.ImageRequest
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.ui.components.AccountTextField
import pt.isec.amov.cityfix.ui.components.ImpactDetectionRow
import pt.isec.amov.cityfix.ui.components.ProfilePhotoPicker
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.viewmodel.ProfileUiState
import pt.isec.amov.cityfix.viewmodel.ProfileViewModel
import java.io.File

/**
 * Stateful Profile screen (D-011): collects [viewModel] and shows [ProfileContent]. The photo is
 * picked as on Register (PickVisualMedia, or TakePicture into a FileProvider file); the launchers
 * sit here, above the content. [onChangeEmail] and [onChangePassword] open their routes;
 * [onLogOut] is CityFixApp's logOut helper.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Picks up an e-mail changed through the link; the ViewModel runs it once per visit.
    LaunchedEffect(Unit) {
        viewModel.reloadAccount()
    }

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

    ProfileContent(
        state = state,
        onPhoneChange = viewModel::onPhoneChange,
        onImpactDetectionChange = viewModel::onImpactDetectionChange,
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        onTakePhoto = {
            // A second tap before the camera opens would overwrite the path and orphan its file.
            if (tempCameraPath == null) {
                val (file, uri) = FileUtils.createTempImageFile(context)
                tempCameraPath = file.absolutePath
                if (!launchProfileCamera(takePhoto, uri)) {
                    tempCameraPath = null
                    file.delete()
                    viewModel.onCameraUnavailable()
                }
            }
        },
        onRemovePhoto = viewModel::onRemoveNewPhoto,
        onSave = viewModel::save,
        onChangeEmail = onChangeEmail,
        onChangePassword = onChangePassword,
        onLogOut = onLogOut,
        modifier = modifier,
    )
}

/** False when there is no camera app, or Android refuses the launch (as on Register). */
private fun launchProfileCamera(
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

/**
 * The signed-in user's profile (brief I.2.C, I.3.C). Name, NIF, role, e-mail and, for citizens,
 * the registration status and georeferencing are read-only; phone, photo and (citizen) impact
 * detection are editable and saved together. Stateless and previewable.
 */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onPhoneChange: (String) -> Unit,
    onImpactDetectionChange: (Boolean) -> Unit,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    FormColumn(modifier = modifier) {
        if (profile == null) {
            if (state.unavailable) {
                FormMessage(R.string.profile_unavailable, isError = true)
            } else {
                val loadingDescription = stringResource(R.string.cd_profile_loading)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription },
                    )
                }
            }
            OutlinedButton(onClick = onLogOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_log_out))
            }
            return@FormColumn
        }

        // A photo picked now, else the one saved in this visit (until its upload ends), else the
        // stored one. The stored photo's cache key includes photoUpdatedAt: an overwrite may keep
        // the same URL, and Coil would otherwise show the cached old image (D-011 R4).
        val context = LocalContext.current
        val storedPhoto = profile.photo
        val storedPhotoModel: Any? = remember(storedPhoto, state.photoUpdatedAt) {
            if (storedPhoto == null || state.photoUpdatedAt == null) {
                storedPhoto
            } else {
                val key = "$storedPhoto#${state.photoUpdatedAt}"
                ImageRequest.Builder(context)
                    .data(storedPhoto)
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .build()
            }
        }
        val localPhoto = state.newPhotoPath ?: state.savedPhotoPath
        ProfilePhotoPicker(
            photo = localPhoto?.let { File(it) } ?: storedPhotoModel,
            isCopying = state.isCopyingPhoto,
            enabled = !state.isCopyingPhoto,
            showRemove = state.newPhotoPath != null,
            onPickPhoto = onPickPhoto,
            onTakePhoto = onTakePhoto,
            onRemovePhoto = onRemovePhoto,
        )
        FormMessage(state.photoNotice, isError = true)
        if (state.photoUploadPending) {
            FormMessage(R.string.profile_photo_uploading, isError = false)
        }

        ReadOnlyField(value = profile.name, labelRes = R.string.register_name_label)
        ReadOnlyField(value = profile.nif, labelRes = R.string.register_nif_label)
        ReadOnlyField(
            value = stringResource(
                if (profile is Citizen) R.string.register_role_citizen else R.string.register_role_operator,
            ),
            labelRes = R.string.profile_role_label,
        )
        if (profile is Citizen) {
            ReadOnlyField(
                value = stringResource(
                    when (profile.registrationStatus) {
                        RegistrationStatus.PENDING -> R.string.profile_status_pending
                        RegistrationStatus.ACTIVE -> R.string.profile_status_active
                    },
                ),
                labelRes = R.string.profile_status_label,
            )
            ReadOnlyField(
                value = stringResource(
                    R.string.report_location_value,
                    profile.georeferencing.latitude,
                    profile.georeferencing.longitude,
                ),
                labelRes = R.string.register_location_label,
            )
        }
        ReadOnlyField(value = state.email, labelRes = R.string.login_email_label)

        AccountTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            labelRes = R.string.register_phone_label,
            error = state.phoneError?.let { stringResource(it) },
            enabled = true,
            keyboardType = KeyboardType.Phone,
            prefix = stringResource(R.string.setup_phone_prefix),
            supportingHint = stringResource(R.string.profile_phone_hint),
        )
        if (profile is Citizen) {
            ImpactDetectionRow(
                checked = state.impactDetection,
                enabled = true,
                onCheckedChange = onImpactDetectionChange,
            )
        }

        FormMessage(state.notice, isError = false)
        Button(
            onClick = onSave,
            enabled = state.hasChanges && !state.isCopyingPhoto,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.profile_save))
        }
        OutlinedButton(onClick = onChangeEmail, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_change_email))
        }
        OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_change_password))
        }
        OutlinedButton(onClick = onLogOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_log_out))
        }
    }
}

/** A value the user cannot change: the text field reads "read only" and opens no keyboard. */
@Composable
private fun ReadOnlyField(value: String, @StringRes labelRes: Int) {
    AccountTextField(
        value = value,
        onValueChange = {},
        labelRes = labelRes,
        error = null,
        enabled = true,
        keyboardType = KeyboardType.Text,
        readOnly = true,
    )
}

private val previewCitizen = Citizen(
    id = "c1",
    name = "Ana Silva",
    nif = "123456789",
    email = "ana@example.com",
    phone = "912345678",
    georeferencing = Coordinates(40.21, -8.419),
    registrationStatus = RegistrationStatus.ACTIVE,
    impactDetection = true,
)

@Composable
private fun ProfilePreview(state: ProfileUiState) {
    CityFixTheme {
        ProfileContent(
            state = state,
            onPhoneChange = {},
            onImpactDetectionChange = {},
            onPickPhoto = {},
            onTakePhoto = {},
            onRemovePhoto = {},
            onSave = {},
            onChangeEmail = {},
            onChangePassword = {},
            onLogOut = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ProfileCitizenPreview() {
    ProfilePreview(
        ProfileUiState(
            profile = previewCitizen,
            email = previewCitizen.email,
            phone = previewCitizen.phone,
            impactDetection = true,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfilePendingCitizenPreview() {
    val pending = previewCitizen.copy(registrationStatus = RegistrationStatus.PENDING, impactDetection = false)
    ProfilePreview(
        ProfileUiState(profile = pending, email = pending.email, phone = pending.phone),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ProfileOperatorPreview() {
    val operator = Operator(
        id = "o1",
        name = "Rui Operador",
        nif = "987654321",
        email = "rui@cm-example.pt",
        phone = "239000000",
    )
    ProfilePreview(
        ProfileUiState(profile = operator, email = operator.email, phone = operator.phone),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfilePhoneErrorPreview() {
    ProfilePreview(
        ProfileUiState(
            profile = previewCitizen,
            email = previewCitizen.email,
            phone = "123",
            impactDetection = true,
            phoneError = R.string.register_error_phone,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileLoadingPreview() {
    ProfilePreview(ProfileUiState())
}

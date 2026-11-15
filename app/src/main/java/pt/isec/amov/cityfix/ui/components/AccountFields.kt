package pt.isec.amov.cityfix.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R

/*
 * Form fields shared by Register (D-010) and Profile (D-011): one text field, the password
 * field, the impact detection row and the photo block.
 */

/**
 * A single-line text field with its error (also announced by TalkBack). [readOnly] shows a value
 * that cannot be edited (TalkBack reads it as read only). [supportingHint] shows when there is
 * no error.
 */
@Composable
internal fun AccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    prefix: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    autoCorrect: Boolean = true,
    readOnly: Boolean = false,
    supportingHint: String? = null,
) {
    val supporting = error ?: supportingHint
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        prefix = prefix?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        readOnly = readOnly,
        isError = error != null,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            autoCorrectEnabled = autoCorrect,
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) },
    )
}

/** A password field; [onToggleShowPassword] null means no Show/Hide button (the confirmation). */
@Composable
internal fun PasswordField(
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

/** The impact detection choice (brief I.2.A). The whole row toggles, so the target is large. */
@Composable
internal fun ImpactDetectionRow(
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

/**
 * The optional photo (brief I.2.A, I.3.A): a preview, Gallery, Camera and Remove.
 * [photo] is the Coil model: a File (local pick) or the https photoPath; null = no photo.
 * [showRemove] shows Remove (only for a photo picked on this screen). The caller shows any
 * notice below it.
 */
@Composable
internal fun ProfilePhotoPicker(
    photo: Any?,
    isCopying: Boolean,
    enabled: Boolean,
    showRemove: Boolean,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.register_photo_label),
            style = MaterialTheme.typography.titleSmall,
        )
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.cd_register_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
            )
        }
        if (isCopying) {
            val copyingDescription = stringResource(R.string.cd_register_photo_copying)
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = copyingDescription },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickPhoto, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.register_photo_gallery))
            }
            OutlinedButton(onClick = onTakePhoto, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.register_photo_camera))
            }
        }
        if (showRemove) {
            TextButton(
                onClick = onRemovePhoto,
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.register_photo_remove))
            }
        }
    }
}

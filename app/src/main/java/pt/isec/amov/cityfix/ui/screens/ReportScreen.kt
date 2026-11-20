package pt.isec.amov.cityfix.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.ReportOutcome
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.ui.components.AttachmentGallery
import pt.isec.amov.cityfix.ui.components.LocationDeniedMessage
import pt.isec.amov.cityfix.ui.components.situationLabelRes
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.LOCATION_PERMISSIONS
import pt.isec.amov.cityfix.utils.hasPreciseLocationPermission
import pt.isec.amov.cityfix.utils.openAppSettings
import pt.isec.amov.cityfix.viewmodel.MAX_REPORT_ATTACHMENTS
import pt.isec.amov.cityfix.viewmodel.ReportUiState
import pt.isec.amov.cityfix.viewmodel.ReportViewModel
import java.io.File

/** Label for a ticket scope ("Alcance" in the brief). */
fun scopeLabelRes(scope: Scope): Int = when (scope) {
    Scope.PUBLIC -> R.string.scope_public
    Scope.PRIVATE -> R.string.scope_private
}

/**
 * Stateful half of the report form (brief I.2.E): collects [viewModel], which validates and saves
 * the ticket. [onSubmitted] runs once the report is saved, with what it did; a refused save keeps the form open and
 * shows an error. Location was requested at app start (MainActivity); here the form only checks
 * the answer when it opens, and "Try again" asks again.
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onSubmitted: (ReportOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The pending photo survives rotation and process death while the camera app is open.
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    // Launchers stay here, above the orientation branch: their saved key is position-based, so
    // inside a pane a rotation with the picker or camera open would lose the result.
    val pickSeveral = rememberLauncherForActivityResult(
        remember { PickMultipleVisualMedia() },
    ) { uris -> viewModel.onMediaPicked(uris) }
    val pickOne = rememberLauncherForActivityResult(
        remember { PickVisualMedia() },
    ) { uri -> viewModel.onMediaPicked(listOfNotNull(uri)) }
    val takePhoto = rememberLauncherForActivityResult(
        remember { TakePicture() },
    ) { success ->
        val path = tempCameraPath
        tempCameraPath = null
        if (path != null) attachCaptured(viewModel, path, success)
    }
    val askLocation = rememberLauncherForActivityResult(
        remember { RequestMultiplePermissions() },
    ) { permissions ->
        // Precise only: an "Approximate" fix (~2 km off) would group unrelated reports.
        viewModel.onLocationPermissionResult(permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true)
    }

    // Only while there is no fix, so a rotation does not read the location again.
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.location == null) {
            viewModel.onLocationPermissionResult(hasPreciseLocationPermission(context))
        }
    }

    ReportContent(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onSituationSelected = viewModel::onSituationSelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onScopeSelected = viewModel::onScopeSelected,
        onAddFromGalleryClick = {
            val remaining = state.remainingAttachments
            // maxItems must be above 1, so the last free slot uses the single picker.
            if (remaining > 1) {
                pickSeveral.launch(
                    PickVisualMediaRequest(PickVisualMedia.ImageAndVideo, maxItems = remaining),
                )
            } else {
                pickOne.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
            }
        },
        onTakePhotoClick = {
            // A second tap before the camera opens would overwrite the path and orphan its file.
            // The takePhoto callback and a failed launch both clear it, so this cannot stick.
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
        onRemoveAttachment = viewModel::onRemoveAttachment,
        onLocationTryAgainClick = { askLocation.launch(LOCATION_PERMISSIONS) },
        onOpenSettingsClick = { openAppSettings(context) },
        onRefreshLocationClick = viewModel::refreshLocation,
        onSubmitClick = { viewModel.submit(onSuccess = onSubmitted) },
        modifier = modifier,
    )
}

/** False when there is no camera app, or Android refuses the launch. */
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

/**
 * A cancelled capture deletes its file silently; an empty "success" also shows the notice. The
 * limit is checked here too: a pick that started with the camera can take the last slot first.
 */
private fun attachCaptured(viewModel: ReportViewModel, path: String, success: Boolean) {
    val file = File(path)
    val hasPhoto = success && file.length() > 0
    when {
        hasPhoto && viewModel.uiState.value.remainingAttachments > 0 ->
            viewModel.onPhotosAttached(listOf(path))
        hasPhoto -> {
            file.delete()
            viewModel.onAttachmentLimitReached()
        }
        else -> {
            file.delete()
            if (success) viewModel.onCameraUnavailable()
        }
    }
}

/** Stateless half of the report form: draws [state]; previews target this. */
@Composable
fun ReportContent(
    state: ReportUiState,
    onCategorySelected: (Category) -> Unit,
    onSituationSelected: (Situation) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onScopeSelected: (Scope) -> Unit,
    onAddFromGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onLocationTryAgainClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onRefreshLocationClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Hoisted so the scroll position survives rotation; portrait uses leftScroll.
    val leftScroll = rememberScrollState()
    val rightScroll = rememberScrollState()

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ChoicesPane(
                state = state,
                onCategorySelected = onCategorySelected,
                onSituationSelected = onSituationSelected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .imePadding()
                    .verticalScroll(leftScroll)
                    .padding(bottom = LocalBottomBarPadding.current),
            )
            DetailsPane(
                state = state,
                onDescriptionChange = onDescriptionChange,
                onScopeSelected = onScopeSelected,
                onAddFromGalleryClick = onAddFromGalleryClick,
                onTakePhotoClick = onTakePhotoClick,
                onRemoveAttachment = onRemoveAttachment,
                onLocationTryAgainClick = onLocationTryAgainClick,
                onOpenSettingsClick = onOpenSettingsClick,
                onRefreshLocationClick = onRefreshLocationClick,
                onSubmitClick = onSubmitClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .imePadding()
                    .verticalScroll(rightScroll)
                    .padding(bottom = LocalBottomBarPadding.current),
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(leftScroll)
                .padding(16.dp)
                // Room to scroll above the floating island (D-016).
                .padding(bottom = LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChoicesPane(
                state = state,
                onCategorySelected = onCategorySelected,
                onSituationSelected = onSituationSelected,
            )
            DetailsPane(
                state = state,
                onDescriptionChange = onDescriptionChange,
                onScopeSelected = onScopeSelected,
                onAddFromGalleryClick = onAddFromGalleryClick,
                onTakePhotoClick = onTakePhotoClick,
                onRemoveAttachment = onRemoveAttachment,
                onLocationTryAgainClick = onLocationTryAgainClick,
                onOpenSettingsClick = onOpenSettingsClick,
                onRefreshLocationClick = onRefreshLocationClick,
                onSubmitClick = onSubmitClick,
            )
        }
    }
}

/** Required note, category and situation: the whole form in portrait, the left pane in landscape. */
@Composable
private fun ChoicesPane(
    state: ReportUiState,
    onCategorySelected: (Category) -> Unit,
    onSituationSelected: (Situation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.report_required_note),
            style = MaterialTheme.typography.bodyMedium,
        )
        CategoryDropdown(
            categories = state.categories,
            selectedName = state.categoryName,
            error = state.categoryError,
            onSelect = onCategorySelected,
        )
        RadioGroupSection(
            title = stringResource(R.string.report_situation_title),
            options = Situation.entries,
            selected = state.situation,
            label = { stringResource(situationLabelRes(it)) },
            error = state.situationError,
            onSelect = onSituationSelected,
        )
    }
}

/** Description, attachments, location, visibility, errors and Submit: the right pane in landscape. */
@Composable
private fun DetailsPane(
    state: ReportUiState,
    onDescriptionChange: (String) -> Unit,
    onScopeSelected: (Scope) -> Unit,
    onAddFromGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onLocationTryAgainClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onRefreshLocationClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DescriptionField(
            value = state.description,
            error = state.descriptionError,
            onValueChange = onDescriptionChange,
        )
        AttachmentsSection(
            state = state,
            onAddFromGalleryClick = onAddFromGalleryClick,
            onTakePhotoClick = onTakePhotoClick,
            onRemoveAttachment = onRemoveAttachment,
        )
        LocationSection(
            state = state,
            onTryAgainClick = onLocationTryAgainClick,
            onOpenSettingsClick = onOpenSettingsClick,
            onRefreshClick = onRefreshLocationClick,
        )
        RadioGroupSection(
            title = stringResource(R.string.report_scope_title),
            options = Scope.entries,
            selected = state.scope,
            label = { stringResource(scopeLabelRes(it)) },
            error = state.scopeError,
            onSelect = onScopeSelected,
        )
        if (state.showErrorSummary) {
            ErrorText(R.string.report_error_summary)
        }
        if (state.submitError != null) {
            ErrorText(state.submitError)
        }
        Button(
            onClick = onSubmitClick,
            enabled = !state.submitted && !state.isCopyingAttachments,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.report_submit))
        }
    }
}

/** Read-only dropdown of the active categories; disabled with a message when there are none. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedName: String,
    @StringRes error: Int?,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val hasCategories = categories.isNotEmpty()
    // With no categories the reason wins over "Choose a category." (I.3.E).
    val supportingRes = if (hasCategories) error else R.string.report_no_categories
    val supportingText = supportingRes?.let { stringResource(it) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (hasCategories) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = hasCategories,
            label = { Text(stringResource(R.string.report_category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = supportingText?.let { { Text(it) } },
            // The disabled default (38% alpha) is too faint for the no-categories message.
            colors = OutlinedTextFieldDefaults.colors(
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = hasCategories)
                .fillMaxWidth()
                .semantics { if (error != null && supportingText != null) error(supportingText) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Titled single-choice group; each row is one 48dp radio item for TalkBack. */
@Composable
private fun <T> RadioGroupSection(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    @StringRes error: Int?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorText = error?.let { stringResource(it) }

    Column(
        modifier = modifier
            .selectableGroup()
            .semantics { if (errorText != null) error(errorText) },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(option) },
                        role = Role.RadioButton,
                    ),
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DescriptionField(
    value: String,
    @StringRes error: Int?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorText = error?.let { stringResource(it) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.report_description_label)) },
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        minLines = 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        // TalkBack reads the real message instead of the default "Invalid input".
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (errorText != null) error(errorText) },
    )
}

/** Photos and videos (brief I.2.E): thumbnails, the ways to add one, error and notice. */
@Composable
private fun AttachmentsSection(
    state: ReportUiState,
    onAddFromGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.ticket_media_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.report_attachments_hint, MAX_REPORT_ATTACHMENTS),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.attachments.isNotEmpty()) {
            AttachmentGallery(
                attachments = state.attachments,
                onRemove = onRemoveAttachment,
                removeEnabled = !state.submitted,
            )
        }
        OutlinedButton(
            onClick = onAddFromGalleryClick,
            enabled = state.canAddAttachment,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.report_attachments_gallery))
        }
        OutlinedButton(
            onClick = onTakePhotoClick,
            enabled = state.canAddAttachment,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.report_attachments_camera))
        }
        if (state.attachmentsError != null) {
            ErrorText(state.attachmentsError)
        }
        if (state.attachmentsNotice != null) {
            ErrorText(state.attachmentsNotice)
        }
    }
}

/**
 * The reporter's GPS fix: their own position, so it is shown with 5 decimals (about 1 m).
 * Denied location shows the denied message instead.
 */
@Composable
private fun LocationSection(
    state: ReportUiState,
    onTryAgainClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.report_location_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (state.locationPermissionDenied) {
            LocationDeniedMessage(
                message = R.string.report_location_precise_needed,
                onTryAgainClick = onTryAgainClick,
                onOpenSettingsClick = onOpenSettingsClick,
            )
        } else {
            if (state.isLocating) {
                Text(
                    text = stringResource(R.string.report_location_locating),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.location != null) {
                Text(
                    text = stringResource(
                        R.string.report_location_value,
                        state.location.latitude,
                        state.location.longitude,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            OutlinedButton(
                onClick = onRefreshClick,
                enabled = !state.isLocating && !state.submitted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.report_location_refresh))
            }
            if (state.locationError != null) {
                ErrorText(state.locationError)
            }
        }
    }
}

/** Form-level error, announced by TalkBack when it appears. */
@Composable
private fun ErrorText(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private val previewCategories = listOf(
    Category(id = "pavement", name = "Pavement"),
    Category(id = "lighting", name = "Public lighting"),
    Category(id = "furniture", name = "Urban furniture"),
)

private val previewAttachments = listOf(
    Attachment(id = "IMG_1.jpg", file = "IMG_1.jpg"),
    Attachment(id = "VID_2.mp4", type = AttachmentType.VIDEO, file = "VID_2.mp4"),
    Attachment(id = "IMG_3.jpg", file = "IMG_3.jpg"),
)

private val previewLocation = Coordinates(40.19250, -8.41150)

private val emptyState = ReportUiState(categories = previewCategories, location = previewLocation)

private val filledState = emptyState.copy(
    categoryId = "lighting",
    categoryName = "Public lighting",
    situation = Situation.BREAKDOWN,
    description = "The street lamp in front of number 12 is off every night.",
    scope = Scope.PUBLIC,
    attachments = previewAttachments.take(1),
)

private val errorState = emptyState.copy(
    location = null,
    locationError = R.string.report_error_location,
    categoryError = R.string.report_error_category,
    situationError = R.string.report_error_situation,
    descriptionError = R.string.report_error_description,
    scopeError = R.string.report_error_scope,
    attachmentsError = R.string.report_error_attachments,
    showErrorSummary = true,
)

private val noCategoriesState = ReportUiState(location = previewLocation)

private val locationDeniedState = emptyState.copy(
    location = null,
    locationPermissionDenied = true,
)

private val locatingState = emptyState.copy(location = null, isLocating = true)

private val noCategoriesSubmittedState = noCategoriesState.copy(
    categoryError = R.string.report_error_category,
    showErrorSummary = true,
)

private val submitFailedState = filledState.copy(submitError = R.string.report_error_not_created)

private val withAttachmentsState = filledState.copy(attachments = previewAttachments.take(2))

private val fullAttachmentsState = filledState.copy(attachments = previewAttachments)

private val attachmentsNoticeState = fullAttachmentsState.copy(
    attachmentsNotice = R.string.report_attachments_limit,
)

@Composable
private fun ReportPreview(state: ReportUiState) {
    CityFixTheme {
        ReportContent(
            state = state,
            onCategorySelected = {},
            onSituationSelected = {},
            onDescriptionChange = {},
            onScopeSelected = {},
            onAddFromGalleryClick = {},
            onTakePhotoClick = {},
            onRemoveAttachment = {},
            onLocationTryAgainClick = {},
            onOpenSettingsClick = {},
            onRefreshLocationClick = {},
            onSubmitClick = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ReportEmptyPreview() {
    ReportPreview(emptyState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ReportFilledPreview() {
    ReportPreview(filledState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun ReportErrorPreview() {
    ReportPreview(errorState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ReportNoCategoriesPreview() {
    ReportPreview(noCategoriesState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ReportNoCategoriesSubmittedPreview() {
    ReportPreview(noCategoriesSubmittedState)
}

@Preview(showBackground = true)
@Composable
private fun ReportSubmitFailedPreview() {
    ReportPreview(submitFailedState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ReportWithAttachmentsPreview() {
    ReportPreview(withAttachmentsState)
}

/** The narrow landscape spec gives the grid 2 columns, so the third item needs the grid's scroll. */
@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,orientation=landscape")
@Composable
private fun ReportAttachmentsFullPreview() {
    ReportPreview(fullAttachmentsState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ReportAttachmentsNoticePreview() {
    ReportPreview(attachmentsNoticeState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ReportLocationDeniedPreview() {
    ReportPreview(locationDeniedState)
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun ReportLocatingPreview() {
    ReportPreview(locatingState)
}

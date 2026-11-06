package pt.isec.amov.cityfix.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.ui.components.situationLabelRes
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.ReportSubmission
import pt.isec.amov.cityfix.viewmodel.ReportUiState
import pt.isec.amov.cityfix.viewmodel.ReportViewModel

/** Label for a ticket scope ("Alcance" in the brief). */
fun scopeLabelRes(scope: Scope): Int = when (scope) {
    Scope.PUBLIC -> R.string.scope_public
    Scope.PRIVATE -> R.string.scope_private
}

/**
 * Stateful half of the report form (brief I.2.E): collects [viewModel] and calls [onSubmit] with
 * valid data. [onSubmit] returns true if the ticket was created; false keeps the form open and
 * shows an error.
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onSubmit: (ReportSubmission) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContent(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onSituationSelected = viewModel::onSituationSelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onScopeSelected = viewModel::onScopeSelected,
        onSubmitClick = { viewModel.submit()?.let { viewModel.onSubmitResult(onSubmit(it)) } },
        modifier = modifier,
    )
}

/** Stateless half of the report form: draws [state]; previews target this. */
@Composable
fun ReportContent(
    state: ReportUiState,
    onCategorySelected: (Category) -> Unit,
    onSituationSelected: (Situation) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onScopeSelected: (Scope) -> Unit,
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
                    .verticalScroll(leftScroll),
            )
            DetailsPane(
                state = state,
                onDescriptionChange = onDescriptionChange,
                onScopeSelected = onScopeSelected,
                onSubmitClick = onSubmitClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .imePadding()
                    .verticalScroll(rightScroll),
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(leftScroll)
                .padding(16.dp),
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

/** Description, media note, visibility, errors and Submit: the right pane in landscape. */
@Composable
private fun DetailsPane(
    state: ReportUiState,
    onDescriptionChange: (String) -> Unit,
    onScopeSelected: (Scope) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DescriptionField(
            value = state.description,
            error = state.descriptionError,
            onValueChange = onDescriptionChange,
        )
        MediaPendingSection()
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
            enabled = !state.submitted,
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

/**
 * Photos and videos (brief I.2.E): a note only, nothing to tap. F-007 replaces it with the
 * picker/camera row and previews, in the same place, and adds the "at least one" rule.
 */
@Composable
private fun MediaPendingSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.ticket_media_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.report_media_pending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

private val emptyState = ReportUiState(categories = previewCategories)

private val filledState = emptyState.copy(
    categoryId = "lighting",
    categoryName = "Public lighting",
    situation = Situation.BREAKDOWN,
    description = "The street lamp in front of number 12 is off every night.",
    scope = Scope.PUBLIC,
)

private val errorState = emptyState.copy(
    categoryError = R.string.report_error_category,
    situationError = R.string.report_error_situation,
    descriptionError = R.string.report_error_description,
    scopeError = R.string.report_error_scope,
    showErrorSummary = true,
)

private val noCategoriesState = ReportUiState()

private val noCategoriesSubmittedState = noCategoriesState.copy(
    categoryError = R.string.report_error_category,
    showErrorSummary = true,
)

private val submitFailedState = filledState.copy(submitError = R.string.report_error_not_created)

@Composable
private fun ReportPreview(state: ReportUiState) {
    CityFixTheme {
        ReportContent(
            state = state,
            onCategorySelected = {},
            onSituationSelected = {},
            onDescriptionChange = {},
            onScopeSelected = {},
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

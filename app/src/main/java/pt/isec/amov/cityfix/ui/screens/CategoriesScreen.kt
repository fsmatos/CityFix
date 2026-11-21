package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.ui.components.EmptyState
import pt.isec.amov.cityfix.ui.components.LoadingState
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.CategoriesUiState
import pt.isec.amov.cityfix.viewmodel.CategoriesViewModel
import pt.isec.amov.cityfix.viewmodel.CategoryEditor
import pt.isec.amov.cityfix.viewmodel.MAX_CATEGORY_NAME_LENGTH

/** Categories (brief I.3.E), the stateful half: collects the ViewModel state and wires the events. */
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    // D-020: hoisted so MainActivity can scroll it to the top when the tab is tapped again.
    listState: LazyListState = rememberLazyListState(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesContent(
        state = state,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        onActiveChange = viewModel::onActiveChange,
        onEditorNameChange = viewModel::onEditorNameChange,
        onEditorSave = viewModel::onEditorSave,
        onEditorDismiss = viewModel::onEditorDismiss,
        listState = listState,
    )
}

/**
 * Every category, active and inactive: name ("Inactive" under it when off), Edit and an active
 * switch. The FAB adds one; Add and Edit open the same name dialog. Same single column in both
 * orientations, width capped and centred so rows are not stretched in landscape.
 */
@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    onAddClick: () -> Unit,
    onEditClick: (Category) -> Unit,
    onActiveChange: (Category, Boolean) -> Unit,
    onEditorNameChange: (String) -> Unit,
    onEditorSave: () -> Unit,
    onEditorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState(descriptionRes = R.string.cd_categories_loading)
            state.categories.isEmpty() -> EmptyState(messageRes = R.string.categories_empty)
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    state = listState,
                    // Bottom padding so the last row clears the FAB and the floating island.
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 88.dp + LocalBottomBarPadding.current,
                    ),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onEditClick = { onEditClick(category) },
                            onActiveChange = { active -> onActiveChange(category, active) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        // Hidden until the first snapshot: the duplicate check needs the loaded list.
        if (!state.isLoading) {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.categories_add)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // Above the floating island (D-016).
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = maxOf(16.dp, LocalBottomBarPadding.current)),
            )
        }
    }

    state.editor?.let { editor ->
        CategoryEditorDialog(
            editor = editor,
            onNameChange = onEditorNameChange,
            onSave = onEditorSave,
            onDismiss = onEditorDismiss,
        )
    }
}

/** Name and status, an Edit button and the active switch (the switch area is one toggle). */
@Composable
private fun CategoryRow(
    category: Category,
    onEditClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    val activeDescription = stringResource(R.string.cd_category_active, category.name)
    val stateText = stringResource(
        if (category.active) R.string.categories_active else R.string.categories_inactive,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
            if (!category.active) {
                Text(
                    text = stringResource(R.string.categories_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.cd_category_edit, category.name),
            )
        }
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .toggleable(
                    value = category.active,
                    role = Role.Switch,
                    onValueChange = onActiveChange,
                )
                .semantics {
                    contentDescription = activeDescription
                    stateDescription = stateText
                }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // onCheckedChange = null: the Box above handles the toggle, so the whole area reacts.
            Switch(checked = category.active, onCheckedChange = null)
        }
    }
}

/** Add or edit one name: Save checks it in the ViewModel and shows the error under the field. */
@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditor,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val error = editor.error
    val errorText = when {
        error == null -> null
        error == R.string.categories_error_too_long -> stringResource(error, MAX_CATEGORY_NAME_LENGTH)
        else -> stringResource(error)
    }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editor.id == null) R.string.categories_dialog_new else R.string.categories_dialog_edit,
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.categories_name_label)) },
                singleLine = true,
                isError = errorText != null,
                supportingText = errorText?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { if (errorText != null) error(errorText) },
            )
            // Here, not before AlertDialog: the text slot is composed in the dialog's own window,
            // so only from inside it is the field (and its FocusRequester) surely attached.
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.categories_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.categories_cancel))
            }
        },
    )
}

/** Synthetic preview data only. */
private val previewCategories = listOf(
    Category(id = "k1", name = "Pavement", active = true),
    Category(id = "k2", name = "Public lighting", active = true),
    Category(id = "k3", name = "Urban furniture", active = false),
)

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
private fun CategoriesContentPreview() {
    CityFixTheme {
        CategoriesContent(
            state = CategoriesUiState(categories = previewCategories, isLoading = false),
            onAddClick = {},
            onEditClick = {},
            onActiveChange = { _, _ -> },
            onEditorNameChange = {},
            onEditorSave = {},
            onEditorDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CategoriesContentEmptyPreview() {
    CityFixTheme {
        CategoriesContent(
            state = CategoriesUiState(isLoading = false),
            onAddClick = {},
            onEditClick = {},
            onActiveChange = { _, _ -> },
            onEditorNameChange = {},
            onEditorSave = {},
            onEditorDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CategoriesContentNewDialogPreview() {
    CityFixTheme {
        CategoriesContent(
            state = CategoriesUiState(
                categories = previewCategories,
                isLoading = false,
                editor = CategoryEditor(),
            ),
            onAddClick = {},
            onEditClick = {},
            onActiveChange = { _, _ -> },
            onEditorNameChange = {},
            onEditorSave = {},
            onEditorDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CategoriesContentErrorDialogPreview() {
    CityFixTheme {
        CategoriesContent(
            state = CategoriesUiState(
                categories = previewCategories,
                isLoading = false,
                editor = CategoryEditor(id = "k1", name = "public lighting", error = R.string.categories_error_duplicate),
            ),
            onAddClick = {},
            onEditClick = {},
            onActiveChange = { _, _ -> },
            onEditorNameChange = {},
            onEditorSave = {},
            onEditorDismiss = {},
        )
    }
}

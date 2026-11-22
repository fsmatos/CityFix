package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.model.Category

class CategoriesViewModel(private val repository: CategoriesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allCategories().collect { list ->
                _uiState.update { it.copy(categories = list, isLoading = false) }
            }
        }
    }

    fun onAddClick() {
        // The duplicate check needs the loaded list (the screen also hides Add until then).
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(editor = CategoryEditor()) }
    }

    fun onEditClick(category: Category) {
        _uiState.update { it.copy(editor = CategoryEditor(id = category.id, name = category.name)) }
    }

    fun onEditorNameChange(name: String) {
        _uiState.update { state ->
            state.copy(editor = state.editor?.copy(name = name, error = null))
        }
    }

    fun onEditorDismiss() {
        _uiState.update { it.copy(editor = null) }
    }

    fun onEditorSave() {
        val state = _uiState.value
        val editor = state.editor ?: return
        val name = editor.name.trim()
        val error = when {
            name.isEmpty() -> R.string.categories_error_blank
            name.length > MAX_CATEGORY_NAME_LENGTH -> R.string.categories_error_too_long
            state.categories.any { it.id != editor.id && it.name.trim().equals(name, ignoreCase = true) } ->
                R.string.categories_error_duplicate
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(editor = editor.copy(error = error)) }
            return
        }
        if (editor.id == null) {
            repository.addCategory(name)
        } else {
            // Only when the name changed: saving the same name writes nothing.
            val stored = state.categories.firstOrNull { it.id == editor.id }?.name
            if (stored != name) repository.renameCategory(editor.id, name)
        }
        _uiState.update { it.copy(editor = null) }
    }

    fun onActiveChange(category: Category, active: Boolean) {
        repository.setActive(category.id, active)
    }
}

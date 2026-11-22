package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Category

const val MAX_CATEGORY_NAME_LENGTH = 40

data class CategoryEditor(
    val id: String? = null,
    val name: String = "",
    @StringRes val error: Int? = null,
)

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val editor: CategoryEditor? = null,
)

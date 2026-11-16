package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Category

/** The longest category name the operator may save (D-012 Q7). */
const val MAX_CATEGORY_NAME_LENGTH = 40

/** The add/edit category dialog. */
data class CategoryEditor(
    /** null = a new category. */
    val id: String? = null,
    val name: String = "",
    /** Why Save was refused, shown under the field; null = no error. */
    @StringRes val error: Int? = null,
)

/** The Categories screen (brief I.3.E, D-012). */
data class CategoriesUiState(
    /** Every category, active and inactive, sorted by name. */
    val categories: List<Category> = emptyList(),
    /** True until the first snapshot arrives. */
    val isLoading: Boolean = true,
    /** The add/edit dialog, null when closed. */
    val editor: CategoryEditor? = null,
)

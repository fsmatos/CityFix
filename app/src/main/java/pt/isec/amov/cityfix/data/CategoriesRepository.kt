package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Category

interface CategoriesRepository {
    fun allCategories(): Flow<List<Category>>

    fun addCategory(name: String)

    fun renameCategory(id: String, name: String)

    fun setActive(id: String, active: Boolean)
}

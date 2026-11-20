package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Category

/**
 * The municipality's categories for the operator (brief I.3.E, D-012). The report form reads
 * TicketsRepository.activeCategories() instead. [FirestoreCategoriesRepository] is the
 * implementation.
 */
interface CategoriesRepository {
    /** Every category, active and inactive, sorted by name with Collator, live. */
    fun allCategories(): Flow<List<Category>>

    /** New categories/{auto-id} with [name] (already trimmed and checked) and active = true. Not awaited. */
    fun addCategory(name: String)

    /** Changes the name only; tickets keep their categoryName snapshot (decision 0003). Not awaited. */
    fun renameCategory(id: String, name: String)

    /** Changes `active` only. Not awaited. */
    fun setActive(id: String, active: Boolean)
}

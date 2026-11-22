package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pt.isec.amov.cityfix.model.Category
import java.text.Collator

class FirestoreCategoriesRepository(
    private val firestore: FirebaseFirestore,
) : CategoriesRepository {

    override fun allCategories(): Flow<List<Category>> =
        firestore.collection(CATEGORIES)
            .snapshots()
            .map { snapshot ->
                val collator = Collator.getInstance()
                snapshot.documents.map { it.toCategory() }
                    .sortedWith { a, b -> collator.compare(a.name, b.name) }
            }
            .catch {
                Log.w(TAG, "categories failed", it)
                emit(emptyList())
            }

    // The writes are not awaited (as F-011 and D-011): queued in the local cache, also offline,
    // and the listener above shows them at once. A refusal by the server is only logged.

    override fun addCategory(name: String) {
        firestore.collection(CATEGORIES).document()
            .set(Category(name = name).toCategoryDocument())
            .addOnFailureListener { Log.w(TAG, "add category refused", it) }
    }

    override fun renameCategory(id: String, name: String) {
        firestore.collection(CATEGORIES).document(id).update("name", name)
            .addOnFailureListener { Log.w(TAG, "rename category refused", it) }
    }

    override fun setActive(id: String, active: Boolean) {
        firestore.collection(CATEGORIES).document(id).update("active", active)
            .addOnFailureListener { Log.w(TAG, "category active change refused", it) }
    }

    private companion object {
        const val TAG = "CategoriesRepository"
    }
}

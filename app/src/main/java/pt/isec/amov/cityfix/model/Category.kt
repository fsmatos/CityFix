package pt.isec.amov.cityfix.model

/** Issue category defined by the municipality ("Categoria" in the brief). */
data class Category(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val name: String = "",
    val active: Boolean = true,
)

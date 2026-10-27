package pt.isec.amov.cityfix.model

/**
 * Base of the user types ("Utilizador" in the diagram).
 * Sealed: the concrete type is the role (no role field). The Firebase layer stores a `role` string.
 */
sealed interface User {
    /** Firestore document id (Auth uid for users). Empty until persisted; sample data must set distinct ids. */
    val id: String
    val name: String
    val nif: String
    val email: String
    val phone: String
    val photo: String?
}

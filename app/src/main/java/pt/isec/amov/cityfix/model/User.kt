package pt.isec.amov.cityfix.model

sealed interface User {
    val id: String
    val name: String
    val nif: String
    val email: String
    val phone: String
    val photo: String?
}

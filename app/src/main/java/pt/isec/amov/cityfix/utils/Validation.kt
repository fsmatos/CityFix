package pt.isec.amov.cityfix.utils

/*
 * Input rules for the account forms (registration, login, password reset, account setup).
 * Pure Kotlin, no Android imports: the ViewModels map each false to a string resource.
 */

const val MIN_PASSWORD_LENGTH = 8

// Dots only between characters, on both sides of the @ (Firebase refuses "a@b..pt" and ".a@b.pt").
private val EMAIL = Regex("^[A-Za-z0-9_%+-]+(\\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
private val PORTUGUESE_PHONE = Regex("[29][0-9]{8}")
private val PORTUGUESE_MOBILE = Regex("9[0-9]{8}")
private val NIF = Regex("[1-9][0-9]{8}")

fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

fun isInEmailDomain(email: String, domain: String): Boolean =
    email.trim().substringAfterLast('@', missingDelimiterValue = "").lowercase() == domain

fun isPortuguesePhone(digits: String): Boolean = PORTUGUESE_PHONE.matches(digits)

fun nationalPhoneDigits(phone: String): String {
    val compact = phone.filterNot { it.isWhitespace() }
    return when {
        compact.startsWith("+351") -> compact.removePrefix("+351")
        compact.startsWith("00351") -> compact.removePrefix("00351")
        else -> compact
    }
}

fun isPortugueseMobile(digits: String): Boolean = PORTUGUESE_MOBILE.matches(digits)

fun isValidNif(nif: String): Boolean {
    if (!NIF.matches(nif)) return false
    var sum = 0
    for (i in 0 until 8) {
        sum += nif[i].digitToInt() * (9 - i)
    }
    val remainder = sum % 11
    val check = if (remainder < 2) 0 else 11 - remainder
    return nif[8].digitToInt() == check
}

enum class PasswordProblem { TOO_SHORT, NO_LETTER, NO_DIGIT }

fun passwordProblem(password: String): PasswordProblem? = when {
    password.length < MIN_PASSWORD_LENGTH -> PasswordProblem.TOO_SHORT
    password.none { it.isLetter() } -> PasswordProblem.NO_LETTER
    password.none { it.isDigit() } -> PasswordProblem.NO_DIGIT
    else -> null
}

fun isValidName(name: String): Boolean = name.trim().length >= 2

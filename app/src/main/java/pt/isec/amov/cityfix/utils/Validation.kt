package pt.isec.amov.cityfix.utils

/*
 * Input rules for the account forms (registration, login, password reset, account setup).
 * Pure Kotlin, no Android imports: the ViewModels map each false to a string resource.
 */

/** Minimum password length for a new account (D-010 Q7; Firebase's own minimum is 6). */
const val MIN_PASSWORD_LENGTH = 8

// Dots only between characters, on both sides of the @ (Firebase refuses "a@b..pt" and ".a@b.pt").
private val EMAIL = Regex("^[A-Za-z0-9_%+-]+(\\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
private val PORTUGUESE_PHONE = Regex("[29][0-9]{8}")
private val PORTUGUESE_MOBILE = Regex("9[0-9]{8}")
private val NIF = Regex("[1-9][0-9]{8}")

/** A plain address such as `name@example.com` (checked on the trimmed value). */
fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

/**
 * True when the part after the last `@` of [email] is [domain]. The domain is stored lowercase
 * without `@` (decision 0010), so the e-mail's domain is lowercased before comparing.
 */
fun isInEmailDomain(email: String, domain: String): Boolean =
    email.trim().substringAfterLast('@', missingDelimiterValue = "").lowercase() == domain

/** A Portuguese phone number: 9 digits starting with 2 (landline) or 9 (mobile), as in the PT numbering plan. */
fun isPortuguesePhone(digits: String): Boolean = PORTUGUESE_PHONE.matches(digits)

/** A Portuguese mobile number: 9 digits starting with 9 (the SMS second factor needs a mobile). */
fun isPortugueseMobile(digits: String): Boolean = PORTUGUESE_MOBILE.matches(digits)

/**
 * A Portuguese NIF: 9 digits, the first not 0, and the mod-11 check digit.
 * sum = d1*9 + d2*8 + ... + d8*2; r = sum % 11; the 9th digit is 0 when r < 2, else 11 - r.
 * For example 123456789 is valid.
 */
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

/** Why a password is too weak. */
enum class PasswordProblem { TOO_SHORT, NO_LETTER, NO_DIGIT }

/** The first problem of [password], or null when it has [MIN_PASSWORD_LENGTH] characters, a letter and a digit. */
fun passwordProblem(password: String): PasswordProblem? = when {
    password.length < MIN_PASSWORD_LENGTH -> PasswordProblem.TOO_SHORT
    password.none { it.isLetter() } -> PasswordProblem.NO_LETTER
    password.none { it.isDigit() } -> PasswordProblem.NO_DIGIT
    else -> null
}

/** A name with at least 2 characters after trimming. */
fun isValidName(name: String): Boolean = name.trim().length >= 2

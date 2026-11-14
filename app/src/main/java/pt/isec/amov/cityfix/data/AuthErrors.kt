package pt.isec.amov.cityfix.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * One place for the Firebase exception -> AuthError mapping, shared by [FirebaseAuthRepository]
 * (sign-in, MFA) and [FirestoreUsersRepository] (sign-up). With e-mail enumeration protection,
 * a wrong e-mail and a wrong password both arrive as invalid credentials.
 *
 * FirebaseAuthWeakPasswordException extends FirebaseAuthInvalidCredentialsException, so it is
 * checked first.
 */
internal fun Exception.toAuthError(): AuthError = when (this) {
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_VERIFICATION_CODE" -> AuthError.WRONG_CODE
        "ERROR_SESSION_EXPIRED" -> AuthError.CODE_EXPIRED
        "ERROR_INVALID_PHONE_NUMBER" -> AuthError.INVALID_PHONE_NUMBER
        else -> AuthError.INVALID_CREDENTIALS
    }
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseAuthInvalidUserException ->
        if (errorCode == "ERROR_USER_NOT_FOUND") AuthError.INVALID_CREDENTIALS else AuthError.USER_DISABLED
    is FirebaseAuthRecentLoginRequiredException -> AuthError.RECENT_LOGIN_REQUIRED
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    else -> AuthError.UNKNOWN
}

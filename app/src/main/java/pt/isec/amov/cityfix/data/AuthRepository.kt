package pt.isec.amov.cityfix.data

import android.app.Activity
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneMultiFactorInfo
import kotlinx.coroutines.flow.StateFlow

data class AuthAccount(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean,
    val hasVerifiedPhone: Boolean,
)

enum class AuthError {
    INVALID_CREDENTIALS,
    USER_DISABLED,
    NETWORK,
    TOO_MANY_REQUESTS,
    WRONG_CODE,
    CODE_EXPIRED,
    CODE_NOT_SENT,
    INVALID_PHONE_NUMBER,
    PHONE_IN_USE,
    RECENT_LOGIN_REQUIRED,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    UNKNOWN,
}

class SecondFactorChallenge internal constructor(
    internal val resolver: MultiFactorResolver,
    internal val hint: PhoneMultiFactorInfo,
) {
    val maskedPhone: String get() = hint.phoneNumber
}

sealed interface SignInResult {
    data object Success : SignInResult
    data class SecondFactorRequired(val challenge: SecondFactorChallenge) : SignInResult
    data class Failure(val error: AuthError) : SignInResult
}

sealed interface CodeSendResult {
    data class Sent(val verificationId: String) : CodeSendResult

    data object Linked : CodeSendResult
    data class Failure(val error: AuthError) : CodeSendResult
}

interface AuthRepository {
    val account: StateFlow<AuthAccount?>

    suspend fun signIn(email: String, password: String): SignInResult

    suspend fun sendSecondFactorCode(activity: Activity, challenge: SecondFactorChallenge): CodeSendResult

    suspend fun resolveSecondFactor(
        challenge: SecondFactorChallenge,
        verificationId: String,
        code: String,
    ): AuthError?

    suspend fun sendPasswordReset(email: String): AuthError?

    suspend fun sendEmailVerification(): AuthError?

    suspend fun reloadAccount(): AuthError?

    suspend fun sendPhoneCode(activity: Activity, phoneE164: String): CodeSendResult

    suspend fun linkPhone(verificationId: String, code: String): AuthError?

    suspend fun reauthenticate(password: String): SignInResult

    suspend fun changePassword(newPassword: String): AuthError?

    suspend fun requestEmailChange(newEmail: String): AuthError?

    fun signOut()
}

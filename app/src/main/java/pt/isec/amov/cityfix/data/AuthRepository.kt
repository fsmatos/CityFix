package pt.isec.amov.cityfix.data

import android.app.Activity
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneMultiFactorInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * What the app needs to know about the signed-in Firebase account, without Firebase types.
 * The profile (name, role, registration status) is in `users/{uid}`, read by [UsersRepository].
 */
data class AuthAccount(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean,
    /** True when the account has at least one enrolled second factor (SMS). */
    val hasSecondFactor: Boolean,
)

/** Auth failures the screens can show. The ViewModels map each one to a string resource. */
enum class AuthError {
    INVALID_CREDENTIALS,
    USER_DISABLED,
    NETWORK,
    TOO_MANY_REQUESTS,
    WRONG_CODE,
    CODE_EXPIRED,
    CODE_NOT_SENT,
    /** Firebase refused the phone number given for enrolment. */
    INVALID_PHONE_NUMBER,
    RECENT_LOGIN_REQUIRED,
    UNKNOWN,
}

/**
 * The second step of a sign-in with MFA. Opaque to the ViewModels: it wraps the Firebase
 * [MultiFactorResolver] and the chosen phone hint, so no Firebase type leaves `data/`.
 * Not serialisable: after process death the login starts again from the credentials step.
 */
class SecondFactorChallenge internal constructor(
    internal val resolver: MultiFactorResolver,
    internal val hint: PhoneMultiFactorInfo,
) {
    /** The enrolled phone as Firebase gives it (already masked, for example +3519******78). */
    val maskedPhone: String get() = hint.phoneNumber
}

sealed interface SignInResult {
    data object Success : SignInResult
    data class SecondFactorRequired(val challenge: SecondFactorChallenge) : SignInResult
    data class Failure(val error: AuthError) : SignInResult
}

sealed interface CodeSendResult {
    /** The SMS was sent; [verificationId] goes back with the typed code. */
    data class Sent(val verificationId: String) : CodeSendResult
    data class Failure(val error: AuthError) : CodeSendResult
}

/**
 * E-mail/password sign-in with an SMS second factor (brief I.2.B, I.3.B). ViewModels depend on
 * this interface; [FirebaseAuthRepository] is the implementation. For the `AuthError?` returns,
 * `null` means success.
 */
interface AuthRepository {
    /** The current account, or null when signed out. Updated on sign-in, sign-out, reload and enrol. */
    val account: StateFlow<AuthAccount?>

    /** Signs in with e-mail and password; an enrolled account answers [SignInResult.SecondFactorRequired]. */
    suspend fun signIn(email: String, password: String): SignInResult

    /** Sends the SMS code to the challenge's phone. [activity] is used for reCAPTCHA only and not kept. */
    suspend fun sendSecondFactorCode(activity: Activity, challenge: SecondFactorChallenge): CodeSendResult

    /** Finishes the sign-in with the typed code. Null on success. */
    suspend fun resolveSecondFactor(
        challenge: SecondFactorChallenge,
        verificationId: String,
        code: String,
    ): AuthError?

    /** Sends the Firebase password reset e-mail. Null on success (also for an unknown e-mail). */
    suspend fun sendPasswordReset(email: String): AuthError?

    /** Sends the verification e-mail to the signed-in account. Null on success. */
    suspend fun sendEmailVerification(): AuthError?

    /** Reloads the account and refreshes its token (verified e-mail claim), then updates [account]. Null on success. */
    suspend fun reloadAccount(): AuthError?

    /** Sends the enrolment SMS to [phoneE164]. [activity] is used for reCAPTCHA only and not kept. */
    suspend fun sendEnrolmentCode(activity: Activity, phoneE164: String): CodeSendResult

    /** Enrols the phone as second factor with the typed code, then updates [account]. Null on success. */
    suspend fun enrolSecondFactor(verificationId: String, code: String): AuthError?

    fun signOut()
}

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
    /**
     * True when the phone was verified by SMS at sign-up (F-018: a linked phone), or the account
     * was created before F-018 with the phone enrolled as an MFA factor.
     */
    val hasVerifiedPhone: Boolean,
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
    /** Firebase refused the phone number given at sign-up. */
    INVALID_PHONE_NUMBER,
    /** The phone is already linked to another account (F-018: one account per phone). */
    PHONE_IN_USE,
    RECENT_LOGIN_REQUIRED,
    /** Sign-up with an e-mail that already has an account. */
    EMAIL_IN_USE,
    /** Sign-up with a password Firebase considers too weak. */
    WEAK_PASSWORD,
    UNKNOWN,
}

/**
 * The second step of a sign-in with MFA, only for accounts created before F-018 (their phone is
 * still an enrolled factor, so Firebase keeps asking for the code). Opaque to the ViewModels: it wraps the Firebase
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

    /** Sign-up only (F-020): Firebase verified the phone without a typed code, and it is already linked. */
    data object Linked : CodeSendResult
    data class Failure(val error: AuthError) : CodeSendResult
}

/**
 * E-mail/password sign-in; the phone is verified by SMS once, at sign-up (F-018, decision 0026).
 * Accounts created before F-018 still answer the sign-in with an SMS second factor. ViewModels depend on
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

    /** Sends the sign-up SMS to [phoneE164]. [activity] is used for reCAPTCHA only and not kept. */
    suspend fun sendPhoneCode(activity: Activity, phoneE164: String): CodeSendResult

    /** Links the verified phone to the account with the typed code, then updates [account]. Null on success. */
    suspend fun linkPhone(verificationId: String, code: String): AuthError?

    /**
     * Re-authenticates the signed-in user with [password]. An enrolled account answers
     * SecondFactorRequired; finish it with sendSecondFactorCode + resolveSecondFactor.
     */
    suspend fun reauthenticate(password: String): SignInResult

    /** After a recent re-authentication. Null on success. */
    suspend fun changePassword(newPassword: String): AuthError?

    /** Sends the verification link to [newEmail] (verifyBeforeUpdateEmail). Null on success. */
    suspend fun requestEmailChange(newEmail: String): AuthError?

    fun signOut()
}

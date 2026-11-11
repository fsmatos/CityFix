package pt.isec.amov.cityfix.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * [AuthRepository] over Firebase Auth. Created once in CityFixApplication, so it lives for the
 * whole process: the AuthStateListener registered in `init` is never removed, and there is no
 * screen or activity it could leak.
 *
 * The listener fires on sign-in and sign-out only. `reload()` and enrolment change the same
 * FirebaseUser without firing it, so those calls update [account] themselves.
 */
class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {

    private val _account = MutableStateFlow(auth.currentUser?.toAccount())
    override val account: StateFlow<AuthAccount?> = _account.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _account.value = firebaseAuth.currentUser?.toAccount()
        }
    }

    override suspend fun signIn(email: String, password: String): SignInResult =
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            SignInResult.Success
        } catch (e: Exception) {
            SignInResult.Failure(e.toAuthError())
        }

    override suspend fun sendSecondFactorCode(
        activity: Activity,
        challenge: SecondFactorChallenge,
    ): CodeSendResult {
        // D-009 slice 6
        return CodeSendResult.Failure(AuthError.UNKNOWN)
    }

    override suspend fun resolveSecondFactor(
        challenge: SecondFactorChallenge,
        verificationId: String,
        code: String,
    ): AuthError? {
        // D-009 slice 6
        return AuthError.UNKNOWN
    }

    override suspend fun sendPasswordReset(email: String): AuthError? =
        try {
            auth.sendPasswordResetEmail(email).await()
            null
        } catch (e: Exception) {
            e.toAuthError()
        }

    override suspend fun sendEmailVerification(): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            user.sendEmailVerification().await()
            null
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    override suspend fun reloadAccount(): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            user.reload().await()
            // A new token carries the new email_verified claim the rules check (F-010 handover).
            user.getIdToken(true).await()
            _account.value = auth.currentUser?.toAccount()
            null
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    override suspend fun sendEnrolmentCode(activity: Activity, phoneE164: String): CodeSendResult {
        val user = auth.currentUser ?: return CodeSendResult.Failure(AuthError.UNKNOWN)
        val session = try {
            user.multiFactor.session.await()
        } catch (e: Exception) {
            return CodeSendResult.Failure(e.toAuthError())
        }
        return requestCode(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneE164)
                .setMultiFactorSession(session)
                .setActivity(activity),
        )
    }

    override suspend fun enrolSecondFactor(verificationId: String, code: String): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            user.multiFactor.enroll(PhoneMultiFactorGenerator.getAssertion(credential), FACTOR_NAME).await()
            // The AuthStateListener does not fire for an enrolment: reload and re-emit by hand.
            user.reload().await()
            _account.value = auth.currentUser?.toAccount()
            null
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    /**
     * Sends an SMS code with [builder] (phone or hint, session and activity already set) and
     * waits for Firebase's callbacks. With requireSmsValidation(true) the code is always typed:
     * an auto-retrieval (onVerificationCompleted) is ignored. The callbacks are bound to the
     * activity, so a rotation during the send can lose onCodeSent: after [SEND_TIMEOUT_MS] the
     * result is CODE_NOT_SENT and the screen offers Resend.
     */
    private suspend fun requestCode(builder: PhoneAuthOptions.Builder): CodeSendResult =
        withTimeoutOrNull(SEND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        if (continuation.isActive) continuation.resume(CodeSendResult.Sent(verificationId))
                    }

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) = Unit

                    override fun onVerificationFailed(e: FirebaseException) {
                        val error = when (val mapped = e.toAuthError()) {
                            AuthError.NETWORK, AuthError.TOO_MANY_REQUESTS -> mapped
                            else -> AuthError.CODE_NOT_SENT
                        }
                        if (continuation.isActive) continuation.resume(CodeSendResult.Failure(error))
                    }
                }
                PhoneAuthProvider.verifyPhoneNumber(
                    builder
                        .setTimeout(SEND_TIMEOUT_MS / 1_000, TimeUnit.SECONDS)
                        .requireSmsValidation(true)
                        .setCallbacks(callbacks)
                        .build(),
                )
            }
        } ?: CodeSendResult.Failure(AuthError.CODE_NOT_SENT)

    private fun FirebaseUser.toAccount() = AuthAccount(
        uid = uid,
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        hasSecondFactor = multiFactor.enrolledFactors.isNotEmpty(),
    )

    /**
     * One place for the Firebase exception -> AuthError mapping. With e-mail enumeration
     * protection, a wrong e-mail and a wrong password both arrive as invalid credentials.
     */
    private fun Exception.toAuthError(): AuthError = when (this) {
        is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
            "ERROR_INVALID_VERIFICATION_CODE" -> AuthError.WRONG_CODE
            "ERROR_SESSION_EXPIRED" -> AuthError.CODE_EXPIRED
            else -> AuthError.INVALID_CREDENTIALS
        }
        is FirebaseAuthInvalidUserException ->
            if (errorCode == "ERROR_USER_NOT_FOUND") AuthError.INVALID_CREDENTIALS else AuthError.USER_DISABLED
        is FirebaseAuthRecentLoginRequiredException -> AuthError.RECENT_LOGIN_REQUIRED
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        else -> AuthError.UNKNOWN
    }

    private companion object {
        const val SEND_TIMEOUT_MS = 60_000L

        /** The factor's name in the Firebase console; never shown in the app. */
        const val FACTOR_NAME = "Phone"
    }
}

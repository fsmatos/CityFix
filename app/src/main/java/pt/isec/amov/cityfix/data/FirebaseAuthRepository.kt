package pt.isec.amov.cityfix.data

import android.app.Activity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

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
        // D-009 slice 5
        return CodeSendResult.Failure(AuthError.UNKNOWN)
    }

    override suspend fun enrolSecondFactor(verificationId: String, code: String): AuthError? {
        // D-009 slice 5
        return AuthError.UNKNOWN
    }

    override fun signOut() {
        auth.signOut()
    }

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
}

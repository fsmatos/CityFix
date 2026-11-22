package pt.isec.amov.cityfix.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

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
        } catch (e: FirebaseAuthMultiFactorException) {
            // Right password, account enrolled before F-018: Firebase asks for the second factor.
            challengeOf(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SignInResult.Failure(e.toAuthError())
        }

    override suspend fun reauthenticate(password: String): SignInResult {
        val user = auth.currentUser ?: return SignInResult.Failure(AuthError.UNKNOWN)
        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), password)).await()
            SignInResult.Success
        } catch (e: FirebaseAuthMultiFactorException) {
            challengeOf(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SignInResult.Failure(e.toAuthError())
        }
    }

    override suspend fun changePassword(newPassword: String): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            // This device stays signed in; Firebase signs the other devices out.
            user.updatePassword(newPassword).await()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    override suspend fun requestEmailChange(newEmail: String): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            // Only sends the link: the Auth e-mail changes when the user opens it.
            user.verifyBeforeUpdateEmail(newEmail.trim().lowercase()).await()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    private fun challengeOf(e: FirebaseAuthMultiFactorException): SignInResult {
        val hint = e.resolver.hints.filterIsInstance<PhoneMultiFactorInfo>().firstOrNull()
            ?: return SignInResult.Failure(AuthError.UNKNOWN)
        return SignInResult.SecondFactorRequired(SecondFactorChallenge(e.resolver, hint))
    }

    override suspend fun sendSecondFactorCode(
        activity: Activity,
        challenge: SecondFactorChallenge,
    ): CodeSendResult =
        when (
            val outcome = requestCode(
                PhoneAuthOptions.newBuilder(auth)
                    .setMultiFactorHint(challenge.hint)
                    .setMultiFactorSession(challenge.resolver.session)
                    .setActivity(activity),
                mfa = true,
            )
        ) {
            is SmsOutcome.Done -> outcome.result
            // Not reached: with requireSmsValidation(true) Firebase never auto-verifies. A
            // Failure (not a crash) keeps the screen usable: the user can tap Resend.
            is SmsOutcome.Verified -> CodeSendResult.Failure(AuthError.CODE_NOT_SENT)
        }

    override suspend fun resolveSecondFactor(
        challenge: SecondFactorChallenge,
        verificationId: String,
        code: String,
    ): AuthError? =
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            // Signs the user in: the AuthStateListener then updates the account. With the
            // resolver of reauthenticate() it completes the re-authentication instead (D-011).
            challenge.resolver.resolveSignIn(PhoneMultiFactorGenerator.getAssertion(credential)).await()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }

    override suspend fun sendPasswordReset(email: String): AuthError? =
        try {
            auth.sendPasswordResetEmail(email).await()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }

    override suspend fun sendEmailVerification(): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            user.sendEmailVerification().await()
            null
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    override suspend fun sendPhoneCode(activity: Activity, phoneE164: String): CodeSendResult {
        if (auth.currentUser == null) return CodeSendResult.Failure(AuthError.UNKNOWN)
        // A plain phone verification (no multi-factor session): the code is only used by linkPhone.
        val outcome = requestCode(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneE164)
                .setActivity(activity),
            mfa = false,
        )
        return when (outcome) {
            is SmsOutcome.Done -> outcome.result
            // Firebase verified the phone by itself (instant verification or auto-retrieval
            // before onCodeSent): link it now instead of waiting for a typed code (F-020).
            is SmsOutcome.Verified -> when (val error = linkCredential(outcome.credential)) {
                null -> CodeSendResult.Linked
                else -> CodeSendResult.Failure(error)
            }
        }
    }

    override suspend fun linkPhone(verificationId: String, code: String): AuthError? =
        linkCredential(PhoneAuthProvider.getCredential(verificationId, code))

    private suspend fun linkCredential(credential: PhoneAuthCredential): AuthError? {
        val user = auth.currentUser ?: return AuthError.UNKNOWN
        return try {
            // Adds the phone to the same account; the e-mail/password login is unchanged.
            user.linkWithCredential(credential).await()
            // The AuthStateListener does not fire for a link: reload and re-emit by hand. The
            // phone is already linked on the server, so a failed reload (a network blip) must not
            // report the link as failed: the reload is best effort.
            try {
                user.reload().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing to report: linkWithCredential already updated the local user.
            }
            _account.value = auth.currentUser?.toAccount()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    private sealed interface SmsOutcome {
        data class Done(val result: CodeSendResult) : SmsOutcome
        data class Verified(val credential: PhoneAuthCredential) : SmsOutcome
    }

    private suspend fun requestCode(builder: PhoneAuthOptions.Builder, mfa: Boolean): SmsOutcome =
        withTimeoutOrNull(SEND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(SmsOutcome.Done(CodeSendResult.Sent(verificationId)))
                        }
                    }

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        if (continuation.isActive) continuation.resume(SmsOutcome.Verified(credential))
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        val error = when (val mapped = e.toAuthError()) {
                            AuthError.NETWORK,
                            AuthError.TOO_MANY_REQUESTS,
                            AuthError.INVALID_PHONE_NUMBER -> mapped
                            else -> AuthError.CODE_NOT_SENT
                        }
                        if (continuation.isActive) {
                            continuation.resume(SmsOutcome.Done(CodeSendResult.Failure(error)))
                        }
                    }
                }
                PhoneAuthProvider.verifyPhoneNumber(
                    builder
                        .setTimeout(SEND_TIMEOUT_MS / 1_000, TimeUnit.SECONDS)
                        .setCallbacks(callbacks)
                        .apply { if (mfa) requireSmsValidation(true) }
                        .build(),
                )
            }
        } ?: SmsOutcome.Done(CodeSendResult.Failure(AuthError.CODE_NOT_SENT))

    private fun FirebaseUser.toAccount() = AuthAccount(
        uid = uid,
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        hasVerifiedPhone = !phoneNumber.isNullOrBlank() || multiFactor.enrolledFactors.isNotEmpty(),
    )

    private companion object {
        const val SEND_TIMEOUT_MS = 60_000L
    }
}

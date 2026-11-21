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

/**
 * [AuthRepository] over Firebase Auth. Created once in CityFixApplication, so it lives for the
 * whole process: the AuthStateListener registered in `init` is never removed, and there is no
 * screen or activity it could leak.
 *
 * Phone at sign-up (F-018, decision 0026): [sendPhoneCode] and [linkPhone] verify the phone once
 * and link it to the account (a phone provider), not as an MFA factor, so later logins need only
 * e-mail and password. Accounts created before F-018 still have the phone enrolled as a factor:
 * Firebase then throws the multi-factor exception at sign-in, and [resolveSecondFactor] finishes
 * both that sign-in and a re-authentication (D-011): the resolver comes from [signIn] or
 * [reauthenticate].
 *
 * The listener fires on sign-in and sign-out only. `reload()` and linking change the same
 * FirebaseUser without firing it, so those calls update [account] themselves.
 *
 * Every `catch (e: Exception)` is preceded by a rethrow of CancellationException: `await()`
 * throws it when the caller's scope is cancelled (a screen closed), and turning it into an
 * AuthError would break structured concurrency.
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
        } catch (e: FirebaseAuthMultiFactorException) {
            // Right password, account enrolled before F-018: Firebase asks for the second factor.
            challengeOf(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SignInResult.Failure(e.toAuthError())
        }

    /**
     * Re-authentication (D-011) works like [signIn]: an enrolled account throws the multi-factor
     * exception, and [resolveSecondFactor] with its resolver completes the re-authentication.
     */
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

    /** Accounts have one phone factor (D-009 Q13), so the first phone hint is the one. */
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
            // Not reached: with requireSmsValidation(true) Firebase never auto-verifies.
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

    /** Links the phone [credential] to the signed-in account and re-emits [account]. Null on success. */
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

    /** What [requestCode] got back: a send result, or a phone Firebase verified by itself. */
    private sealed interface SmsOutcome {
        data class Done(val result: CodeSendResult) : SmsOutcome
        data class Verified(val credential: PhoneAuthCredential) : SmsOutcome
    }

    /**
     * Sends an SMS code with [builder] (phone or hint, session and activity already set) and
     * waits for Firebase's callbacks.
     *
     * [mfa] is true for the second-factor code (sign-in and re-authentication): it adds
     * requireSmsValidation(true), so the code is always typed and onVerificationCompleted never
     * comes. Firebase accepts that option only with a multi-factor session, so the sign-up
     * phone ([mfa] false) must not set it (F-020); there an onVerificationCompleted before
     * onCodeSent returns [SmsOutcome.Verified]. One that comes after onCodeSent is ignored:
     * the user types the code as usual.
     *
     * The callbacks are bound to the activity, so a rotation during the send can lose
     * onCodeSent: after [SEND_TIMEOUT_MS] the result is CODE_NOT_SENT and the screen offers Resend.
     */
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
                builder
                    .setTimeout(SEND_TIMEOUT_MS / 1_000, TimeUnit.SECONDS)
                    .setCallbacks(callbacks)
                if (mfa) builder.requireSmsValidation(true)
                PhoneAuthProvider.verifyPhoneNumber(builder.build())
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

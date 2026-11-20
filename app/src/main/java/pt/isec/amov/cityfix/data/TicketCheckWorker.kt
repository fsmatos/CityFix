package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import pt.isec.amov.cityfix.CityFixApplication
import java.util.concurrent.TimeUnit

/**
 * Checks the user's tickets while the app is closed (brief III, F-016 Gate 1 Q2, Q7): every
 * 15 minutes (the WorkManager minimum) with a network connection, it reads the tickets once and
 * hands them to TicketNotifier, which also covers the 24 h warning and the close (Q5).
 *
 * As the slides 10/11 example and our UploadWorker: a CoroutineWorker, the user from inputData,
 * Result.retry() on an exception. A citizen reads their own tickets, an operator all of them.
 */
class TicketCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CityFixApplication
        val uid = inputData.getString(KEY_UID) ?: return Result.success()
        val isOperator = inputData.getBoolean(KEY_IS_OPERATOR, false)
        // Logged out (or another account) since it was scheduled: nothing to check.
        if (app.auth.currentUser?.uid != uid) return Result.success()

        return try {
            val tickets = if (isOperator) {
                app.ticketsRepository.allTicketsOnce()
            } else {
                app.ticketsRepository.participantTicketsOnce(uid)
            }
            app.ticketNotifier.check(uid, isOperator, tickets, System.currentTimeMillis())
            Result.success()
        } catch (e: CancellationException) {
            throw e // WorkManager stopped the worker: it runs again later.
        } catch (e: Exception) {
            Log.w(TAG, "ticket check failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "TicketCheckWorker"
        private const val WORK_NAME = "ticket-check"
        private const val KEY_UID = "uid"
        private const val KEY_IS_OPERATOR = "isOperator"

        /**
         * Starts (or replaces, for a new user) the periodic check. Unique work, so calling it
         * again after a rotation or a new start does not add a second one.
         */
        fun schedule(context: Context, uid: String, isOperator: Boolean) {
            val request = PeriodicWorkRequestBuilder<TicketCheckWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_UID to uid, KEY_IS_OPERATOR to isOperator))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Stops the periodic check (Log out, or a user who gets no notifications). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

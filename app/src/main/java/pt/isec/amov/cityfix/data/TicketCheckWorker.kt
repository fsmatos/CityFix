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

        fun schedule(context: Context, uid: String, isOperator: Boolean) {
            val request = PeriodicWorkRequestBuilder<TicketCheckWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_UID to uid, KEY_IS_OPERATOR to isOperator))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

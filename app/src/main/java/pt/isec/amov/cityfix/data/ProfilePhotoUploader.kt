package pt.isec.amov.cityfix.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Enqueues the upload of the profile photo chosen at registration (D-010) or on Profile (D-011),
 * the same pattern as AttachmentUploader.enqueue (F-013). Create one per app (CityFixApplication);
 * it keeps the application context only.
 */
class ProfilePhotoUploader(context: Context) {

    private val appContext = context.applicationContext

    /**
     * One OneTimeWorkRequest for [localPath] (a file in filesDir/images): it runs only with a
     * network connection and, after a failed attempt, waits 30 s, then longer each time. Call it
     * after `users/[uid]` is committed, so the worker's update finds the document.
     *
     * Unique work per user with REPLACE (D-011): a newer photo cancels an older upload still
     * waiting, so two quick changes cannot finish in the wrong order. The cancelled worker's
     * source file may stay in filesDir/images (a small orphan, accepted).
     */
    fun enqueue(uid: String, localPath: String) {
        val request = OneTimeWorkRequestBuilder<ProfilePhotoWorker>()
            .setInputData(
                workDataOf(
                    ProfilePhotoWorker.KEY_UID to uid,
                    ProfilePhotoWorker.KEY_PATH to localPath,
                ),
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("profile_photo_$uid", ExistingWorkPolicy.REPLACE, request)
    }
}

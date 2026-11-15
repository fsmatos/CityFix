package pt.isec.amov.cityfix.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Enqueues the upload of the optional profile photo chosen at registration (D-010), the same
 * pattern as AttachmentUploader.enqueue (F-013). Create one per app (CityFixApplication); it keeps
 * the application context only.
 */
class ProfilePhotoUploader(context: Context) {

    private val appContext = context.applicationContext

    /**
     * One OneTimeWorkRequest for [localPath] (a file in filesDir/images): it runs only with a
     * network connection and, after a failed attempt, waits 30 s, then longer each time. Call it
     * after `users/[uid]` is committed, so the worker's update finds the document.
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
        WorkManager.getInstance(appContext).enqueue(request)
    }
}

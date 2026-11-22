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

class ProfilePhotoUploader(context: Context) {

    private val appContext = context.applicationContext

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

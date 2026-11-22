package pt.isec.amov.cityfix.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.isec.amov.cityfix.MainActivity
import pt.isec.amov.cityfix.R

/*
 * Local notifications (brief III, F-016), written like the class demos: P11 and the
 * ResourcesMenusNotify example create the channel, then NotificationCompat.Builder + notify().
 */

const val CHANNEL_TICKETS = "tickets"

const val CHANNEL_MESSAGES = "messages"

const val CHANNEL_PROXIMITY = "proximity"

const val CHANNEL_TRACKING = "tracking"

const val CHANNEL_IMPACT = "impact"

const val EXTRA_TICKET_ID = "ticketId"

const val EXTRA_FROM_IMPACT = "fromImpact"

private const val IMPACT_NOTIFICATION_ID = 2001

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TICKETS,
                context.getString(R.string.channel_tickets),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.channel_messages),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROXIMITY,
                context.getString(R.string.channel_proximity),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRACKING,
                context.getString(R.string.channel_tracking),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_IMPACT,
                context.getString(R.string.channel_impact),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}

fun showNotification(
    context: Context,
    channel: String,
    id: Int,
    title: String,
    text: String,
    ticketId: String?,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val intent = Intent(context, MainActivity::class.java)
    if (ticketId != null) intent.putExtra(EXTRA_TICKET_ID, ticketId)
    // The request code is the notification id, so each notification keeps its own ticket id.
    val pendingIntent = PendingIntent.getActivity(
        context,
        id,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val builder = NotificationCompat.Builder(context, channel)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    NotificationManagerCompat.from(context).notify(id, builder.build())
}

fun showImpactNotification(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val intent = Intent(context, MainActivity::class.java).putExtra(EXTRA_FROM_IMPACT, true)
    val pendingIntent = PendingIntent.getActivity(
        context,
        IMPACT_NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val builder = NotificationCompat.Builder(context, CHANNEL_IMPACT)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(context.getString(R.string.impact_notification_title))
        .setContentText(context.getString(R.string.impact_suggestion))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
    NotificationManagerCompat.from(context).notify(IMPACT_NOTIFICATION_ID, builder.build())
}

package corsica.universita.carnet.notif

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import corsica.universita.carnet.MainActivity
import corsica.universita.carnet.R

private const val TAG = "Carnet"

/**
 * Les notifications de Carnet (TD A6). Deux canaux, créés une fois par process (`CarnetApp.onCreate`) :
 * l'utilisateur règle chacun (importance, son, vibration) dans les réglages système, pas l'app.
 * Sans canal, `notify()` n'affiche rien sur Android 8+ — et ne lève aucune exception.
 */
object Notifications {
    const val CHANNEL_SYNC = "sync"
    const val CHANNEL_HUB = "hub"

    /** L'extra de l'Intent de clic : la note à ouvrir. Lu par MainActivity.handleIntent. */
    const val EXTRA_NOTE_ID = "noteId"

    private const val ID_SYNC = 1

    fun createChannels(ctx: Context) {
        val manager = NotificationManagerCompat.from(ctx)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_SYNC, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(ctx.getString(R.string.channel_sync_name))
                .setDescription(ctx.getString(R.string.channel_sync_desc))
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_HUB, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(ctx.getString(R.string.channel_hub_name))
                .setDescription(ctx.getString(R.string.channel_hub_desc))
                .build(),
        )
        Log.d(TAG, "Canaux de notification créés : $CHANNEL_SYNC, $CHANNEL_HUB")
    }

    /** `false` si la permission POST_NOTIFICATIONS est refusée (Android 13+) ou l'app coupée dans les réglages. */
    fun areEnabled(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /** Notification locale : la fin d'une synchronisation (SyncWorker). */
    fun showSyncDone(ctx: Context, count: Int) {
        val notification = NotificationCompat.Builder(ctx, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.notif_sync_title))
            .setContentText(ctx.resources.getQuantityString(R.plurals.notif_sync_text, count, count))
            .setContentIntent(openCarnet(ctx, noteId = null))
            .setAutoCancel(true)
            .build()
        post(ctx, ID_SYNC, notification)
    }

    /** Push FCM reçu (CarnetMessagingService) : le clic ouvre Carnet sur la note visée. */
    fun showHub(ctx: Context, title: String, body: String, noteId: Long?) {
        val notification = NotificationCompat.Builder(ctx, CHANNEL_HUB)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openCarnet(ctx, noteId))
            .setAutoCancel(true)
            .build()
        // Un id par note : deux pushs sur deux notes = deux notifications ; sur la même note, remplacée
        post(ctx, id = noteId?.toInt() ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    /**
     * Le clic (≈ `notificationclick` du Service Worker) : un PendingIntent — un Intent que l'OS lancera
     * plus tard, au nom de Carnet. Vers MainActivity (singleTop : onNewIntent si elle est déjà ouverte).
     */
    private fun openCarnet(ctx: Context, noteId: Long?): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            noteId?.let { putExtra(EXTRA_NOTE_ID, it) }
        }
        return PendingIntent.getActivity(
            ctx,
            noteId?.toInt() ?: 0, // requestCode distinct par note, sinon l'OS réutilise le même PendingIntent
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun post(ctx: Context, id: Int, notification: android.app.Notification) {
        if (!areEnabled(ctx)) {
            // Permission refusée, ou app coupée dans les réglages : notify() ne fait rien, sans exception (le catch ci-dessous est défensif)
            Log.d(TAG, "Notification $id non affichée : notifications désactivées pour Carnet")
            return
        }
        try {
            NotificationManagerCompat.from(ctx).notify(id, notification)
            Log.d(TAG, "Notification $id affichée")
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification $id refusée par l'OS : $e")
        }
    }
}

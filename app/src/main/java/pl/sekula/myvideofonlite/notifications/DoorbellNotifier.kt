package pl.sekula.myvideofonlite.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.sekula.myvideofonlite.R
import pl.sekula.myvideofonlite.domain.usecase.RingNotification
import pl.sekula.myvideofonlite.presentation.MainActivity
import pl.sekula.myvideofonlite.presentation.common.formatDateTime

/** Kanał i powiadomienie o dzwonku (spec. 4.3). */
class DoorbellNotifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_doorbell_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_doorbell_description)
            // Na ekranie blokady pokazujemy tylko ogólną treść (publicVersion).
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun showRing(ring: RingNotification) {
        if (!canNotify()) return
        val openLiveView = PendingIntent.getActivity(
            context,
            REQUEST_LIVE,
            MainActivity.intent(context, MainActivity.ACTION_LIVE_VIEW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_doorbell)
            .setContentTitle("Dzwonek")
            .setContentText("Nowe zdarzenie wideodomofonu")
            .build()

        // Celowo brak akcji "Otwórz furtkę" w powiadomieniu – otwarcie tylko z aplikacji, po potwierdzeniu.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_doorbell)
            .setContentTitle(ring.deviceName)
            .setContentText("Ktoś dzwoni do drzwi • ${formatDateTime(ring.timestampMillis)}")
            .setWhen(ring.timestampMillis)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setAutoCancel(true)
            .setContentIntent(openLiveView)
            .addAction(0, "Podgląd", openLiveView)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_RING, notification)
    }

    companion object {
        const val CHANNEL_ID = "doorbell"
        private const val NOTIFICATION_RING = 1001
        private const val REQUEST_LIVE = 1
    }
}

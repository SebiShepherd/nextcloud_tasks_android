package com.nextcloud.tasks.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nextcloud.tasks.R
import com.nextcloud.tasks.data.sync.PushSyncManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that keeps the notify_push WebSocket connection alive while the app
 * is in the background.
 *
 * The persistent notification is kept at low priority to minimise visual noise.
 * Users who prefer polling-only can disable push via Settings (Phase 2).
 */
@AndroidEntryPoint
class PushForegroundService : Service() {
    @Inject
    lateinit var pushSyncManager: PushSyncManager

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.d("PushForegroundService: starting")
        startForegroundWithNotification()
        pushSyncManager.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("PushForegroundService: stopping")
        pushSyncManager.stop()
    }

    private fun startForegroundWithNotification() {
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.push_sync_notification_title))
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .build()

        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "push_sync_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.push_sync_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = context.getString(R.string.push_sync_channel_description)
                        setShowBadge(false)
                    }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }

        fun start(context: Context) {
            Timber.d("PushForegroundService: requesting start")
            createNotificationChannel(context)
            ContextCompat.startForegroundService(
                context,
                Intent(context, PushForegroundService::class.java),
            )
        }

        fun stop(context: Context) {
            Timber.d("PushForegroundService: requesting stop")
            context.stopService(Intent(context, PushForegroundService::class.java))
        }
    }
}

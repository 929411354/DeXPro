package com.dexpro.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dexpro.launcher.DesktopActivity
import com.dexpro.launcher.R

/**
 * Foreground service that keeps DeX Pro desktop mode alive.
 * Provides persistent notification showing desktop mode is active.
 */
class DesktopService : Service() {

    companion object {
        private const val CHANNEL_ID = "dexpro_desktop"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, DesktopActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DeX Pro Desktop Mode")
                .setContentText("Desktop mode is active")
                .setSmallIcon(R.drawable.ic_dexpro_logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — service runs without notification
        } catch (e: Exception) {
            // Silently continue without foreground notification
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Desktop Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when DeX Pro desktop mode is running"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

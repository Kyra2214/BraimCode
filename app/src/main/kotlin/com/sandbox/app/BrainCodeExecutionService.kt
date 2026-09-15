package com.sandbox.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps the BrainCode process in the Android foreground while long-running
 * Sandbox work (downloads, extraction, setup, builds and jobs) is active.
 * The actual work remains owned by the existing SandboxViewModel/runtime.
 */
class BrainCodeExecutionService : Service() {
    companion object {
        const val ACTION_UPDATE = "com.sandbox.app.action.UPDATE_EXECUTION_NOTIFICATION"
        const val EXTRA_TEXT = "text"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_MAX = "max"

        private const val CHANNEL_ID = "braincode_execution"
        private const val NOTIFICATION_ID = 4202

        fun update(context: android.content.Context, text: String, progress: Int? = null, max: Int = 100) {
            val intent = Intent(context, BrainCodeExecutionService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TEXT, text)
                progress?.let { putExtra(EXTRA_PROGRESS, it) }
                putExtra(EXTRA_MAX, max)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("BrainCode ativo — processos continuam em segundo plano", null))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE) {
            val text = intent.getStringExtra(EXTRA_TEXT)
                ?: "BrainCode ativo — processos continuam em segundo plano"
            val progress = if (intent.hasExtra(EXTRA_PROGRESS)) intent.getIntExtra(EXTRA_PROGRESS, 0) else null
            val max = intent.getIntExtra(EXTRA_MAX, 100)
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(text, progress, max))
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(text: String, progress: Int?, max: Int = 100): Notification {
        val openIntent = Intent(this, BrainCodeActivity::class.java)
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, openIntent, pendingFlags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("BrainCode")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress != null) {
            builder.setProgress(max.coerceAtLeast(1), progress.coerceIn(0, max.coerceAtLeast(1)), false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Execução do BrainCode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém tarefas longas do Sandbox executando em segundo plano."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

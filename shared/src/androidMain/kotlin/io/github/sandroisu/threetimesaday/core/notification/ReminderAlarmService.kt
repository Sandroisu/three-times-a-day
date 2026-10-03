package io.github.sandroisu.threetimesaday.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import io.github.sandroisu.threetimesaday.shared.R

class ReminderAlarmService : Service() {

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: return START_NOT_STICKY
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val maxDurationMinutes = intent.getIntExtra(EXTRA_MAX_DURATION_MINUTES, DEFAULT_MAX_DURATION_MINUTES)
            .coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification(title, message))
        startAlarmOutput()
        timeoutHandler.removeCallbacksAndMessages(null)
        timeoutHandler.postDelayed(
            { stopSelf() },
            maxDurationMinutes * MILLIS_PER_MINUTE,
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(buildLaunchIntent())
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.alarm_stop),
                    buildStopIntent(),
                ).build()
            )
            .build()
    }

    private fun buildLaunchIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            LAUNCH_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildStopIntent(): PendingIntent {
        val stopIntent = Intent(this, ReminderAlarmService::class.java).apply { action = ACTION_STOP }
        return PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun startAlarmOutput() {
        mediaPlayer?.release()
        mediaPlayer = null
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (alarmUri != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(alarmAudioAttributes())
                    setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    setDataSource(applicationContext, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (playbackFailure: Exception) {
                mediaPlayer?.release()
                mediaPlayer = null
                Log.e(LOG_TAG, "Could not start alarm sound", playbackFailure)
            }
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator?.let { alarmVibrator ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    alarmVibrator.vibrate(
                        vibrationEffect,
                        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alarmVibrator.vibrate(vibrationEffect, alarmAudioAttributes())
                }
            } else {
                @Suppress("DEPRECATION")
                alarmVibrator.vibrate(VIBRATION_PATTERN, 0, alarmAudioAttributes())
            }
        }
    }

    private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.alarm_channel_description)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "persistent_reminder_alarms"
        private const val ACTION_START = "io.github.sandroisu.threetimesaday.START_REMINDER_ALARM"
        private const val ACTION_STOP = "io.github.sandroisu.threetimesaday.STOP_REMINDER_ALARM"
        private const val EXTRA_TITLE = "alarm_title"
        private const val EXTRA_MESSAGE = "alarm_message"
        private const val EXTRA_NOTIFICATION_ID = "alarm_notification_id"
        private const val EXTRA_MAX_DURATION_MINUTES = "alarm_max_duration_minutes"
        private const val DEFAULT_MAX_DURATION_MINUTES = 10
        private const val MIN_DURATION_MINUTES = 1
        private const val MAX_DURATION_MINUTES = 10
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val FOREGROUND_NOTIFICATION_ID = 0x524d4e44
        private const val LAUNCH_REQUEST_CODE = 0x524d4e45
        private const val STOP_REQUEST_CODE = 0x524d4e46
        private const val LOG_TAG = "ReminderAlarmService"
        private val VIBRATION_PATTERN = longArrayOf(0L, 1_000L, 500L)

        fun start(
            context: Context,
            title: String,
            message: String,
            notificationId: String,
            maxDurationMinutes: Int,
        ) {
            val serviceIntent = Intent(context, ReminderAlarmService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_MAX_DURATION_MINUTES, maxDurationMinutes)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (startFailure: Exception) {
                Log.e(LOG_TAG, "Could not start reminder alarm", startFailure)
                showFallbackNotification(context, title, message, notificationId)
            }
        }

        private fun showFallbackNotification(
            context: Context,
            title: String,
            message: String,
            notificationId: String,
        ) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, MedicationReminderReceiver.CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
            manager.notify(
                reminderRequestCode(notificationId),
                builder
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }
}

package com.example.batterymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.util.Log

class BatteryMonitorService : Service() {
    private val CHANNEL_ID = "BatteryMonitorChannel"
    private val NOTIFICATION_ID = 1

    // Configurable parameters with default values
    private var generalThreshold: Double = 5.0
    private var criticalThreshold: Double = 7.0
    private var cooldown: Long = 60000L
    private var criticalRangeLower: Double = 15.0
    private var criticalRangeUpper: Double = 60.0
    private var chirpDuration: Long = 10000L // 10 seconds

    // Latest dynamic values for battery percentage and discharge rate
    private var currentBatteryPct: Float = 0f
    private var currentDischargeRate: Double = 0.0

    private var lastChirpTime: Long = 0
    private var previousBatteryLevel: Int? = null
    private var previousTimestamp: Long? = null

    private lateinit var mediaPlayer: MediaPlayer
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    // BroadcastReceiver for battery status updates from ACTION_BATTERY_CHANGED
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = it.getIntExtra("level", -1)
                    val scale = it.getIntExtra("scale", -1)
                    val batteryPct = level * 100 / scale.toFloat()
                    val currentTime = System.currentTimeMillis()
                    currentBatteryPct = batteryPct
                    Log.d("BatteryMonitorService", "Battery percentage: $batteryPct")

                    // Start or stop frequent polling depending on battery percentage
                    if (batteryPct in criticalRangeLower..criticalRangeUpper) {
                        startFrequentPolling()
                    } else {
                        stopFrequentPolling()
                    }

                    // General monitoring: calculate discharge rate
                    if (previousBatteryLevel != null && previousTimestamp != null) {
                        val timeDiff = (currentTime - previousTimestamp!!) / 60000.0
                        if (timeDiff > 0) {
                            val levelDiff = (previousBatteryLevel!! - batteryPct)
                            currentDischargeRate = levelDiff / timeDiff
                            Log.d("BatteryMonitorService", "General Discharge Rate: $currentDischargeRate%/min")
                            if (currentDischargeRate >= generalThreshold && (currentTime - lastChirpTime) >= cooldown) {
                                lastChirpTime = currentTime
                                playChirpSound()
                            }
                        }
                    }
                    previousBatteryLevel = batteryPct.toInt()
                    previousTimestamp = currentTime

                    // Update the ongoing notification with the new values
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize MediaPlayer with chirp sound and set to maximum volume
        mediaPlayer = MediaPlayer.create(this, R.raw.chirp_sound)
        mediaPlayer.setVolume(1.0f, 1.0f)
        // Register battery receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(batteryReceiver)
        stopFrequentPolling()
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Create and start the foreground service notification
    private fun startForegroundService() {
        createNotificationChannel()
        // Build initial notification with current values
        val notification = buildNotification().build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor Channel",
                NotificationManager.IMPORTANCE_HIGH // High importance for visibility
            )
            channel.description = "Notifications for battery monitoring service"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Build the notification using current batteryPct and dischargeRate values
    private fun buildNotification(): NotificationCompat.Builder {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)  // Persistent notification
            .setOnlyAlertOnce(true) // Update silently, no extra alerts on update
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setContentTitle("Battery Monitor Running")
            .setContentText("Battery: $currentBatteryPct%  Discharge Rate: ${"%.2f".format(currentDischargeRate)}%/min")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Battery: $currentBatteryPct%\n" +
                        "Discharge Rate: ${"%.2f".format(currentDischargeRate)}%/min\n" +
                        "General Threshold: $generalThreshold%/min\n" +
                        "Critical Threshold: $criticalThreshold%/min\n" +
                        "Cooldown: $cooldown ms\n" +
                        "Critical Range: $criticalRangeLower% - $criticalRangeUpper%\n" +
                        "Chirp Duration: $chirpDuration ms"
            ))
    }

    // Update the ongoing notification with the latest battery status
    private fun updateNotification() {
        val notification = buildNotification().build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // Helper function to broadcast battery status (if needed for UI updates)
    private fun broadcastBatteryStatus(batteryPct: Float, dischargeRate: Double) {
        val intent = Intent("com.example.batterymonitor.BATTERY_STATUS").apply {
            putExtra("batteryPct", batteryPct)
            putExtra("dischargeRate", dischargeRate)
        }
        sendBroadcast(intent)
    }

    // Plays the chirp sound for the configured chirpDuration at full volume
    private fun playChirpSound() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.isLooping = true
            mediaPlayer.start()
            handler.postDelayed({
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    mediaPlayer.seekTo(0)
                }
            }, chirpDuration)
        }
    }

    // Starts high-frequency polling (every 10 seconds) when battery is in the critical range
    private fun startFrequentPolling() {
        if (pollingRunnable == null) {
            pollingRunnable = object : Runnable {
                var previousCriticalBatteryLevel: Int? = null
                var previousCriticalTimestamp: Long? = null
                override fun run() {
                    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
                    val currentTime = System.currentTimeMillis()
                    Log.d("BatteryMonitorService", "Polling: Battery percentage: $batteryPct")
                    var dischargeRate = 0.0
                    if (previousCriticalBatteryLevel != null && previousCriticalTimestamp != null) {
                        val timeDiff = (currentTime - previousCriticalTimestamp!!) / 60000.0
                        if (timeDiff > 0) {
                            val levelDiff = (previousCriticalBatteryLevel!! - batteryPct)
                            dischargeRate = levelDiff / timeDiff
                            Log.d("BatteryMonitorService", "Rapid Polling Rate: $dischargeRate%/min")
                            if (dischargeRate >= criticalThreshold && (currentTime - lastChirpTime) >= cooldown) {
                                lastChirpTime = currentTime
                                playChirpSound()
                            }
                        }
                    }
                    previousCriticalBatteryLevel = batteryPct.toInt()
                    previousCriticalTimestamp = currentTime
                    // Also update the notification from polling
                    currentBatteryPct = batteryPct
                    currentDischargeRate = dischargeRate
                    updateNotification()
                    handler.postDelayed(this, 10000)
                }
            }
            handler.post(pollingRunnable!!)
        }
    }

    private fun stopFrequentPolling() {
        pollingRunnable?.let {
            handler.removeCallbacks(it)
            pollingRunnable = null
        }
    }

    // onStartCommand reads configuration parameters and checks for the "test" flag
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra("generalThreshold")) {
                generalThreshold = it.getDoubleExtra("generalThreshold", 5.0)
                criticalThreshold = it.getDoubleExtra("criticalThreshold", 7.0)
                cooldown = it.getLongExtra("cooldown", 60000L)
                criticalRangeLower = it.getDoubleExtra("criticalRangeLower", 15.0)
                criticalRangeUpper = it.getDoubleExtra("criticalRangeUpper", 60.0)
                chirpDuration = it.getLongExtra("chirpDuration", 10000L)
            }
            if (it.getBooleanExtra("test", false)) {
                playChirpSound()
            }
        }
        startForegroundService()
        return START_STICKY
    }
}

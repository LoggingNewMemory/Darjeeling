package com.kanagawa.yamada.darjeeling

import android.app.*
import android.content.*
import android.os.*
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo

class CaffeineService : Service() {
    companion object {
        const val ACTION_CYCLE = "ACTION_CYCLE"
        const val ACTION_UNLIMITED = "ACTION_UNLIMITED"
        const val ACTION_APPLY_CURRENT_STATE = "ACTION_APPLY_CURRENT_STATE"
        
        var currentState = 0 // 0=Off, 1=5m, 2=10m, 3=15m, 4=30m, 5=Unlimited
        var originalTimeout = 60000
        var isRunning = false
        var lastTapTime = 0L
        var endTime = 0L
    }

    private var countdownTimer: CountDownTimer? = null
    private var currentAppliedTimeout = -1

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                currentState = 0
                turnOff()
                updateTile()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY
        
        val action = intent.action
        
        if (!isRunning) {
            originalTimeout = try {
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            } catch (e: Exception) {
                60000 // default 1 min
            }
            isRunning = true
            
            val notification = buildNotification("Caffeine is starting...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
            
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenOffReceiver, filter)
        }
        
        if (action == ACTION_APPLY_CURRENT_STATE || action == ACTION_CYCLE || action == ACTION_UNLIMITED) {
            if (currentState == 0) {
                turnOff()
            } else {
                applyState(currentState)
            }
        }
        
        updateTile()
        return START_STICKY
    }

    private fun applyState(state: Int) {
        val timeoutMinutes = when(state) {
            1 -> 5
            2 -> 10
            3 -> 15
            4 -> 30
            else -> -1
        }
        
        if (state == 5) {
            setScreenTimeoutAsync(Int.MAX_VALUE)
            countdownTimer?.cancel()
            countdownTimer = null
            endTime = 0L
            updateNotification("Darjeeling is active (Unlimited)")
        } else if (timeoutMinutes > 0) {
            val ms = timeoutMinutes * 60 * 1000L
            endTime = System.currentTimeMillis() + ms
            setScreenTimeoutAsync(Int.MAX_VALUE)
            countdownTimer?.cancel()
            countdownTimer = object : CountDownTimer(ms, 60000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minLeft = millisUntilFinished / 60000 + 1
                    updateNotification("Darjeeling is active ($minLeft minutes left)")
                }
                override fun onFinish() {
                    currentState = 0
                    turnOff()
                    updateTile()
                }
            }.start()
            updateNotification("Darjeeling is active ($timeoutMinutes minutes left)")
        }
    }

    private fun turnOff() {
        setScreenTimeoutAsync(originalTimeout)
        countdownTimer?.cancel()
        countdownTimer = null
        endTime = 0L
        isRunning = false
        try {
            unregisterReceiver(screenOffReceiver)
        } catch(e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateTile() {
        TileService.requestListeningState(
            this,
            ComponentName(this, CaffeineTileService::class.java)
        )
        val intent = Intent(CaffeineTileService.ACTION_UPDATE_TILE)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun setScreenTimeoutAsync(timeoutMs: Int) {
        if (currentAppliedTimeout == timeoutMs) return
        currentAppliedTimeout = timeoutMs
        
        Thread {
            val prefs = getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
            val useRoot = prefs.getBoolean("use_root", false)
            if (useRoot) {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system screen_off_timeout $timeoutMs")).waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                if (Settings.System.canWrite(this)) {
                    try {
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, timeoutMs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "darjeeling_channel",
                "Darjeeling Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String = "Darjeeling is active"): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, "darjeeling_channel")
            .setContentTitle("Darjeeling")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, buildNotification(text))
    }
}

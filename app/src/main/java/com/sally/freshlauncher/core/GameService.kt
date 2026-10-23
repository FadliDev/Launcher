package com.sally.freshlauncher.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

/**
 * Menjalankan JVM di proses terpisah (:game) agar exit() dari JVM
 * hanya mematikan proses game, bukan UI launcher.
 */
class GameService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jreHome = intent?.getStringExtra(EXTRA_JRE_HOME)
        val gameDir = intent?.getStringExtra(EXTRA_GAME_DIR)
        val argv = intent?.getStringArrayExtra(EXTRA_ARGS)
        if (jreHome == null || gameDir == null || argv == null) {
            Log.e(TAG, "Argumen peluncuran tidak lengkap")
            stopSelf()
            return START_NOT_STICKY
        }

        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val appContext = applicationContext
        Thread({
            val rc = try {
                JreEnvironment.prepare(appContext, File(jreHome), File(gameDir))
                NativeJvm.launch(argv)
            } catch (t: Throwable) {
                Log.e(TAG, "Gagal menjalankan JVM", t)
                -1
            }
            Log.i(TAG, "Selesai dengan kode $rc")
            stopSelf()
        }, "jvm-main").start()

        return START_NOT_STICKY
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Game berjalan", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Minecraft sedang berjalan")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

    companion object {
        const val EXTRA_JRE_HOME = "jreHome"
        const val EXTRA_GAME_DIR = "gameDir"
        const val EXTRA_ARGS = "argv"
        private const val TAG = "FreshGame"
        private const val CHANNEL_ID = "game"
        private const val NOTIFICATION_ID = 1
    }
}

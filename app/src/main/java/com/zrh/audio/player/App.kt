package com.zrh.audio.player

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zrh.audio.player.db.AudioDataBase
import com.zrh.audio.player.service.AudioPlayService

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
class App : Application() {

    companion object {
        lateinit var Instance: App
            private set

        val database: AudioDataBase by lazy { AudioDataBase.getInstance(Instance) }
    }

    override fun onCreate() {
        super.onCreate()
        Instance = this
        initNotificationChannel(this, "AudioPlayer")
        startService(Intent(this, AudioPlayService::class.java))
    }

    private fun initNotificationChannel(context: Context, channelName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(context.packageName, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.enableLights(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        channel.setShowBadge(true)
        channel.setSound(null, null)
        channel.enableVibration(false)
        manager.createNotificationChannel(channel)
    }
}
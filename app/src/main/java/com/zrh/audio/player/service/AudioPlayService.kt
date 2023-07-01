package com.zrh.audio.player.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.zrh.audio.lib.AudioPlayer
import com.zrh.audio.player.App
import com.zrh.audio.player.R
import com.zrh.audio.player.db.dao.AudioDao
import com.zrh.audio.player.db.entity.AudioEntity
import com.zrh.audio.player.ui.MainActivity
import java.io.File

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
class AudioPlayService : Service(), IAudioPlayService, AudioPlayer.PlayListener {
    companion object {
        const val EVENT_ACTION = "com.zrh.audio.player.EVENT_ACTION"
        const val EVENT_TYPE = "EVENT_TYPE"
        const val EVENT_ERROR = 0
        const val EVENT_PROGRESS = 1
        const val EVENT_COMPLETE = 2
        const val EVENT_PLAY = 3
        const val EVENT_PAUSE = 4
        const val EVENT_PREPARING = 5

        const val AUDIO_PROGRESS = "AUDIO_PROGRESS"
        const val AUDIO_DURATION = "AUDIO_DURATION"
        const val AUDIO_ID = "AUDIO_ID"

        const val COMMAND = "COMMAND"
        const val CMD_PRE_AUDIO = 2
        const val CMD_NEXT_AUDIO = 3
        const val CMD_PLAY = 4
        const val CMD_PAUSE = 5
    }

    private val mPlayer: AudioPlayer by lazy { AudioPlayer(this.applicationContext).apply { setListener(this@AudioPlayService) } }
    private var mAudioEntity: AudioEntity? = null
    private val audioDao: AudioDao = App.database.getAudioDao()
    private var isForeground = false
    private val notificationId = 100
    private var duration: Int = 0
    private var progress: Int = 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mAudioEntity?.let { audioEntity ->
            when (intent.getIntExtra(COMMAND, 0)) {
                CMD_PRE_AUDIO -> {
                    playPreAudio(audioEntity.createAt)
                }
                CMD_NEXT_AUDIO -> {
                    playNextAudio(audioEntity.createAt)
                }
                CMD_PLAY -> {
                    play()
                }
                CMD_PAUSE -> {
                    pause()
                }
            }
        }
        return START_STICKY
    }

    private fun playNextAudio(createAt: Long) {
        val list = audioDao.getNextAudio(createAt)
        if (list.isEmpty()) return
        setAudio(list[0])
        play()
    }

    private fun playPreAudio(createAt: Long) {
        val list = audioDao.getPreAudio(createAt)
        if (list.isEmpty()) return
        setAudio(list[0])
        play()
    }

    private fun updateForegroundNotification() {
        val builder = NotificationCompat.Builder(this, packageName)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        builder.setAutoCancel(true).setOngoing(false)
        builder.setSmallIcon(R.mipmap.ic_launcher)

        val remoteView = RemoteViews(packageName, R.layout.notification_player).apply {
            setTextViewText(R.id.tvAudioName, mAudioEntity?.name ?: "")
            setOnClickPendingIntent(R.id.btnPre, getCommandIntent(CMD_PRE_AUDIO))
            setOnClickPendingIntent(R.id.btnNext, getCommandIntent(CMD_NEXT_AUDIO))
            if (isPlaying()) {
                setOnClickPendingIntent(R.id.btnPlay, getCommandIntent(CMD_PAUSE))
                setImageViewResource(R.id.btnPlay, R.mipmap.pause)
            } else {
                setOnClickPendingIntent(R.id.btnPlay, getCommandIntent(CMD_PLAY))
                setImageViewResource(R.id.btnPlay, R.mipmap.play)
            }

        }
        builder.setCustomBigContentView(remoteView)
        builder.setContent(remoteView)
        val clickIntent = Intent(this, MainActivity::class.java)
        builder.setContentIntent(PendingIntent.getActivity(this, 100, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT))

        if (isForeground) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notificationId, builder.build())
        } else {
            startForeground(notificationId, builder.build())
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return AudioPlayServiceProxy(this)
    }

    override fun getAudioId(): Long {
        return mAudioEntity?.id ?: 0
    }

    override fun isPlaying(): Boolean {
        return mPlayer.isPlaying
    }

    override fun getDuration(): Int {
        return duration
    }

    override fun getProgress(): Int {
        return progress
    }

    override fun setAudio(entity: AudioEntity) {
        mAudioEntity = entity
        mPlayer.setDataSource(entity.getDataSource())
        mPlayer.stop()
        duration = entity.duration
        progress = 0
    }

    override fun play() {
        mPlayer.play()
    }

    override fun pause() {
        mPlayer.pause()
    }

    override fun stop() {
        mAudioEntity = null
        mPlayer.stop()
        updateForegroundNotification()
    }

    override fun onPrepare() {
        updateForegroundNotification()
        sendBroadcast(getBroadcastIntent(EVENT_PREPARING))
    }

    override fun onStart() {
        updateForegroundNotification()
        sendBroadcast(getBroadcastIntent(EVENT_PLAY))
    }

    override fun onPause() {
        updateForegroundNotification()
        sendBroadcast(getBroadcastIntent(EVENT_PAUSE))
    }

    override fun onError(code: Int, msg: String?) {
        updateForegroundNotification()
        sendBroadcast(getBroadcastIntent(EVENT_ERROR))
        progress = 0
    }

    override fun onComplete() {
        updateForegroundNotification()
        sendBroadcast(getBroadcastIntent(EVENT_COMPLETE))
        progress = 0
    }

    override fun onProgress(duration: Int, progress: Int) {
        this.duration = duration
        this.progress = progress
        sendBroadcast(getBroadcastIntent(EVENT_PROGRESS).apply {
            putExtra(AUDIO_DURATION, duration)
            putExtra(AUDIO_PROGRESS, progress)
        })
    }

    private fun getBroadcastIntent(eventType: Int): Intent {
        return Intent(EVENT_ACTION).apply {
            putExtra(EVENT_TYPE, eventType)
            putExtra(AUDIO_ID, getAudioId())
        }
    }

    private fun getCommandIntent(command: Int): PendingIntent {
        val intent = Intent(this, AudioPlayService::class.java).apply { putExtra(COMMAND, command) }
        return PendingIntent.getService(this, command, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPlayer.release()
    }

    class AudioPlayServiceProxy(private val service: IAudioPlayService) : Binder(), IAudioPlayService by service
}
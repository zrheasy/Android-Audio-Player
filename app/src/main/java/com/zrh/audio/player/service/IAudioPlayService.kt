package com.zrh.audio.player.service

import com.zrh.audio.player.db.entity.AudioEntity

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
interface IAudioPlayService {
    fun getAudioId(): Long
    fun isPlaying():Boolean
    fun getDuration():Int
    fun getProgress():Int
    fun setAudio(entity: AudioEntity)
    fun play()
    fun pause()
    fun stop()
}
package com.zrh.audio.player.ui

import androidx.lifecycle.ViewModel
import com.zrh.audio.player.App
import com.zrh.audio.player.db.dao.AudioDao
import com.zrh.audio.player.db.entity.AudioEntity
import com.zrh.audio.player.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 *
 * @author zrh
 * @date 2023/6/30
 *
 */
class AudioViewModel : ViewModel() {
    private val audioDao: AudioDao = App.database.getAudioDao()

    fun getAudioList(): Flow<List<AudioEntity>> = flow {
        emit(audioDao.getAudioList())
    }.flowOn(Dispatchers.IO)

    fun insertAudio(entity: AudioEntity): Flow<AudioEntity> = flow {
        val id = audioDao.insertAudio(entity)
        emit(entity.copy(id = id))
    }.flowOn(Dispatchers.IO)

    fun deleteAudio(entity: AudioEntity): Flow<Any> = flow {
        audioDao.deleteAudio(entity.id)
        FileUtils.delete(entity.filePath)
        emit(true)
    }.flowOn(Dispatchers.IO)
}
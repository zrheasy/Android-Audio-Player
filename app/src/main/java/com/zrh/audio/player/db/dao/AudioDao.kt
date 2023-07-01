package com.zrh.audio.player.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zrh.audio.player.db.entity.AudioEntity

/**
 *
 * @author zrh
 * @date 2023/6/29
 *
 */
@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = AudioEntity::class)
    fun insertAudio(entity: AudioEntity): Long

    @Query("SELECT * FROM Audio ORDER BY createAt DESC")
    fun getAudioList(): List<AudioEntity>

    @Query("DELETE FROM Audio WHERE id=:id")
    fun deleteAudio(id: Long)

    @Query("SELECT * FROM Audio WHERE createAt>:createAt ORDER BY createAt LIMIT 1")
    fun getPreAudio(createAt: Long): List<AudioEntity>

    @Query("SELECT * FROM Audio WHERE createAt<:createAt ORDER BY createAt DESC LIMIT 1")
    fun getNextAudio(createAt: Long): List<AudioEntity>
}
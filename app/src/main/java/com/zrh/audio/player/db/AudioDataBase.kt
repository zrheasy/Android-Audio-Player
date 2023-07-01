package com.zrh.audio.player.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zrh.audio.player.db.dao.AudioDao
import com.zrh.audio.player.db.entity.AudioEntity

/**
 *
 * @author zrh
 * @date 2023/6/29
 *
 */
@Database(entities = [AudioEntity::class], version = 1)
abstract class AudioDataBase : RoomDatabase() {

    companion object {
        private const val DB_NAME = "audio.db"
        private lateinit var database: AudioDataBase

        @Synchronized
        fun getInstance(context: Context): AudioDataBase {
            if (!this::database.isInitialized) {
                database = Room.databaseBuilder(context, AudioDataBase::class.java, DB_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }

            return database
        }
    }

    abstract fun getAudioDao(): AudioDao

}
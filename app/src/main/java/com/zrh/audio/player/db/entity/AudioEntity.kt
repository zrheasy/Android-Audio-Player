package com.zrh.audio.player.db.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

/**
 *
 * @author zrh
 * @date 2023/6/29
 *
 */
@Entity(tableName = "Audio")
data class AudioEntity(
    val name: String,
    val filePath: String,
    val duration: Int,
    val createAt: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
) {
    fun getDataSource(): Uri {
        if (filePath.startsWith("/")) {
            return Uri.fromFile(File(filePath))
        }
        return Uri.parse(filePath)
    }
}


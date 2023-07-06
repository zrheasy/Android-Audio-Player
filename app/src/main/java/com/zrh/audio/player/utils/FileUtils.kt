package com.zrh.audio.player.utils

import java.io.File

/**
 *
 * @author zrh
 * @date 2023/7/1
 *
 */
object FileUtils {
    fun delete(filePath:String){
        try {
            val file = File(filePath)
            if (file.exists()){
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}
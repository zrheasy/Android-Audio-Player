package com.zrh.audio.player.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 *
 * @author zrh
 * @date 2023/7/1
 *
 */
object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri, dir: File, fileName: String): File? {
        try {
            if (!dir.exists()) {
                val succ = dir.mkdirs()
                if (!succ) return null
            }
            val file = File(dir, fileName)
            if (file.exists()) return file
            val fileInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileOutputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var byteRead: Int
            while (fileInputStream.read(buffer).also { byteRead = it } != -1) {
                fileOutputStream.write(buffer, 0, byteRead)
            }
            fileInputStream.close()
            fileOutputStream.flush()
            fileOutputStream.close()
            return file
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

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
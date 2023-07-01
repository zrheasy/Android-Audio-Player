package com.zrh.audio.lib;

import android.media.MediaMetadataRetriever;

import java.io.File;

/**
 * @author zrh
 * @date 2023/7/1
 */
public class AudioUtils {

    /**
     * duration = size*8/bitrate
     *
     * @param file audio file
     * @return duration in ms
     */
    public static int getDuration(File file) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            long bitRate = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            long size = file.length();
            int duration = (int) (size * 8 * 1000f / bitRate);
            retriever.release();
            return duration;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }
}

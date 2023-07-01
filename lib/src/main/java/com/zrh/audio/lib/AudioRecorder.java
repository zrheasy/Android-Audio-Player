package com.zrh.audio.lib;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;

import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author zrh
 * @date 2023/6/29
 */
public class AudioRecorder implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
    private static final int MIN_DURATION = 500;
    private static final int TIMER_PERIOD = 50;
    public static final int ERROR_RECORD = 4001;
    public static final int ERROR_DURATION_SO_SHORT = 4002;


    private RecordListener mListener;
    private final String mAudioDir;
    private int mMaxDuration = 60 * 1000;

    private MediaRecorder mRecorder;
    private Timer recordTimer;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private File output;
    private long startTime;

    public AudioRecorder(String audioDir) {
        this.mAudioDir = audioDir;
    }

    public void setMaxDuration(int maxDuration) {
        this.mMaxDuration = maxDuration;
    }

    public void setListener(RecordListener listener) {
        this.mListener = listener;
    }

    public void start() {
        release();
        try {
            output = createNewFile();

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(output.getAbsolutePath());
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setMaxDuration(mMaxDuration);
            mRecorder.setOnInfoListener(this);
            mRecorder.setOnErrorListener(this);
            mRecorder.prepare();
            mRecorder.start();

            startTimer();

            startTime = System.currentTimeMillis();
        } catch (Throwable e) {
            e.printStackTrace();
            notifyError(ERROR_RECORD, "start error");
            cancel();
        }
    }

    private void startTimer() {
        recordTimer = new Timer();
        recordTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onRecording();
            }
        }, TIMER_PERIOD, TIMER_PERIOD);
    }

    private void onRecording() {
        if (mListener == null || mRecorder == null) return;
        long recordDuration = System.currentTimeMillis() - startTime;
        mHandler.post(() -> {
            double volume = 0;
            try {
                double ratio = mRecorder.getMaxAmplitude() / 270.0;
                if (ratio > 1) volume = 20 * Math.log10(ratio);
            } catch (Exception ignored) {}

            notifyRecording((int) recordDuration, (int) volume);
        });
    }

    public void stop() {
        long recordDuration = System.currentTimeMillis() - startTime;
        if (recordDuration < MIN_DURATION) {
            notifyError(ERROR_DURATION_SO_SHORT, "duration so short!");
            cancel();
        } else {
            notifyComplete((int) recordDuration);
            release();
        }
    }

    public void cancel() {
        if (output != null && output.exists()) {
            try {
                output.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        release();
    }

    private void release() {
        mHandler.removeCallbacks(null);

        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mRecorder = null;
        }

        if (recordTimer != null) {
            recordTimer.cancel();
            recordTimer = null;
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        notifyError(ERROR_RECORD, "record error");
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            stop();
        }
    }

    private void notifyError(int code, String msg) {
        if (mListener != null) mListener.onError(code, msg);
    }

    private void notifyComplete(int duration) {
        if (mListener != null) mListener.onComplete(duration, output);
    }

    private void notifyRecording(int duration, int volume) {
        if (mListener != null) mListener.onRecording(duration, volume);
    }

    private File createNewFile() {
        File dir = new File(mAudioDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = System.currentTimeMillis() / 1000 + ".m4a";
        return new File(mAudioDir, fileName);
    }

    public interface RecordListener {
        void onError(int code, @NonNull String msg);

        void onRecording(int duration, int volume);

        void onComplete(int duration, @NonNull File output);
    }
}

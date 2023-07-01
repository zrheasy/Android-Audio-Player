package com.zrh.audio.lib;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author zrh
 * @date 2023/6/30
 */
public class AudioPlayer {
    public static final int ERROR_PREPARE = 4001;
    public static final int ERROR_PLAY = 4002;
    private static final int TIMER_PERIOD = 50;

    // 播放器状态
    public static final int STATE_UNPREPARED = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;

    private final Context mContext;
    private PlayListener mListener;
    private MediaPlayer mPlayer;
    private Timer mTimer;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Uri mDataSource;
    private int mDuration = 0;
    private float mVolume = 0.74f;
    private int mState = STATE_UNPREPARED;

    public AudioPlayer(Context context) {
        this.mContext = context;
    }

    public void setListener(PlayListener listener) {
        this.mListener = listener;
    }

    public void setDataSource(Uri uri) {
        this.mDataSource = uri;
    }

    public void setVolume(float volume) {
        this.mVolume = volume;
    }

    public boolean isPlaying() {
        return mPlayer != null && mState == STATE_PLAYING;
    }

    public boolean isPrepared() {
        return mPlayer != null && mState >= STATE_PREPARED;
    }

    public boolean isPreparing() {
        return mPlayer != null && mState == STATE_PREPARING;
    }

    public void play() {
        if (isPrepared()) {
            internalStart();
        } else {
            internalPlay();
        }
    }

    private void internalPlay() {
        if (mPlayer == null) mPlayer = new MediaPlayer();
        stop();
        try {
            mPlayer.setDataSource(mContext, mDataSource);
            mPlayer.setVolume(mVolume, mVolume);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.prepareAsync();
            mPlayer.setOnPreparedListener(mp -> {
                mState = STATE_PREPARED;
                mDuration = mPlayer.getDuration();
                internalStart();
            });
            mPlayer.setOnErrorListener((mp, what, extra) -> {
                stop();
                notifyError(ERROR_PLAY, "play error:" + what + " - " + extra);
                return true;
            });
            mPlayer.setOnCompletionListener(mp -> {
                mState = STATE_PREPARED;
                notifyComplete();
            });
            mState = STATE_PREPARING;
            notifyPrepare();
        } catch (Throwable e) {
            e.printStackTrace();
            stop();
            notifyError(ERROR_PREPARE, "prepare error");
        }
    }

    private void internalStart() {
        try {
            mPlayer.start();

            startTimer();

            mState = STATE_PLAYING;

            notifyStart();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            stop();
            notifyError(ERROR_PLAY, "play error");
        }
    }

    public void pause() {
        if (mPlayer != null) {
            try {
                mPlayer.pause();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        stopTimer();
        mState = STATE_PAUSED;
        notifyPause();
    }

    public void stop() {
        if (mPlayer != null) {
            try {
                mPlayer.stop();
                mPlayer.reset();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        stopTimer();
        mState = STATE_UNPREPARED;
    }

    public void release() {
        stop();

        if (mPlayer != null) {
            try {
                mPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPlayer = null;
        }
    }

    private void startTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onProgress();
            }
        }, TIMER_PERIOD, TIMER_PERIOD);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    private void onProgress() {
        try {
            if (mPlayer != null && mPlayer.isPlaying()) {
                int progress = mPlayer.getCurrentPosition();// IllegalStateException
                mHandler.post(() -> notifyProgress(progress));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyPrepare() {
        if (mListener != null) mListener.onPrepare();
    }

    private void notifyStart() {
        if (mListener != null) mListener.onStart();
    }

    private void notifyPause() {
        if (mListener != null) mListener.onPause();
    }

    private void notifyError(int code, String msg) {
        if (mListener != null) mListener.onError(code, msg);
    }

    private void notifyComplete() {
        if (mListener != null) mListener.onComplete();
    }

    private void notifyProgress(int progress) {
        if (mListener != null) mListener.onProgress(mDuration, progress);
    }

    public interface PlayListener {
        void onPrepare();

        void onStart();

        void onPause();

        void onError(int code, String msg);

        void onComplete();

        /**
         * @param duration the duration in ms
         * @param progress the progress in ms
         */
        void onProgress(int duration, int progress);
    }
}

package com.example.harry.aug;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * Created by harry on 15/8/2.
 */
public class AUGManager {
    private static final String TAG = "AUGManager";
    private static final int UPDATE_INTERVAL = 50;

    public static final int UPDATE_FAIL = -1;

    private AUGActivity activity;
    private State state;
    private MediaExtractor mediaExtractor;
    private AUGComponent[] AUGComponents;
    private Handler handler;
    private TimeUpdater timeUpdater;
    private Destroyer destroyer;

    public AUGManager(AUGActivity activity, AUGComponent[] AUGComponents) {
        this.activity = activity;
        this.state = State.STATE_STOPPED;
        this.mediaExtractor = new MediaExtractor();
        this.AUGComponents = AUGComponents;

        this.handler = new Handler(Looper.getMainLooper());
        this.timeUpdater = new TimeUpdater();
        this.destroyer = new Destroyer();

        for(int i = 0; i < AUGComponents.length; i++) {
            AUGComponents[i].setAugManager(this);
            AUGComponents[i].setNext((i != AUGComponents.length - 1)? AUGComponents[i + 1] : null);
        }
    }

    //

    public State getState() {
        return state;
    }

    public MediaExtractor getMediaExtractor() {
        return mediaExtractor;
    }

    public void setDataSource(String dataSource) {
        // Media Extractor
        try {
            mediaExtractor.setDataSource(dataSource);
        } catch (Exception e) {
            Log.e(TAG, "Media extractor error: " + e.getMessage());
            return;
        }

        Log.d(TAG, "Source info: "
                + "dataSource = " + dataSource);

        // Media Format
        MediaFormat mediaFormat;
        try {
            mediaFormat = mediaExtractor.getTrackFormat(0);

            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if(!mime.startsWith("audio")) {
                Log.e(TAG, "Media format error: media not audio");
                return;
            }
        } catch(Exception e) {
            Log.e(TAG, "Media format error: " + e.getMessage());
        }

        // TODO: remove this in the future

        LinearLayout playerTimeLayout = (LinearLayout) activity.findViewById(R.id.player);
        if(playerTimeLayout != null) {
            TextView playerTimeView = new TextView(activity);
            playerTimeLayout.addView(playerTimeView);
            float time = (float) mediaExtractor.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION) / TimeUnit.SECONDS.toMicros(1);
            playerTimeView.setText(String.format("All: %.2f s", time));
        }
    }

    //

    public void prepare() {
        for(AUGComponent AUGComponent : AUGComponents) {
            AUGComponent.create();
            AUGComponent.start();
        }
    }

    public void start() {
        switch(state) {
            case STATE_STOPPED:
                state = State.STATE_PLAYING;
                for(AUGComponent AUGComponent : AUGComponents) {
                    (new Thread(AUGComponent)).start();
                }
                timeUpdater.initiate(true);
                break;
            case STATE_PAUSED:
                state = State.STATE_PLAYING;
                synchronized(this) {
                    for(AUGComponent AUGComponent : AUGComponents) AUGComponent.notify();
                }
                timeUpdater.initiate(true);
                break;
            default:
                break;
        }
    }

    public void pause() {
        Log.d(TAG, "Pause");
        if(state == State.STATE_PLAYING) {
            state = State.STATE_PAUSED;

            timeUpdater.terminate();
            timeUpdater.initiate(false);
        }
    }

    public void stop() {
        Log.d(TAG, "Stop");
        if(state != State.STATE_STOPPED) {
            state = State.STATE_STOPPED;

            destroyer.initiate();
        }
    }

    public void seek(long time) {
        mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    public long seek() {
        return AUGComponents[0].getTime();
    }

    //

    public enum State {
        STATE_STOPPED,
        STATE_PLAYING,
        STATE_PAUSED
    }

    private class TimeUpdater implements Runnable {
        private TextView[] playerTimeViews;
        private int length;
        private boolean loop;

        public TimeUpdater() {
            this.length = AUGManager.this.AUGComponents.length;
            this.playerTimeViews = new TextView[length];
        }

        public void initiate(boolean loop) {
            Log.d(TAG, "Initiate TimeUpdater with loop = " + String.valueOf(loop));
            this.loop = loop;
            AUGManager.this.handler.post(this);
        }

        public void terminate() {
            AUGManager.this.handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            LinearLayout playerTimeLayout = (LinearLayout) activity.findViewById(R.id.player);

            if(playerTimeLayout == null) {
                return;
            }

            for (int i = 0; i < length; i++) {
                long timeUs = AUGManager.this.AUGComponents[i].getTime();
                if(timeUs == AUGManager.UPDATE_FAIL) {
                    continue;
                }
                float time = (float) timeUs / TimeUnit.SECONDS.toMicros(1);
                if(playerTimeViews[i] == null) {
                    playerTimeViews[i] = new TextView(AUGManager.this.activity);
                    playerTimeViews[i].setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    playerTimeLayout.addView(playerTimeViews[i]);
                }
                playerTimeViews[i].setText(String.format("AUGComponent %d: %.2f s", i, time));
            }

            if(loop) {
                AUGManager.this.handler.postDelayed(timeUpdater, UPDATE_INTERVAL);
            }
        }
    }

    private class Destroyer implements Runnable {
        public void initiate() {
            Log.d(TAG, "Initiate Destroyer");
            AUGManager.this.handler.post(this);
        }

        @Override
        public void run() {
            for(AUGComponent AUGComponent : AUGComponents) {
                AUGComponent.destroy();
            }
        }
    }
}

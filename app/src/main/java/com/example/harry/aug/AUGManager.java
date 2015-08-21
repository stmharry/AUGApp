package com.example.harry.aug;

import android.app.Activity;
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

    private Activity activity;
    private State state;
    private MediaExtractor mediaExtractor;
    private Component[] components;
    private Handler handler;
    private TimeUpdater timeUpdater;

    public AUGManager(Activity activity) {
        this.activity = activity;
        this.state = State.STATE_STOPPED;
        this.mediaExtractor = new MediaExtractor();
        this.components = new Component[]{
                new Decoder(this),
                new PhaseVocoder(this),
                new AudioPlayer(this)};

        this.handler = new Handler(Looper.getMainLooper());
        this.timeUpdater = new TimeUpdater();

        for(int i = 0; i < components.length; i++) {
            components[i].setNext((i != components.length - 1)? components[i + 1] : null);
        }
    }

    /////////////
    // UTILITY //
    /////////////

    public State getState() {
        return state;
    }

    public void setState(State state) {
        String str;
        switch(state) {
            case STATE_STOPPED: str = "STATE_STOPPED"; break;
            case STATE_PLAYING: str = "STATE_PLAYING"; break;
            case STATE_PAUSED: str = "STATE_PAUSED"; break;
            default: str = "STATE_ERROR"; break;
        }
        Log.d(TAG, "State = " + str);

        this.state = state;
    }

    public MediaExtractor getMediaExtractor() {
        return mediaExtractor;
    }

    public Component[] getComponents() {
        return components;
    }

    public Component getComponent(int i) {
        return components[i];
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
    }

    /////////////
    // PROCESS //
    /////////////

    public void prepare() {
        for(Component component: components) {
            component.initializeElement();
            component.initializeBuffer();
        }
    }

    public void start() {
        switch(state) {
            case STATE_STOPPED:
                state = State.STATE_PLAYING;
                for(Component component: components) {
                    (new Thread(component)).start();
                }
                timeUpdater.initiate(true);
                break;
            case STATE_PLAYING:
                break;
            case STATE_PAUSED:
                state = State.STATE_PLAYING;

                synchronized(this) {
                    for(Component component: components) component.notify();
                }
                timeUpdater.initiate(true);
                break;
        }
    }

    public void pause() {
        state = State.STATE_PAUSED;

        handler.removeCallbacks(timeUpdater);
        timeUpdater.initiate(false);
    }

    public void stop() {
        if(state != State.STATE_STOPPED) {
            state = State.STATE_STOPPED;
            handler.removeCallbacks(timeUpdater);
            timeUpdater.initiate(false);
        }
        // TODO: bug
    }

    public void seek(long time) {
        mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        state = State.STATE_SEEK;
    }

    public long seek() {
        return components[0].getTime();
    }

    /////////////////
    // INNER CLASS //
    /////////////////

    public enum State {
        STATE_STOPPED,
        STATE_PLAYING,
        STATE_PAUSED,
        STATE_SEEK
    }

    private class TimeUpdater implements Runnable {
        private TextView[] playerTimeViews;
        private int length;
        private boolean loop;

        public TimeUpdater() {
            this.length = AUGManager.this.getComponents().length;
            this.playerTimeViews = new TextView[length];
        }

        public void initiate(boolean loop) {
            this.loop = loop;
            AUGManager.this.handler.postDelayed(this, UPDATE_INTERVAL);
        }

        @Override
        public void run() {
            LinearLayout playerTimeLayout = (LinearLayout) activity.findViewById(R.id.player);

            for(int i = 0; i < length; i++) {
                float time = (float) AUGManager.this.getComponent(i).getTime() / TimeUnit.SECONDS.toMicros(1);
                if(playerTimeViews[i] == null) {
                    playerTimeViews[i] = new TextView(activity);
                    playerTimeViews[i].setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    playerTimeLayout.addView(playerTimeViews[i], i);
                }
                playerTimeViews[i].setText(String.format("Component %d: %.2f s", i, time));
            }

            if(loop) {
                AUGManager.this.handler.postDelayed(timeUpdater, UPDATE_INTERVAL);
            }
        }
    }
}

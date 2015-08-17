package com.example.harry.aug;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by harry on 15/8/2.
 */
public class AUGManager {
    private static final String TAG = "AUGManager";

    private State state;
    private MediaExtractor mediaExtractor;
    private Component[] components;

    /////////////////
    // INNER CLASS //
    /////////////////

    public AUGManager() {
        state = State.STATE_STOPPED;
        mediaExtractor = new MediaExtractor();
        components = new Component[]{new Decoder(this), new PhaseVocoder(this), new AudioPlayer(this)};

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
                for(Component component: components) (new Thread(component)).start();
                break;
            case STATE_PLAYING:
                break;
            case STATE_PAUSED:
                state = State.STATE_PLAYING;

                synchronized(this) {
                    for(Component component: components) component.notify();
                }
                break;
        }
    }

    public void pause() {
        if(state == State.STATE_PLAYING) {
            state = State.STATE_PAUSED;
        }
    }

    public void stop() {
        state = State.STATE_STOPPED;
    }

    public void seek(long time) {
        mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        state = State.STATE_SEEK;
    }

    public long seek() {
        return components[0].getTime();
    }

    public enum State {
        STATE_STOPPED,
        STATE_PLAYING,
        STATE_PAUSED,
        STATE_SEEK
    }
}

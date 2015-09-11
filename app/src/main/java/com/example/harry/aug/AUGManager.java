package com.example.harry.aug;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by harry on 15/8/2.
 */
public class AUGManager {
    private static final String TAG = "AUGManager";
    private static final boolean MULTI_THREAD = true;

    public static final int UPDATE_FAIL = -1;

    private AUGActivity augActivity;
    private AUGFragment augFragment;
    private State state;
    private Song song;
    private MediaExtractor mediaExtractor;
    private AUGComponent[] AUGComponents;
    private Handler handler;
    private AUGTimeUpdater augTimeUpdater;
    private Destroyer destroyer;

    public AUGManager(AUGActivity augActivity, AUGFragment augFragment, AUGComponent[] AUGComponents, AUGTimeUpdater augTimeUpdater) {
        this.augActivity = augActivity;
        this.augFragment = augFragment;
        this.state = State.STATE_STOPPED;
        this.mediaExtractor = new MediaExtractor();
        this.AUGComponents = AUGComponents;

        this.handler = new Handler(Looper.getMainLooper());
        this.augTimeUpdater = augTimeUpdater;
        this.augTimeUpdater.setAUGActivity(augActivity);
        this.augTimeUpdater.setAUGManager(this);
        this.destroyer = new Destroyer();

        for(int i = 0; i < AUGComponents.length; i++) {
            AUGComponents[i].setAugManager(this);
            AUGComponents[i].setNext((i != AUGComponents.length - 1)? AUGComponents[i + 1] : null);
        }
    }

    //

    public AUGActivity getAUGActivity() {
        return augActivity;
    }

    public AUGFragment getAugFragment() {
        return augFragment;
    }

    public State getState() {
        return state;
    }

    public MediaExtractor getMediaExtractor() {
        return mediaExtractor;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setSong(Song song) {
        this.song = song;
        String dataSource = song.getData();

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

    //

    public void prepare() {
        for(AUGComponent AUGComponent: AUGComponents) {
            AUGComponent.create();
            AUGComponent.start();
        }
    }

    public void start() {
        switch(state) {
            case STATE_STOPPED:
                state = State.STATE_PLAYING;
                for(AUGComponent augComponent: AUGComponents) {
                    (new Thread(augComponent)).start();
                }
                augTimeUpdater.setLoop(true);
                handler.post(augTimeUpdater);
                break;
            case STATE_PAUSED:
                state = State.STATE_PLAYING;
                synchronized(this) {
                    for(AUGComponent augComponent: AUGComponents) {
                        augComponent.notify();
                    }
                }
                augTimeUpdater.setLoop(true);
                handler.post(augTimeUpdater);
                break;
            default:
                break;
        }
    }

    public void pause() {
        if(state == State.STATE_PLAYING) {
            state = State.STATE_PAUSED;

            handler.removeCallbacks(augTimeUpdater);
            //augTimeUpdater.setLoop(false);
            //handler.post(augTimeUpdater);
        }
    }

    public void stop() {
        if(state != State.STATE_STOPPED) {
            state = State.STATE_STOPPED;
            destroyer.run();
            //handler.post(destroyer);
        }
    }

    public void seek(long time) {
        mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    public long getAllTime() {
        return mediaExtractor.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION);
    }

    public long getTime() {
        return AUGComponents[0].getTime();
    }

    //

    public enum State {
        STATE_STOPPED,
        STATE_PLAYING,
        STATE_PAUSED
    }

    private class Destroyer implements Runnable {
        @Override
        public void run() {
            for(AUGComponent AUGComponent: AUGComponents) {
                AUGComponent.destroy();
            }
        }
    }
}

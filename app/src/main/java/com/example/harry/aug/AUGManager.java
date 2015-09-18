package com.example.harry.aug;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by harry on 15/8/2.
 */
public class AUGManager {
    private static final String TAG = "AUGManager";

    public static final int UPDATE_FAIL = -1;
    public static final int COMPONENT_STOP = 1;

    private AUGActivity augActivity;
    private AUGFragment augFragment;
    private State state;
    private Song song;
    private MediaExtractor mediaExtractor;
    private AUGComponent[] augComponents;
    private Handler handler;
    private AUGTimeUpdater augTimeUpdater;
    private long time;

    public AUGManager(AUGActivity augActivity, AUGFragment augFragment, AUGComponent[] augComponents, AUGTimeUpdater augTimeUpdater) {
        this.augActivity = augActivity;
        this.augFragment = augFragment;
        this.state = State.STATE_STOPPED;
        this.mediaExtractor = new MediaExtractor();
        this.augComponents = augComponents;

        this.handler = new ComponentHandler(Looper.getMainLooper());
        this.augTimeUpdater = augTimeUpdater;
        this.augTimeUpdater.setAUGActivity(augActivity);
        this.augTimeUpdater.setAUGManager(this);

        for(int i = 0; i < augComponents.length; i++) {
            augComponents[i].setAugManager(this);
            augComponents[i].setHandler(handler);
            augComponents[i].setNext((i != augComponents.length - 1)? augComponents[i + 1] : null);
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

    public Song getSong() {
        return song;
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
            mediaExtractor = new MediaExtractor();
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

    private void syncNotify() {
        for(AUGComponent augComponent: augComponents) {
            augComponent.syncNotify();
        }
    }

    public void prepare() {
        for(AUGComponent augComponent: augComponents) {
            augComponent.create();
            augComponent.start();
        }
    }

    public void start() {
        switch(state) {
            case STATE_STOPPED:
                state = State.STATE_PLAYING;
                for(AUGComponent augComponent: augComponents) {
                    (new Thread(augComponent)).start();
                }
                augTimeUpdater.setLoop(true);
                handler.post(augTimeUpdater);
                break;
            case STATE_PAUSED:
                state = State.STATE_PLAYING;
                syncNotify();
                augTimeUpdater.setLoop(true);
                handler.post(augTimeUpdater);
                break;
            default:
                break;
        }
    }

    public void pause() {
        Log.d(TAG, "Pause");
        if(state == State.STATE_PLAYING) {
            state = State.STATE_PAUSED;

            handler.removeCallbacks(augTimeUpdater);
        }
    }

    public void stop() {
        Log.d(TAG, "Stop");
        switch(state) {
            case STATE_STOPPED:
                break;
            case STATE_PLAYING:
                state = State.STATE_STOPPED;
                break;
            case STATE_PAUSED:
                state = State.STATE_STOPPED;
                syncNotify();
                break;
        }
    }

    public void destroy() {
        state = State.STATE_STOPPED;
        for(AUGComponent augComponent: augComponents) {
            augComponent.destroy();
        }
    }

    public void seek(long time) {
        mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    public long getAllTime() {
        return mediaExtractor.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION);
    }

    public long getTime() {
        return time;
    }

    public long updateTime() {
        return (time = augComponents[0].getTime()); // TODO: getTime() results in 0 sometimes... no lock?
    }

    //

    public enum State {
        STATE_STOPPED,
        STATE_PLAYING,
        STATE_PAUSED
    }

    private class ComponentHandler extends Handler {
        private int stoppedComponentCount;

        public ComponentHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if(message.what == COMPONENT_STOP) {
                if(++stoppedComponentCount == augComponents.length) {
                    stoppedComponentCount = 0;

                    boolean isForcedStop = (state == State.STATE_STOPPED);
                    AUGManager.this.destroy();

                    if(!isForcedStop) {
                        augFragment.onAUGManagerDestroy();
                    }
                }
            }
        }
    }
}

package com.example.harry.aug;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by harry on 15/8/2.
 */
public abstract class Component implements Runnable {
    protected static final int BUFFER_QUEUE_CAPACITY = 16;
    protected static final int BYTE_PER_SHORT = Short.SIZE / 8;
    protected static final long TIMEOUT_US = 1000;
    protected static final long S_TO_US = TimeUnit.SECONDS.toMicros(1);
    protected String TAG;
    protected AUGManager augManager;
    protected Component prev;
    protected Component next;
    protected ArrayBlockingQueue<byte[]> inputQueue;

    protected boolean inputEOS;
    protected boolean outputEOS;
    protected boolean last;

    protected MediaFormat mediaFormat;
    protected String mime;
    protected int sampleRate;
    protected int numChannel;
    protected long duration;

    //

    public Component(String TAG) {
        this.TAG = TAG;
    }

    public void setAugManager(AUGManager augManager) {
        this.augManager = augManager;
    }

    public void setNext(Component next) {
        if(next != null) {
            this.next = next;
            this.next.prev = this;
        } else {
            this.last = true;
        }
    }

    public void setInputEOS() {
        this.inputEOS = true;
        Log.d(TAG, "Input EOS");
    }

    public void setOutputEOS() {
        this.outputEOS = true;
        Log.d(TAG, "Output EOS");

        if(next != null) {
            next.setInputEOS();
        }
    }

    public synchronized long getTime() {
        return 0;
    }

    //

    public void queueInput(byte[] buffer) {
        try {
            inputQueue.put(buffer);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public byte[] dequeueInput(long timeoutUs) {
        byte[] buffer;
        try {
            buffer = inputQueue.poll(timeoutUs, TimeUnit.MICROSECONDS);
        } catch(InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return buffer;
    }

    //

    public void create() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        // Media
        MediaExtractor mediaExtractor = augManager.getMediaExtractor();
        mediaFormat = mediaExtractor.getTrackFormat(0);
        mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        numChannel = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);

        Log.d(TAG, "Media info: "
                + "mime = " + mime + ", "
                + "sampleRate = " + String.valueOf(sampleRate) + ", "
                + "numChannel = " + String.valueOf(numChannel) + ", "
                + "duration = " + String.valueOf(duration));
    }

    public void start() {
        inputQueue = new ArrayBlockingQueue<byte[]>(BUFFER_QUEUE_CAPACITY);
    }

    public boolean loop() {
        return (!outputEOS
                && (augManager.getState() != AUGManager.State.STATE_STOPPED));
    }

    public void operation() {
        synchronized(this) {
            switch(augManager.getState()) {
                case STATE_PAUSED:
                    try {
                        wait();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void stop() {
        Log.d(TAG, "Stop");
        if(last) {
            augManager.pause();
            augManager.stop();
        }
    }

    public void destroy() {
        Log.d(TAG, "Destroy");
    }

    //

    @Override
    public void run() {
        while(loop()) {
            operation();
        }
        stop();
    }
}

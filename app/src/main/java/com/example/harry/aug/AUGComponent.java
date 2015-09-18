package com.example.harry.aug;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by harry on 15/8/2.
 */
public abstract class AUGComponent implements Runnable {
    protected static final int BUFFER_QUEUE_CAPACITY = 16;
    protected static final int BYTE_PER_SHORT = Short.SIZE / 8;
    protected static final long TIMEOUT_US = 1000;
    protected static final long SLEEP_MS = 10;
    protected static final long S_TO_US = TimeUnit.SECONDS.toMicros(1);

    protected String TAG;
    protected AUGManager augManager;
    protected Handler handler;
    protected AUGComponent prev;
    protected AUGComponent next;
    protected ArrayBlockingQueue<byte[]> inputQueue;

    protected boolean inputEOS;
    protected boolean outputEOS;

    protected MediaFormat mediaFormat;
    protected String mime;
    protected int sampleRate;
    protected int numChannel;
    protected long duration;

    private int totalInSize;
    private int totalOutSize;
    private long sleepTime;

    //

    public AUGComponent(String TAG) {
        this.TAG = TAG;
    }

    public void setAugManager(AUGManager augManager) {
        this.augManager = augManager;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setNext(AUGComponent next) {
        if(next != null) {
            this.next = next;
            this.next.prev = this;
        }
    }

    public void setInputEOS() {
        this.inputEOS = true;
        Log.d(TAG, "Input EOS");
    }

    public void setOutputEOS() {
        this.outputEOS = true;

        String str = "[Output EOS]";
        str += ("[totalInSize = " + totalInSize + "]");
        str += ("[totalOutSize = " + totalOutSize + "]");
        str += ("[sleepTime = " + sleepTime + "]");
        Log.d(TAG, str);

        if(next != null) {
            next.setInputEOS();
        }
    }

    public synchronized long getTime() {
        return 0;
    }

    //

    public boolean inputQueueHasRemaining() {
        return (inputQueue.remainingCapacity() > 0);
    }

    public void queueInput(byte[] buffer) {
        try {
            inputQueue.put(buffer);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        prev.totalOutSize += buffer.length;
    }

    public byte[] dequeueInput(long timeoutUs) {
        byte[] buffer;
        try {
            buffer = inputQueue.poll(timeoutUs, TimeUnit.MICROSECONDS);
        } catch(InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        if(buffer != null) {
            totalInSize += buffer.length;
        }
        return buffer;
    }

    //

    public void create() { // TODO: create / start the same?
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        inputEOS = false;
        outputEOS = false;
        totalInSize = 0;
        totalOutSize = 0;
        sleepTime = 0;

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
        inputQueue = new ArrayBlockingQueue<>(BUFFER_QUEUE_CAPACITY);
    }

    public void pause() {}

    public synchronized void syncNotify() {
        notify();
    }

    public boolean loop() {
        return (!outputEOS && (augManager.getState() != AUGManager.State.STATE_STOPPED));
    }

    public void operation() { // TODO: simplify this
        switch(augManager.getState()) {
            case STATE_PAUSED:
                pause();
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }

        if(next != null) {
            while(!next.inputQueueHasRemaining()) {
                try {
                    sleepTime += SLEEP_MS;
                    Thread.sleep(SLEEP_MS);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        handler.sendEmptyMessage(AUGManager.COMPONENT_STOP);
    }

    public void destroy() {}

    //

    @Override
    public void run() {
        while(loop())
            operation();
        stop();
    }
}

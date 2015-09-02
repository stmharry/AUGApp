package com.example.harry.aug;

import android.util.Log;

/**
 * Created by harry on 15/8/29.
 */
public class LabROSAAnalyzer extends Analyzer {
    private static final String TAG = "LabROSAAnalyzer";

    private static final int BUFFER_CAPACITY = 32768;
    private static final float FFT_TIME = 0.05f;
    private static final int HOP_RATIO = 8;

    private int frame;

    //

    public static LabROSAAnalyzer newInstance() {
        return new LabROSAAnalyzer();
    }

    public LabROSAAnalyzer() {
        super(TAG, BUFFER_CAPACITY, FFT_TIME, HOP_RATIO);
    }

    @Override
    public synchronized long getTime() {
        return 0;
    }

    //

    @Override
    public void create() {
        super.create();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void operation() {
        super.operation();

        int floorLeftSample = frame * fftHopSize + 1;

        getInput(floorLeftSample + fftFrameSize);

        for(int i = 0; i < numChannel; i++) {
            float[] in = getFrame(inFloatBuffer[i], floorLeftSample - startSample, fftFrameSize);
        }

        frame++;
        startSample = floorLeftSample;

        if(inputEOS && inputQueue.isEmpty()) {
            setOutputEOS();
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

}

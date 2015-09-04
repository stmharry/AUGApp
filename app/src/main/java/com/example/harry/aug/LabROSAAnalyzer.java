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
    private static final int SAMPLE_RATE_TARGET = 8000;
    private static final int MEL_BIN = 40;

    private Util util;

    private int frame;
    private int downSampleRatio;
    private float downSampleRate;

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

        downSampleRatio = (int)(Math.ceil((float) sampleRate / SAMPLE_RATE_TARGET));
        downSampleRate = (float) sampleRate / downSampleRatio;
        Log.d(TAG, String.format("sampleRate: %d -> %f", sampleRate, downSampleRate));

        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);
        mel = new Mel(0, sampleRate / downSampleRatio, MEL_BIN, fftFrameSize, fftFrameSizeCompact, sampleRate);
        util = new Util();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void operation() {
        super.operation();

        int floorLeftSample = frame * downSampleRatio * fftHopSize + 1;

        getInput(floorLeftSample + downSampleRatio * fftFrameSize);

        for(int i = 0; i < numChannel; i++) {
            float[] in = getFrame(inFloatBuffer[i], floorLeftSample - startSample, downSampleRatio * fftFrameSize);

            float[] inReal = util.downSample(in, downSampleRatio);
            float[] inImag = new float[fftFrameSize];

            window.window(inReal);
            fft.fft(inReal, inImag, true);

            float[] inMag = new float[fftFrameSizeCompact];
            float[] inPhase = new float[fftFrameSizeCompact];

            util.pol2cart(inMag, inPhase, util.compact(inReal), util.compact(inImag));

            float max = 0;
            int maxIndex = 0;
            for(int j = 0; j < fftFrameSizeCompact; j++) {
                if(inMag[j] > max) {
                    max = inMag[j];
                    maxIndex = j;
                }
            }
            Log.d(TAG, String.format("Max: inMag[%d] = %.2e; frequency = %.1f", maxIndex, max, (float) maxIndex / fftFrameSize * downSampleRate));

            float[] inMel = mel.mel(inMag);
            float[] inMelDB = util.db(inMel);

            /*
            for(int j = 0; j < MEL_BIN; j++) {
                Log.d(TAG, String.format("inMelDB[%d] = %f", j, inMelDB[j]));
            }*/

            // TODO
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

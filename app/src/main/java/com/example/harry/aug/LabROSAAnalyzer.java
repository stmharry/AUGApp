package com.example.harry.aug;

import android.util.Log;

/**
 * Created by harry on 15/8/29.
 */
public class LabROSAAnalyzer extends Analyzer {
    private static final String TAG = "LabROSAAnalyzer";

    private static final int BUFFER_CAPACITY = 32768;
    private static final int SAMPLE_RATE_TARGET = 8000;
    private static final float FFT_TIME = 0.05f;
    private static final int HOP_RATIO = 8;
    private static final int MEL_BIN = 40;

    private int frame;

    private float[][] inMelDBLast;
    private float[] onsetEnvelope;
    private int onsetEnvelopeIndex;

    //

    public static LabROSAAnalyzer newInstance() {
        return new LabROSAAnalyzer();
    }

    public LabROSAAnalyzer() {
        super(TAG, BUFFER_CAPACITY, SAMPLE_RATE_TARGET, FFT_TIME, HOP_RATIO);
    }

    @Override
    public synchronized long getTime() {
        return frame * fftHopSizeUs;
    }

    //

    @Override
    public void create() {
        super.create();

        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);
        mel = new Mel(0, sampleRate, MEL_BIN, fftFrameSize, fftFrameSizeCompact, sampleRate);

        inMelDBLast = new float[numChannel][];
        onsetEnvelope = new float[(int)((augManager.getAllTime() - fftFrameSizeUs) / fftHopSizeUs + 1)];
    }

    @Override
    public void start() {
        super.start();

        frame = 0;
    }

    @Override
    public void operation() {
        super.operation();

        int floorLeftSample = frame * fftHopSize + 1;
        requireInput(floorLeftSample + fftFrameSize);

        float inMelDBDiffSum = 0;
        for(int i = 0; i < numChannel; i++) {
            float[] inReal = getFrame(inFloatBuffer[i], floorLeftSample - startSample, fftFrameSize);
            float[] inImag = new float[fftFrameSize];

            window.window(inReal);
            fft.fft(inReal, inImag, true);

            float[] inMag = new float[fftFrameSizeCompact];
            float[] inPhase = new float[fftFrameSizeCompact];

            util.cart2pol(inMag, inPhase, util.compact(inReal), util.compact(inImag));

            float[] inMel = mel.mel(inMag);
            float[] inMelDB = util.db(inMel);

            float inMelDBDiff;
            if(inMelDBLast[i] != null) {
                for(int j = 0; j < MEL_BIN; j++) {
                    inMelDBDiff = inMelDB[j] - inMelDBLast[i][j];
                    if(inMelDBDiff > 0) {
                        inMelDBDiffSum += inMelDBDiff;
                    }
                }
            }
            inMelDBLast[i] = inMelDB;
        }

        onsetEnvelope[onsetEnvelopeIndex++] = inMelDBDiffSum;

        /*
        int scale = 35;
        int inMelDBDiffSumScaled = (int) (inMelDBDiffSum / scale);
        Log.d(TAG, "inMelDBDiffSum: " + new String(new char[inMelDBDiffSumScaled]).replace("\0", " ") + String.format("x (%.0f)", inMelDBDiffSum));
        */

        frame++;
        startSample = floorLeftSample;

        if(inputEOS && inputQueue.isEmpty()) {
            // TODO

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

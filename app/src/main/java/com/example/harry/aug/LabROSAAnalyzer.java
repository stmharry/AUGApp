package com.example.harry.aug;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by harry on 15/8/29.
 */
public class LabROSAAnalyzer extends Analyzer {
    private static final String TAG = "LabROSAAnalyzer";

    private static final int BUFFER_CAPACITY = 32768;
    private static final int SAMPLE_RATE_TARGET = 8000;
    private static final float FFT_TIME = 0.05f;
    private static final int FFT_HOP_RATIO = 8;

    private static final int MEL_BIN = 40;

    private static final int SELECTION_DURATION = 12;
    private static final int SELECTION_SIZE = 32;

    private static final int TEMPO_CENTER = 140;
    private static final int TEMPO_SPREAD = 1;

    private static final float BEAT_PARENT_RANGE = 2;
    private static final float BEAT_COST = 400;
    private static final float BEAT_PENALTY = 1.0f;

    private FFT envFFT;
    private State state;
    private Util util;

    private int frame;
    private float envSampleRate;

    private float[][] inMelDBLast;
    private float[] onsetEnvelope;
    private int selection;
    private int selectionSize;

    //

    private float bpm;
    private float beatScore;
    private long[] beatTime;

    //

    public static LabROSAAnalyzer newInstance() {
        return new LabROSAAnalyzer();
    }

    public LabROSAAnalyzer() {
        super(TAG, BUFFER_CAPACITY, SAMPLE_RATE_TARGET, FFT_TIME, FFT_HOP_RATIO);
    }

    public State getState() {
        return state;
    }

    @Override
    public synchronized long getTime() {
        return frame * fftHopSizeUs;
    }

    public int getSelectionSize() {
        return selectionSize;
    }

    public int getSelection() {
        return selection;
    }

    public float getBPM() {
        return bpm;
    }

    public float getBeatScore() {
        return beatScore;
    }

    public long[] getBeatTime() {
        return beatTime;
    }

    //

    private void analyzeTempo() {
        state = State.STATE_TEMPO;

        int envFrameSize = (int) (SELECTION_DURATION * envSampleRate);
        selectionSize = SELECTION_SIZE;
        if(envFrameSize > frame) {
            envFrameSize = frame;
            selectionSize = 1;
        }

        int envFFTSizeLog = util.nextpow2(2 * envFrameSize - 1);
        int envFFTFrameSize = 1 << envFFTSizeLog;
        envFFT = new FFT(envFFTFrameSize, envFFTSizeLog);

        util.normalize(onsetEnvelope);
        float[] bpmSample = new float[selectionSize];
        for(selection = 0; selection < selectionSize; selection++) {
            float[] onsetSegment = util.select(onsetEnvelope, envFrameSize);
            float[] onsetCorr = Arrays.copyOf(util.xcorr(onsetSegment), envFrameSize);
            util.window(onsetCorr);

            bpmSample[selection] = 60 * envSampleRate / util.maxIndex(onsetCorr);
        }

        bpm = util.mode(bpmSample);
        beatScore = util.score(bpmSample, bpm);

        Log.d(TAG, String.format("bpm = %.2f, beatScore = %.2f", bpm, beatScore));
    }

    private void analyzeBeat() {
        state = State.STATE_BEAT;

        int period = (int)(60 * envSampleRate / bpm);
        int periodLow = (int)(period / BEAT_PARENT_RANGE);
        int periodHigh = (int)(period * BEAT_PARENT_RANGE);
        int periodRange = periodHigh - periodLow;
        Log.d(TAG, "period = " + String.valueOf(period));

        int[] parentRange = new int[periodRange];
        float[] penalty = new float[periodRange];
        for(int i = 0; i < periodRange; i++) {
            parentRange[i] = i + periodLow;
            penalty[i] = (float)(Math.log((float) parentRange[i] / period) / Math.log(2));
            penalty[i] = BEAT_COST * penalty[i] * penalty[i];
        }

        int[] parent = new int[frame];
        float[] score = new float[frame];
        float[] scoreThis = new float[periodRange];

        util.smooth(onsetEnvelope, period);
        for(int i = 0; i < frame; i++) {
            for(int j = 0; j < periodRange; j++) {
                int index = i - parentRange[j];
                scoreThis[j] = ((index > 0)? score[index] : 0) - penalty[j];
            }

            float[] max = util.max(scoreThis);
            parent[i] = i - parentRange[(int) max[1]];
            score[i] = max[0] + onsetEnvelope[i] - BEAT_PENALTY;
        }

        int[] beat = new int[frame];
        int beatIndex = 0;

        beat[beatIndex] = util.maxIndex(score);
        do {
            beat[beatIndex + 1] = parent[beat[beatIndex]];
        } while(beat[++beatIndex] >= 0);

        beat = util.flip(Arrays.copyOf(beat, beatIndex));
        beatTime = new long[beatIndex];
        for(int i = 0; i < beatIndex; i++) {
            beatTime[i] = (long)((beat[i] / envSampleRate) * S_TO_US + fftFrameSizeUs * window.maxSlope()); // TODO: some room for modification
        }
    }

    //

    @Override
    public void create() {
        super.create();

        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);
        mel = new Mel(0, sampleRate, MEL_BIN, fftFrameSize, fftFrameSizeCompact, sampleRate);
        util = new Util();

        envSampleRate = sampleRate / fftHopSize;

        inMelDBLast = new float[numChannel][];
        onsetEnvelope = new float[(int)((augManager.getAllTime() - fftFrameSizeUs) / fftHopSizeUs + 1)];
    }

    @Override
    public void start() {
        super.start();

        state = State.STATE_ONSET;
        frame = 0;
        floorLeftSample = 0;
        ceilRightSample = fftFrameSize;
    }

    @Override
    public void operation() {
        super.operation();

        requireInput(ceilRightSample);

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

        onsetEnvelope[frame] = inMelDBDiffSum;
        frame++;
        startSample = floorLeftSample;
        floorLeftSample = frame * fftHopSize;
        ceilRightSample = floorLeftSample + fftFrameSize;

        if(inputEOS && inputQueue.isEmpty() && isUnderFlow()) {
            analyzeTempo();
            analyzeBeat();
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

    //

    public enum State {
        STATE_ONSET,
        STATE_TEMPO,
        STATE_BEAT
    }

    private class Util extends Analyzer.Util {
        private final float DB_THRESH = -60;
        public float[] db(float[] in) {
            int length = in.length;

            float[] out = new float[length];
            for(int i = 0; i < length; i++) {
                out[i] = (float)(20 * Math.log10(in[i]));
                if(out[i] < DB_THRESH) {
                    out[i] = DB_THRESH;
                }
            }
            return out;
        }

        public float sumX(float[] in) {
            float sumX = 0;
            for(float num: in) {
                sumX += num;
            }
            return sumX;
        }

        public float sumXX(float[] in) {
            float sumXX = 0;
            for(float num: in) {
                sumXX += (num * num);
            }
            return sumXX;
        }

        public void normalize(float[] in) {
            int length = in.length;

            float mean = sumX(in) / length;
            float std = (float) Math.sqrt(sumXX(in) / length - mean * mean);

            for(int i = 0; i < length; i++) {
                in[i] = (in[i] - mean) / std;
            }
        }

        public float[] select(float[] in, int size) {
            int length = in.length;
            int start = (int)(Math.random() * (length - size));
            return Arrays.copyOfRange(in, start, start + size);
        }

        public int[] flip(int[] in) {
            int length = in.length;
            int[] out = new int[length];

            for(int i = 0; i < length; i++) {
                out[i] = in[length - 1 - i];
            }

            return out;
        }

        public float[] xcorr(float[] in) {
            int n = envFFT.getN();

            float[] inReal = Arrays.copyOf(in, n);
            float[] inImag = new float[n];

            envFFT.fft(inReal, inImag, true);

            for(int i = 0; i < n; i++) {
                inReal[i] = inReal[i] * inReal[i] + inImag[i] * inImag[i];
                inImag[i] = 0;
            }

            envFFT.ifft(inReal, inImag, true);
            envFFT.scale(inReal, inImag);

            return inReal;
        }

        public float[] xcorr(float[] in1, float[] in2) {
            int n = envFFT.getN();

            float[] inReal1 = Arrays.copyOf(in1, n);
            float[] inImag1 = new float[n];
            float[] inReal2 = Arrays.copyOf(in2, n);
            float[] inImag2 = new float[n];

            envFFT.fft(inReal1, inImag1, true);
            envFFT.fft(inReal2, inImag2, true);

            float[] outReal = new float[n];
            float[] outImag = new float[n];
            for(int i = 0; i < n; i++) {
                outReal[i] = inReal1[1] * inReal2[i] + inImag1[i] * inImag2[i];
                outImag[i] = - inReal1[i] * inImag2[i] + inReal2[i] * inImag1[i];
            }

            envFFT.ifft(outReal, outImag, true);
            envFFT.scale(outReal, outImag);

            return outReal;
        }

        public void window(float[] in) {
            int length = in.length;

            float weight;
            float log2 = (float) Math.log(2);
            for(int i = 0; i < length; i++) {
                weight = (float)(Math.log(60 * envSampleRate / i / TEMPO_CENTER) / log2 / TEMPO_SPREAD);
                in[i] *= (float) Math.exp(- weight * weight / 2);
            }
        }

        private final float WIDTH = 0.05f;

        public void smooth(float[] in, int period) {
            int length = in.length;
            int filterLength = 2 * period + 1;

            float[] filter = new float[filterLength];
            for(int i = 0; i < filterLength; i++) {
                float num = (i - period) / (period * WIDTH);
                filter[i] = (float)(Math.exp(- num * num / 2));
            }

            float[] out = new float[length];
            for(int i = 0; i < length; i++) {
                for(int j = 0; j < filterLength; j++) {
                    int index = i + j - period;
                    if((index < 0) || (index >= length)) {
                        continue;
                    }
                    out[i] += in[index] * filter[j];
                }
            }

            System.arraycopy(out, 0, in, 0, length);
        }

        private final int ITERATION = 16;
        private final int SPREAD = 5;

        public float mode(float[] in) {
            int length = in.length;

            float modeLast = sumX(in) / length, mode = modeLast;
            float weight, weightSum;
            for(int i = 0; i < ITERATION; i++) {
                modeLast = mode;
                mode = 0;
                weightSum = 0;
                for(int j = 0; j < length; j++) {
                    weight = (in[j] - modeLast) / SPREAD;
                    weight = (float) Math.exp(- weight * weight / 2);
                    mode += (weight * in[j]);
                    weightSum += weight;
                }
                mode /= weightSum;
            }
            return mode;
        }

        public float score(float[] in, float mode) {
            int length = in.length;

            float weight, weightSum = 0;
            for(int j = 0; j < length; j++) {
                weight = (in[j] - mode) / SPREAD;
                weight = (float) Math.exp(- weight * weight / 2);
                weightSum += weight;
            }

            return weightSum / length;
        }
    }
}

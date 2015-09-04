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

    private FFT envFFT;
    private State state;
    private Util util;

    private int frame;

    private float envSampleRate;
    private int envFrameSize;
    private int envFFTSizeLog;
    private int envFFTFrameSize;

    private float[][] inMelDBLast;
    private float[] onsetEnvelope;
    private int onsetEnvelopeIndex;
    private int selectionSize;
    private int iteration;
    private float finalTempo;

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

    public int getIteration() {
        return iteration;
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

        frame++;
        startSample = floorLeftSample;

        if(inputEOS && inputQueue.isEmpty()) {
            state = State.STATE_TEMPO;

            envFrameSize = (int)(SELECTION_DURATION * envSampleRate);
            selectionSize = SELECTION_SIZE;
            if(envFrameSize > onsetEnvelopeIndex) {
                envFrameSize = onsetEnvelopeIndex;
                selectionSize = 1;
            }

            envFFTSizeLog = util.nextpow2(2 * envFrameSize - 1);
            envFFTFrameSize = 1 << envFFTSizeLog;
            envFFT = new FFT(envFFTFrameSize, envFFTSizeLog);

            util.normalize(onsetEnvelope);
            float[] tempo = new float[selectionSize];
            for(int iteration = 0; iteration < selectionSize; iteration++) {
                float[] onsetSegment = util.select(onsetEnvelope, envFrameSize);
                float[] onsetCorr = util.xcorr(onsetSegment);
                util.window(onsetCorr);

                tempo[iteration] = 60 * envSampleRate / util.maxIndex(onsetCorr);
            }
            finalTempo = util.mode(tempo);
            float score = util.score(tempo, finalTempo);
            Log.d(TAG, String.format("finalTempo = %.2f, score = %.2f", finalTempo, score));

            state = State.STATE_BEAT;

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

        public float[] xcorr(float[] in) {
            int length = in.length;

            float[] inReal = Arrays.copyOf(in, envFFTFrameSize);
            float[] inImag = new float[envFFTFrameSize];
            envFFT.fft(inReal, inImag, true);

            for(int i = 0; i < envFFTFrameSize; i++) {
                inReal[i] = inReal[i] * inReal[i] + inImag[i] * inImag[i];
                inImag[i] = 0;
            }
            envFFT.ifft(inReal, inImag, true);
            envFFT.scale(inReal, inImag);

            return Arrays.copyOf(inReal, length);
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

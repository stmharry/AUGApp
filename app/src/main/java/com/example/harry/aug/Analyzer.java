package com.example.harry.aug;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by harry on 8/28/15.
 */
public abstract class Analyzer extends AUGComponent {
    protected int BUFFER_CAPACITY;
    protected int SAMPLE_RATE_TARGET;
    protected float FFT_TIME;
    protected int FFT_HOP_RATIO;

    protected Window window;
    protected FFT fft;
    protected Mel mel;
    protected Util util;

    protected FloatBuffer[] inFloatBuffer;
    protected int startSample;
    protected int endSample;
    protected int floorLeftSample;
    protected int ceilRightSample;
    protected int downSampleRatio;

    protected int fftFrameSize;
    protected long fftFrameSizeUs;
    protected int fftSizeLog;
    protected int fftFrameSizeCompact;
    protected int fftHopSize;
    protected long fftHopSizeUs;
    protected int fftFrameAndHopSize;

    //

    public Analyzer(String TAG, int BUFFER_CAPACITY, int SAMPLE_RATE_TARGET, float FFT_TIME, int FFT_HOP_RATIO) {
        super(TAG);

        this.BUFFER_CAPACITY = BUFFER_CAPACITY;
        this.SAMPLE_RATE_TARGET = SAMPLE_RATE_TARGET;
        this.FFT_TIME = FFT_TIME;
        this.FFT_HOP_RATIO = FFT_HOP_RATIO;
    }

    //

    protected boolean isUnderFlow() {
        return (endSample / downSampleRatio < ceilRightSample);
    }

    protected void requireInput(int requiredEndSample) {
        if(inputEOS && inputQueue.isEmpty()) {
            return;
        }

        requiredEndSample *= downSampleRatio;

        while(endSample < requiredEndSample) {
            byte[] byteArray = dequeueInput(TIMEOUT_US);

            if(byteArray != null) {
                short[] shortArray = new short[byteArray.length / BYTE_PER_SHORT];
                ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                int numSample = byteArray.length / (numChannel * BYTE_PER_SHORT);
                for(int i = 0; i < numChannel; i++) {
                    float[] floatArray = new float[numSample];
                    for(int j = 0; j < numSample; j++) {
                        floatArray[j] = (float)(shortArray[j * numChannel + i]);
                    }
                    /*
                    while(inFloatBuffer[i].remaining() < numSample) {
                        FloatBuffer floatBuffer = FloatBuffer.allocate(inFloatBuffer[i].capacity() * 2);
                        inFloatBuffer[i].flip();
                        floatBuffer.put(inFloatBuffer[i]);
                        inFloatBuffer[i] = floatBuffer;
                    }*/ // TODO: handle variable buffer
                    inFloatBuffer[i].put(floatArray);
                }

                endSample += numSample;
            }
        }
    }

    protected float[] getFrame(FloatBuffer inBuffer, int position, int inSize) {
        position *= downSampleRatio;
        inSize *= downSampleRatio;

        float[] in = new float[inSize];

        inBuffer.flip();
        inBuffer.position(position);
        inBuffer.mark();
        inBuffer.get(in, 0, inSize);
        inBuffer.reset();
        inBuffer.compact();

        return Util.downSample(in, downSampleRatio);
    }

    //

    @Override
    public void create() {
        super.create();

        util = new Util();

        if(SAMPLE_RATE_TARGET == 0) {
            downSampleRatio = 1;
        } else {
            downSampleRatio = (int) (Math.ceil(sampleRate / SAMPLE_RATE_TARGET));
        }
        sampleRate /= downSampleRatio;

        // FFT
        fftFrameSize = (int)(sampleRate * FFT_TIME);
        fftSizeLog = Util.nextpow2(fftFrameSize);

        fftFrameSize = 1 << fftSizeLog;
        fftFrameSizeUs = (S_TO_US * fftFrameSize) / sampleRate;
        fftFrameSizeCompact = fftFrameSize / 2 + 1;
        fftHopSize = fftFrameSize / FFT_HOP_RATIO;
        fftHopSizeUs = (S_TO_US * fftHopSize) / sampleRate;
        fftFrameAndHopSize = fftFrameSize + fftHopSize;

        Log.d(TAG, "FFT size = " + String.valueOf(fftFrameSize));
    }

    @Override
    public void start() {
        super.start();

        //
        startSample = 0;
        endSample = 0;

        //
        inFloatBuffer = new FloatBuffer[numChannel];
        for(int i = 0; i < numChannel; i++) {
            inFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
        }
    }

    //

    protected class Window {
        private int n;
        private float[] w;

        public Window(int n) {
            this.n = n;
            w = new float[n];
            for (int i = 0; i < n; i++) {
                w[i] = (float) (1 - Math.cos(2 * Math.PI * i / n)) / 2;
            }
        }

        public void window(float[] x) {
            if (x.length != n) {
                throw new RuntimeException();
            }

            for (int i = 0; i < n; i++) {
                x[i] *= w[i];
            }
        }

        public void scale(float[] x) {
            if (x.length != n) {
                throw new RuntimeException();
            }

            for (int i = 0; i < n; i++) {
                x[i] *= (2f / 3);
            }
        }

        public float maxSlope() {
            return 0.75f;
        }
    }

    protected class Mel {
        private final int BASE = 2595;
        private final int SCALE = 700;

        private int bin;
        private int k;

        private float[] fftFreq;
        private float[] melFreq;
        private float[][] fftToMel;

        public Mel(float minFreq, float maxFreq, int bin, int n, int k, float sr) {
            this.bin = bin;
            this.k = k;

            fftFreq = new float[k];
            for(int i = 0; i < k; i++) {
                fftFreq[i] = (float) i / n * sr;
            }

            float minBin = freqToBin(minFreq);
            float maxBin = freqToBin(maxFreq);
            melFreq = new float[bin + 2];
            for(int i = 0; i <= bin + 1; i++) {
                melFreq[i] = binToFreq(minBin + (float) i / (bin + 1) * (maxBin - minBin));
            }

            float low, high, res;
            fftToMel = new float[bin][];
            for(int i = 0; i < bin; i++) {
                fftToMel[i] = new float[k];
                for(int j = 0; j < k; j++) {
                    low = (fftFreq[j] - melFreq[i]) / (melFreq[i + 1] - melFreq[i]);
                    high = (melFreq[i + 2] - fftFreq[j]) / (melFreq[i + 2] - melFreq[i + 1]);
                    res = ((low < high)? low : high) * 2 / (melFreq[i + 2] - melFreq[i]);
                    fftToMel[i][j] = (res > 0)? res : 0;
                }
            }
        }

        public float[] mel(float[] in) {
            float[] out = new float[bin];
            for(int i = 0; i < bin; i++) {
                for(int j = 0; j < k; j++) {
                    out[i] += fftToMel[i][j] * in[j];
                }
            }
            return out;
        }

        private float freqToBin(float freq) {
            return (float)(BASE * Math.log10(1 + freq / SCALE));
        }

        private float binToFreq(float bin) {
            return (float)(SCALE * (Math.pow(10, bin / BASE) - 1));
        }
    }

}

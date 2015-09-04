package com.example.harry.aug;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Created by harry on 8/28/15.
 */
public abstract class Analyzer extends AUGComponent {
    protected int BUFFER_CAPACITY;
    protected int SAMPLE_RATE_TARGET;
    protected float FFT_TIME;
    protected int HOP_RATIO;

    protected Window window;
    protected FFT fft;
    protected Mel mel;
    protected Util util;

    protected FloatBuffer[] inFloatBuffer;
    protected int startSample;
    protected int endSample;
    protected int downSampleRatio;

    protected int fftFrameSize;
    protected long fftFrameSizeUs;
    protected int fftSizeLog;
    protected int fftFrameSizeCompact;
    protected int fftHopSize;
    protected long fftHopSizeUs;
    protected int fftFrameAndHopSize;

    //

    public Analyzer(String TAG, int BUFFER_CAPACITY, int SAMPLE_RATE_TARGET, float FFT_TIME, int HOP_RATIO) {
        super(TAG);

        this.BUFFER_CAPACITY = BUFFER_CAPACITY;
        this.SAMPLE_RATE_TARGET = SAMPLE_RATE_TARGET;
        this.FFT_TIME = FFT_TIME;
        this.HOP_RATIO = HOP_RATIO;
    }

    //

    protected void myLogD(String str) {
        //Log.d(TAG, str);
    }

    //

    protected void requireInput(int requiredEndSample) {
        requiredEndSample *= downSampleRatio;

        while(endSample < requiredEndSample) {
            byte[] byteArray = dequeueInput(TIMEOUT_US);

            if(byteArray != null) {
                short[] shortArray = new short[byteArray.length / BYTE_PER_SHORT];
                ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                int numSample = byteArray.length / (numChannel * BYTE_PER_SHORT);
                myLogD("--- [requireInput] ---");
                myLogD("numSample = " + String.valueOf(numSample));
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
                    }*/ // TODO
                    inFloatBuffer[i].put(floatArray);
                }

                endSample += numSample;
                myLogD("endSample = " + String.valueOf(endSample));
            }
        }
    }

    protected float[] getFrame(FloatBuffer inBuffer, int position, int inSize) {
        position *= downSampleRatio;
        inSize *= downSampleRatio;

        myLogD("--- [getFrame] ---");
        myLogD("position = " + String.valueOf(position));
        myLogD("inSize = " + String.valueOf(inSize));

        float[] in = new float[inSize];

        inBuffer.flip();
        inBuffer.position(position);
        inBuffer.mark();
        inBuffer.get(in, 0, inSize);
        inBuffer.reset();
        inBuffer.compact();

        return util.downSample(in, downSampleRatio);
    }

    //

    @Override
    public void create() {
        super.create();

        if(SAMPLE_RATE_TARGET == 0) {
            downSampleRatio = 1;
        } else {
            downSampleRatio = (int) (Math.ceil(sampleRate / SAMPLE_RATE_TARGET));
        }
        sampleRate /= downSampleRatio;

        // FFT
        fftFrameSize = (int)(sampleRate * FFT_TIME);
        fftSizeLog = (fftFrameSize == 0)? 0 : (32 - Integer.numberOfLeadingZeros(fftFrameSize - 1));

        fftFrameSize = 1 << fftSizeLog;
        fftFrameSizeUs = (S_TO_US * fftFrameSize) / sampleRate;
        fftFrameSizeCompact = fftFrameSize / 2 + 1;
        fftHopSize = fftFrameSize / HOP_RATIO;
        fftHopSizeUs = (S_TO_US * fftHopSize) / sampleRate;
        fftFrameAndHopSize = fftFrameSize + fftHopSize;

        Log.d(TAG, "FFT size = " + String.valueOf(fftFrameSize));

        util = new Util();
    }

    @Override
    public void start() {
        super.start();

        //
        startSample = 1;
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
    }

    protected class FFT {
        private int n;
        private int m;
        private float[] cos;
        private float[] sin;

        public FFT(int n, int m) {
            this.n = n;
            this.m = m;

            cos = new float[n / 2];
            sin = new float[n / 2];
            for (int i = 0; i < n / 2; i++) {
                cos[i] = (float) (Math.cos(-2 * Math.PI * i / n));
                sin[i] = (float) (Math.sin(-2 * Math.PI * i / n));
            }
        }

        private void reverse(float[] x, float[] y) {
            // TODO: bit reversal can only be disabled when compacting properly (please do this!)
            int i, j, n1, n2;
            float t1;

            j = 0;
            n2 = n / 2;
            for (i = 1; i < n - 1; i++) {
                n1 = n2;
                while (j >= n1) {
                    j = j - n1;
                    n1 = n1 / 2;
                }
                j = j + n1;

                if (i < j) {
                    t1 = x[i];
                    x[i] = x[j];
                    x[j] = t1;
                    t1 = y[i];
                    y[i] = y[j];
                    y[j] = t1;
                }
            }
        }

        public void fft(float[] x, float[] y, boolean reverse) {
            if ((x.length != n) || (y.length != n)) {
                throw new RuntimeException();
            }

            int i, j, k, n1, n2, a;
            float c, s, t1, t2;

            if(reverse) {
                reverse(x, y);
            }

            // FFT
            n1 = 0;
            n2 = 1;

            for (i = 0; i < m; i++) {
                n1 = n2;
                n2 = n2 + n2;
                a = 0;

                for (j = 0; j < n1; j++) {
                    c = cos[a];
                    s = sin[a];
                    a += 1 << (m - i - 1);

                    for (k = j; k < n; k = k + n2) {
                        t1 = c * x[k + n1] - s * y[k + n1];
                        t2 = s * x[k + n1] + c * y[k + n1];
                        x[k + n1] = x[k] - t1;
                        y[k + n1] = y[k] - t2;
                        x[k] = x[k] + t1;
                        y[k] = y[k] + t2;
                    }
                }
            }
        }

        public void ifft(float[] x, float[] y, boolean reverse) {
            if ((x.length != n) || (y.length != n)) {
                throw new RuntimeException();
            }

            int i, j, k, n1, n2, a;
            float c, s, t1, t2;

            if(reverse) {
                reverse(x, y);
            }

            // FFT
            n1 = 0;
            n2 = 1;

            for (i = 0; i < m; i++) {
                n1 = n2;
                n2 = n2 + n2;
                a = 0;

                for (j = 0; j < n1; j++) {
                    c = cos[a];
                    s = sin[a];
                    a += 1 << (m - i - 1);

                    for (k = j; k < n; k = k + n2) {
                        t1 = c * x[k + n1] + s * y[k + n1];
                        t2 = s * x[k + n1] - c * y[k + n1];
                        x[k + n1] = x[k] - t1;
                        y[k + n1] = y[k] + t2;
                        x[k] = x[k] + t1;
                        y[k] = y[k] - t2;
                    }
                }
            }
        }

        public void scale(float[] x, float[] y) {
            if ((x.length != n) || (y.length != n)) {
                throw new RuntimeException();
            }

            for (int i = 0; i < n; i++) {
                x[i] /= n;
                y[i] /= n;
            }
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

    protected class Util {
        public void cart2pol(float[] r, float[] t, float[] x, float[] y) {
            int length = x.length;

            for(int i = 0; i < length; i++) {
                r[i] = (float)(Math.sqrt(x[i] * x[i] + y[i] * y[i]));
                t[i] = (float)(Math.atan2(y[i], x[i]));
            }
        }

        public void pol2cart(float[] x, float[] y, float[] r, float[] t) {
            int length = r.length;

            for(int i = 0; i < length; i++) {
                x[i] = r[i] * (float)(Math.cos(t[i]));
                y[i] = r[i] * (float)(Math.sin(t[i]));
            }
        }

        public float[] expand(float[] in, boolean pos) {
            int inLength = in.length;
            int outLength = 2 * (inLength - 1);

            float[] out = new float[outLength];

            for(int i = 0; i < inLength; i++) {
                out[i] = in[i];
            }

            for(int i = inLength; i < outLength; i++) {
                out[i] = pos? in[outLength - i] : -in[outLength - i];
            }

            return out;
        }

        public float[] compact(float[] in) {
            return Arrays.copyOf(in, in.length / 2 + 1);
        }

        public float[] downSample(float[] in, int ratio) {
            if(ratio == 1) {
                return in;
            }

            int outSize = in.length / ratio;
            float[] out = new float[outSize];
            for(int i = 0; i < outSize; i++) {
                out[i] = in[i * ratio];
            }
            return out;
        }

        private float DB_THRESH = -60;

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
    }
}

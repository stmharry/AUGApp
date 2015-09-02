package com.example.harry.aug;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by harry on 8/28/15.
 */
public abstract class Analyzer extends AUGComponent {
    protected int BUFFER_CAPACITY = 4096;
    protected float FFT_TIME = 0.01f;
    protected int HOP_RATIO = 4;

    protected Window window;
    protected FFT fft;

    protected FloatBuffer[] inFloatBuffer;
    protected int startSample;
    protected int endSample;

    protected int fftFrameSize;
    protected int fftSizeLog;
    protected int fftSizeCompact;
    protected int fftHopSize;
    protected int fftFrameAndHopSize;
    protected long fftHopSizeUs;

    //

    public Analyzer(String TAG, int BUFFER_CAPACITY, float FFT_TIME, int HOP_RATIO) {
        super(TAG);
        this.BUFFER_CAPACITY = BUFFER_CAPACITY;
        this.FFT_TIME = FFT_TIME;
        this.HOP_RATIO = HOP_RATIO;
    }

    //

    protected void myLogD(String str) {
        // Log.d(TAG, str);
    }

    //

    protected void getInput(int requiredEndSample) {
        while(endSample < requiredEndSample) {
            byte[] byteArray = dequeueInput(TIMEOUT_US);

            if(byteArray != null) {
                short[] shortArray = new short[byteArray.length / BYTE_PER_SHORT];
                ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                int numSample = byteArray.length / (numChannel * BYTE_PER_SHORT);
                myLogD("--- [getInput] ---");
                myLogD("numSample = " + String.valueOf(numSample));
                for(int i = 0; i < numChannel; i++) {
                    float[] floatArray = new float[numSample];
                    for(int j = 0; j < numSample; j++) {
                        floatArray[j] = (float)(shortArray[j * numChannel + i]);
                    }
                    // TODO: handle buffer size change
                    inFloatBuffer[i].put(floatArray);
                }

                endSample += numSample;
                myLogD("endSample = " + String.valueOf(endSample));
            }
        }
    }

    protected float[] getFrame(FloatBuffer inBuffer, int position, int inSize) {
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

        return in;
    }

    //

    @Override
    public void create() {
        super.create();

        // FFT
        fftFrameSize = (int)(sampleRate * FFT_TIME);
        fftSizeLog = (fftFrameSize == 0)? 0 : (32 - Integer.numberOfLeadingZeros(fftFrameSize - 1));

        fftFrameSize = 1 << fftSizeLog;
        fftSizeCompact = fftFrameSize / 2 + 1;
        fftHopSize = fftFrameSize / HOP_RATIO;
        fftFrameAndHopSize = fftFrameSize + fftHopSize;
        fftHopSizeUs = (S_TO_US * fftHopSize) / sampleRate;

        Log.d(TAG, "FFT size = " + String.valueOf(fftFrameSize));

        // Utility
        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);
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

        public void fft(float[] x, float[] y) {
            if ((x.length != n) || (y.length != n)) {
                throw new RuntimeException();
            }

            int i, j, k, n1, n2, a;
            float c, s, t1, t2;

            reverse(x, y);

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

        public void ifft(float[] x, float[] y) {
            if ((x.length != n) || (y.length != n)) {
                throw new RuntimeException();
            }

            int i, j, k, n1, n2, a;
            float c, s, t1, t2;

            reverse(x, y);

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
}

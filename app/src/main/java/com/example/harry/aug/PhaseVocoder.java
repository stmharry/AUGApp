package com.example.harry.aug;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by harry on 15/8/2.
 */
public class PhaseVocoder extends Component implements Runnable {
    private static final String TAG = "PhaseVocoder";

    private static final int BUFFER_CAPACITY = 4096;
    private static final float FFT_TIME = 0.01f;
    private static final int HOP_RATIO = 4;

    private Window window;
    private FFT fft;
    private FloatBuffer[] inFloatBuffer;
    private FloatBuffer[] outFloatBuffer;
    private FloatBuffer frameBuffer;

    private float[] magnitude;
    private float[] phase;

    private float speed;
    private float frame;
    private int removedFrame;
    private int startSample;
    private int endSample;

    private int fftFrameSize;
    private int fftSizeLog;
    private int fftSizeCompact;
    private int fftHopSize;
    private long fftHopSizeUs;

    /////////////////
    // INNER CLASS //
    /////////////////

    public PhaseVocoder(AUGManager augManager) {
        super(TAG, augManager);
    }

    private void myLogD(String str) {
        // Log.d(TAG, str);
    }

    /////////
    // LOG //
    /////////

    private void myLogComplex(float[] r, float[] t, float[] x, float[] y) {
        for(int i = 0; i < 8; i++) {
            myLogD("Bin " + String.valueOf(i) + ": "
                    + "(r, t) = ("
                    + String.format("%.2e", r[i]) + ", "
                    + String.format("%.0f", t[i] / Math.PI * 180) + "º); "
                    + "(x, y) = ("
                    + String.format("%.2e", x[i]) + ", "
                    + String.format("%.2e", y[i]) + ")");
        }
        for(int i = r.length - 8; i < r.length; i++) {
            myLogD("Bin " + String.valueOf(i) + ": "
                    + "(r, t) = ("
                    + String.format("%.2e", r[i]) + ", "
                    + String.format("%.0f", t[i] / Math.PI * 180) + "º); "
                    + "(x, y) = ("
                    + String.format("%.2e", x[i]) + ", "
                    + String.format("%.2e", y[i]) + ")");
        }
    }

    private void myLogArray(float[] x, int skip) {
        for (int i = 0; i < x.length; i += skip) {
            myLogD("["
                    + String.valueOf(i) + "] = "
                    + String.format("%.2e", x[i]));
        }
    }

    private void cart2pol(float[] r, float[] t, float[] x, float[] y) {
        int length = x.length;

        for(int i = 0; i < length; i++) {
            r[i] = (float)(Math.sqrt(x[i] * x[i] + y[i] * y[i]));
            t[i] = (float)(Math.atan2(y[i], x[i]));
        }
    }

    /////////////
    // UTILITY //
    /////////////

    private void pol2cart(float[] x, float[] y, float[] r, float[] t) {
        int length = r.length;

        for(int i = 0; i < length; i++) {
            x[i] = r[i] * (float)(Math.cos(t[i]));
            y[i] = r[i] * (float)(Math.sin(t[i]));
        }
    }

    private void process(float[] r, float[] t, float[] real) {
        float[] imag = new float[fftFrameSize];

        window.window(real);
        //myLogArray(real);
        fft.fft(real, imag);
        cart2pol(r, t, Arrays.copyOf(real, fftSizeCompact), Arrays.copyOf(imag, fftSizeCompact));

        //myLogComplex(r, t, Arrays.copyOf(real, fftSizeCompact), Arrays.copyOf(imag, fftSizeCompact));
    }

    private void interpolate(float[] interReal, float[] leftReal, float[] rightReal, float ratio) {
        float[] leftR = new float[fftSizeCompact];
        float[] rightR = new float[fftSizeCompact];
        float[] leftT = new float[fftSizeCompact];
        float[] rightT = new float[fftSizeCompact];

        //myLogD("--- [interpolate] ---");
        //myLogD("left (time / frequency domain)");
        process(leftR, leftT, leftReal);
        //myLogD("right (time / frequency domain)");
        process(rightR, rightT, rightReal);

        if(phase == null) {
            magnitude = new float[fftSizeCompact];
            phase = Arrays.copyOf(leftT, fftSizeCompact);
        } else {
            for(int i = 0; i < fftSizeCompact; i++) {
                phase[i] += (rightT[i] - leftT[i]);
            }
        }

        for(int i = 0; i < fftSizeCompact; i++) {
            magnitude[i] = (1 - ratio) * leftR[i] + ratio * rightR[i];
        }

        float[] interRealTrunc = new float[fftSizeCompact];
        float[] interImagTrunc = new float[fftSizeCompact];

        pol2cart(interRealTrunc, interImagTrunc, magnitude, phase);

        //myLogD("inter (frequency domain)");
        //myLogComplex(magnitude, phase, interRealTrunc, interImagTrunc);

        float[] interImag = new float[fftFrameSize];

        for(int i = 0; i < fftSizeCompact; i++) {
            interReal[i] = interRealTrunc[i];
            interImag[i] = interImagTrunc[i];
        }

        for(int i = fftSizeCompact; i < fftFrameSize; i++) {
            interReal[i] = interRealTrunc[fftFrameSize - i];
            interImag[i] = - interImagTrunc[fftFrameSize - i];
        }

        fft.ifft(interReal, interImag);
        fft.scale(interReal, interImag);

        window.window(interReal);
        window.scale(interReal);

        //myLogD("inter (time domain)");
        //myLogArray(interReal, 8);
    }

    @Override
    protected void initializeElement() {
        super.initializeElement();

        // FFT
        fftFrameSize = (int)(sampleRate * FFT_TIME);
        fftSizeLog = (fftFrameSize == 0)? 0 : (32 - Integer.numberOfLeadingZeros(fftFrameSize - 1));

        fftFrameSize = 1 << fftSizeLog;
        fftSizeCompact = fftFrameSize / 2 + 1;
        fftHopSize = fftFrameSize / HOP_RATIO;
        fftHopSizeUs = (S_TO_US * fftHopSize) / sampleRate;

        Log.d(TAG, "FFT size = " + String.valueOf(fftFrameSize));

        // Utility
        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);

        // Playback
        speed = 0.6f;
    }

    @Override
    protected void initializeBuffer() {
        super.initializeBuffer();

        // Record
        frame = 0;
        removedFrame = 0;
        startSample = 1;
        endSample = 0;

        // Buffer
        inFloatBuffer = new FloatBuffer[numChannel];
        outFloatBuffer = new FloatBuffer[numChannel];
        for(int i = 0; i < numChannel; i++) {
            inFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
            outFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
            outFloatBuffer[i].put(new float[fftFrameSize]);
        }
        frameBuffer = FloatBuffer.allocate(BUFFER_CAPACITY);
    }

    /////////////
    // PROCESS //
    /////////////

    @Override
    protected void operation() {
        int floorFrame = (int)(Math.floor(frame));
        int ceilFrame = floorFrame + 1;
        float fracFrame = frame - floorFrame;

        int floorLeftSample = floorFrame * fftHopSize + 1;
        int floorRightSample = floorFrame * fftHopSize + fftFrameSize;
        int ceilLeftSample = ceilFrame * fftHopSize + 1;
        int ceilRightSample = ceilFrame * fftHopSize + fftFrameSize;

        myLogD("--- [ACTION] ---");
        myLogD("frame = " + String.valueOf(frame));
        myLogD("startSample = " + String.valueOf(startSample));
        myLogD("floorLeftSample = " + String.valueOf(floorLeftSample));
        myLogD("ceilRightSample = " + String.valueOf(ceilRightSample));

        // INPUT
        while(endSample < ceilRightSample) {
            byte[] byteArray = dequeueInput(TIMEOUT_US);

            if(byteArray != null) {
                short[] shortArray = new short[byteArray.length / BYTE_PER_SHORT];
                ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                int numSample = byteArray.length / (numChannel * BYTE_PER_SHORT);
                myLogD("--- [LOAD] ---");
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

        // PROCESSING
        int inSize = ceilRightSample - floorLeftSample + 1;
        float[][] out = new float[numChannel][];

        for(int i = 0; i < numChannel; i++) {
            float[] in = new float[inSize];

            inFloatBuffer[i].flip();
            inFloatBuffer[i].position(floorLeftSample - startSample);
            inFloatBuffer[i].mark();
            inFloatBuffer[i].get(in, 0, inSize);
            inFloatBuffer[i].reset();
            inFloatBuffer[i].compact();

            float[] left = Arrays.copyOfRange(in, 0, fftFrameSize);
            float[] right = Arrays.copyOfRange(in, fftHopSize, fftHopSize + fftFrameSize);
            float[] inter = new float[fftFrameSize];

            interpolate(inter, left, right, fracFrame);

            float[] outTemp = new float[fftFrameSize];

            outFloatBuffer[i].flip();
            outFloatBuffer[i].mark();
            outFloatBuffer[i].get(outTemp, 0, fftFrameSize);

            for(int j = 0; j < fftFrameSize; j++) {
                outTemp[j] += inter[j];
            }

            outFloatBuffer[i].reset();
            outFloatBuffer[i].put(Arrays.copyOfRange(outTemp, fftHopSize, fftFrameSize));
            outFloatBuffer[i].put(new float[fftHopSize]);

            out[i] = Arrays.copyOfRange(outTemp, 0, fftHopSize);

            //myLogD("out (time domain)");
            //myLogArray(out[i], 8);
        }

        frameBuffer.put(frame);
        if (frameBuffer.position() == frameBuffer.limit()) {
            int thisRemovedSize = frameBuffer.position() - BUFFER_QUEUE_CAPACITY;
            removedFrame += thisRemovedSize;

            frameBuffer.position(thisRemovedSize);
            frameBuffer.compact();
        }
        frame += speed;

        startSample = floorLeftSample;

        // OUTPUT
        short[] shortBufferArray = new short[fftHopSize * numChannel];
        for(int i = 0; i < numChannel; i++) {
            for(int j = 0; j < fftHopSize; j++) {
                shortBufferArray[j * numChannel + i] = (short)(out[i][j]);
            }
        }

        int byteBufferSize = fftHopSize * numChannel * BYTE_PER_SHORT;
        byte[] byteBufferArray = new byte[byteBufferSize];
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferSize);
        ShortBuffer shortBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        shortBuffer.put(shortBufferArray).flip();
        byteBuffer.get(byteBufferArray);

        next.queueInput(byteBufferArray);
    }

    @Override
    protected void setTime() {
        long nextTime = next.getTime();
        float nextFrame = (float) nextTime / fftHopSizeUs - removedFrame;
        int floorNextFrame = (int) (Math.floor(nextFrame));
        float fracNextFrame = nextFrame - floorNextFrame;

        // myLogD("--- [SEEK] ---");

        float leftFrame;
        float rightFrame;
        frameBuffer.flip();
        frameBuffer.position(floorNextFrame);
        frameBuffer.mark();
        leftFrame = frameBuffer.get();
        if (frameBuffer.hasRemaining()) {
            rightFrame = frameBuffer.get();
        } else {
            rightFrame = leftFrame;
        }
        frameBuffer.reset();
        frameBuffer.compact();
        removedFrame += floorNextFrame;

        float thisFrame = leftFrame * (1 - fracNextFrame) + rightFrame * fracNextFrame;
        time = (long) (thisFrame * fftHopSizeUs);

        /*
        myLogD("nextTime = " + String.valueOf(nextTime));
        myLogD("nextFrame = " + String.valueOf(nextFrame));
        myLogD("removedFrame = " + String.valueOf(removedFrame));
        myLogD("leftFrame = " + String.valueOf(leftFrame));
        myLogD("rightFrame = " + String.valueOf(rightFrame));
        myLogD("thisFrame = " + String.valueOf(thisFrame));
        myLogD("thisTime = " + String.valueOf(thisTime));
        */
    }

    @Override
    protected void terminate() {
        super.terminate();
        // TODO
    }

    private static class Window {
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

    private static class FFT {
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

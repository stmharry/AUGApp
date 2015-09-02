package com.example.harry.aug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by harry on 15/8/2.
 */
public class PhaseVocoderAnalyzer extends Analyzer {
    private static final String TAG = "PhaseVocoderAnalyzer";

    private static final int BUFFER_CAPACITY = 4096;
    private static final float FFT_TIME = 0.01f;
    private static final int HOP_RATIO = 4;

    private Util util;
    private FloatBuffer[] outFloatBuffer;
    private FloatBuffer frameBuffer;

    private float[] magnitude;
    private float[] phase;

    private float speed;
    private float frame;
    private int removedFrame;

    public static PhaseVocoderAnalyzer newInstance() {
        return new PhaseVocoderAnalyzer();
    }

    public PhaseVocoderAnalyzer() {
        super(TAG, BUFFER_CAPACITY, FFT_TIME, HOP_RATIO);
    }

    //

    @Override
    public synchronized long getTime() {
        long nextTime = next.getTime();
        if(nextTime == AUGManager.UPDATE_FAIL) {
            return AUGManager.UPDATE_FAIL;
        }
        float nextFrame = (float) nextTime / fftHopSizeUs - removedFrame;
        int floorNextFrame = (int) (Math.floor(nextFrame));
        float fracNextFrame = nextFrame - floorNextFrame;

        myLogD("--- [SEEK] ---");
        myLogD("nextTime = " + String.valueOf(nextTime));
        myLogD("nextFrame = " + String.valueOf(nextFrame));
        myLogD("removedFrame = " + String.valueOf(removedFrame));

        float leftFrame;
        float rightFrame;
        frameBuffer.flip();
        frameBuffer.position(floorNextFrame);
        frameBuffer.mark();

        leftFrame = (frameBuffer.hasRemaining())? frameBuffer.get() : 0;
        rightFrame = (frameBuffer.hasRemaining())? frameBuffer.get() : leftFrame;
        myLogD("leftFrame = " + String.valueOf(leftFrame));
        myLogD("rightFrame = " + String.valueOf(rightFrame));

        frameBuffer.reset();
        frameBuffer.compact();
        removedFrame += floorNextFrame;

        float thisFrame = leftFrame * (1 - fracNextFrame) + rightFrame * fracNextFrame;
        long thisTime = (long) (thisFrame * fftHopSizeUs);

        myLogD("thisFrame = " + String.valueOf(thisFrame));
        myLogD("thisTime = " + String.valueOf(thisTime));

        return thisTime;
    }

    private float[] setOutput(FloatBuffer outBuffer, float[] out) {
        float[] outTemp = new float[fftFrameSize];

        outBuffer.flip();
        outBuffer.mark();
        outBuffer.get(outTemp, 0, fftFrameSize);

        for(int j = 0; j < fftFrameSize; j++) {
            outTemp[j] += out[j];
        }

        outBuffer.reset();
        outBuffer.put(Arrays.copyOfRange(outTemp, fftHopSize, fftFrameSize));
        outBuffer.put(new float[fftHopSize]);

        return Arrays.copyOfRange(outTemp, 0, fftHopSize);
    }

    //

    @Override
    public void create() {
        super.create();

        util = new Util();
        speed = 1.5f; // TODO: change
    }

    @Override
    public void start() {
        super.start();

        //
        frame = 0;
        removedFrame = 0;

        //
        outFloatBuffer = new FloatBuffer[numChannel];
        for(int i = 0; i < numChannel; i++) {
            outFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
            outFloatBuffer[i].put(new float[fftFrameSize]);
        }
        frameBuffer = FloatBuffer.allocate(BUFFER_CAPACITY);
    }

    @Override
    public void operation() {
        super.operation();

        int floorFrame = (int)(Math.floor(frame));
        float fracFrame = frame - floorFrame;
        int floorLeftSample = floorFrame * fftHopSize + 1;

        myLogD("--- [ACTION] ---");
        myLogD("frame = " + String.valueOf(frame));
        myLogD("startSample = " + String.valueOf(startSample));
        myLogD("floorLeftSample = " + String.valueOf(floorLeftSample));

        //
        getInput(floorLeftSample + fftFrameAndHopSize);

        //
        float[][] out = new float[numChannel][];

        for(int i = 0; i < numChannel; i++) {
            float[] in = getFrame(inFloatBuffer[i], floorLeftSample - startSample, fftFrameAndHopSize);
            float[] left = Arrays.copyOfRange(in, 0, fftFrameSize);
            float[] right = Arrays.copyOfRange(in, fftHopSize, fftHopSize + fftFrameSize);
            float[] inter = util.interpolate(left, right, fracFrame);
            out[i] = setOutput(outFloatBuffer[i], inter);
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

        //
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

    //

    private class Util {
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

        public void process(float[] r, float[] t, float[] real) {
            float[] imag = new float[fftFrameSize];

            window.window(real);
            fft.fft(real, imag);
            cart2pol(r, t, Arrays.copyOf(real, fftSizeCompact), Arrays.copyOf(imag, fftSizeCompact));
        }

        public float[] interpolate(float[] leftReal, float[] rightReal, float ratio) {
            float[] interReal = new float[fftFrameSize];

            float[] leftR = new float[fftSizeCompact];
            float[] rightR = new float[fftSizeCompact];
            float[] leftT = new float[fftSizeCompact];
            float[] rightT = new float[fftSizeCompact];

            process(leftR, leftT, leftReal);
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

            return interReal;
        }
    }
}

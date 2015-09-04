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

        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);
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
    private class Util extends Analyzer.Util {
        public void process(float[] r, float[] t, float[] real) {
            float[] imag = new float[fftFrameSize];

            window.window(real);
            fft.fft(real, imag, true);

            cart2pol(r, t, compact(real), compact(imag));
        }

        public float[] interpolate(float[] leftReal, float[] rightReal, float ratio) {
            float[] leftR = new float[fftFrameSizeCompact];
            float[] rightR = new float[fftFrameSizeCompact];
            float[] leftT = new float[fftFrameSizeCompact];
            float[] rightT = new float[fftFrameSizeCompact];

            process(leftR, leftT, leftReal);
            process(rightR, rightT, rightReal);

            if(phase == null) {
                magnitude = new float[fftFrameSizeCompact];
                phase = compact(leftT);
            } else {
                for(int i = 0; i < fftFrameSizeCompact; i++) {
                    phase[i] += (rightT[i] - leftT[i]);
                }
            }

            for(int i = 0; i < fftFrameSizeCompact; i++) {
                magnitude[i] = (1 - ratio) * leftR[i] + ratio * rightR[i];
            }

            float[] interRealTrunc = new float[fftFrameSizeCompact];
            float[] interImagTrunc = new float[fftFrameSizeCompact];

            pol2cart(interRealTrunc, interImagTrunc, magnitude, phase);

            float[] interReal = expand(interRealTrunc, true);
            float[] interImag = expand(interImagTrunc, false);

            fft.ifft(interReal, interImag, true);
            fft.scale(interReal, interImag);

            window.window(interReal);
            window.scale(interReal);

            return interReal;
        }
    }
}

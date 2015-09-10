package com.example.harry.aug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Created by harry on 15/8/2.
 */
public class PhaseVocoderAnalyzer extends Analyzer {
    private static final String TAG = "PhaseVocoderAnalyzer";

    private static final int BUFFER_CAPACITY = 16384;
    private static final int SAMPLE_RATE_TARGET = 0;
    private static final float FFT_TIME = 0.01f;
    private static final int FFT_HOP_RATIO = 4;

    private Util util;
    private FloatBuffer[] outFloatBuffer;
    private FloatBuffer frameBuffer;
    private float[] frameRecord;

    private float[][] phase;

    private float speed;
    private int normalFrame;
    private float frame;
    private int removedFrame;

    private int floorFrame;
    private float fracFrame;


    public static PhaseVocoderAnalyzer newInstance() {
        return new PhaseVocoderAnalyzer();
    }

    public PhaseVocoderAnalyzer() {
        super(TAG, BUFFER_CAPACITY, SAMPLE_RATE_TARGET, FFT_TIME, FFT_HOP_RATIO);
    }

    //

    @Override
    public synchronized long getTime() {
        long nextTime = next.getTime();
        if(nextTime == AUGManager.UPDATE_FAIL) {
            return AUGManager.UPDATE_FAIL;
        }
        float nextFrame = (float) nextTime / fftHopSizeUs;
        int floorNextFrame = (int) Math.floor(nextFrame);
        float fracNextFrame = nextFrame - floorNextFrame;

        float leftFrame = frameRecord[floorNextFrame];
        float rightFrame = leftFrame;

        if(floorNextFrame < frameRecord.length) {
            if(frameRecord[floorNextFrame + 1] != 0) {
                rightFrame = frameRecord[floorNextFrame + 1];
            }
        }

        float thisFrame = leftFrame * (1 - fracNextFrame) + rightFrame * fracNextFrame;
        long thisTime = (long) (thisFrame * fftHopSizeUs);

        return thisTime;
    }

    //@Override
    public synchronized long _getTime() {
        long nextTime = next.getTime();
        if(nextTime == AUGManager.UPDATE_FAIL) {
            return AUGManager.UPDATE_FAIL;
        }

        float nextFrame = (float) nextTime / fftHopSizeUs - removedFrame;
        int floorNextFrame = (int) Math.floor(nextFrame);
        float fracNextFrame = nextFrame - floorNextFrame;

        myLogD("--- [SEEK] ---");
        myLogD("nextTime = " + String.valueOf(nextTime));
        myLogD("nextFrame = " + String.valueOf(nextFrame));
        myLogD("removedFrame = " + String.valueOf(removedFrame));

        frameBuffer.flip();
        frameBuffer.position(floorNextFrame);
        frameBuffer.mark();

        float leftFrame = (frameBuffer.hasRemaining())? frameBuffer.get() : 0;
        float rightFrame = (frameBuffer.hasRemaining())? frameBuffer.get() : leftFrame;
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

        return outTemp;
    }

    private byte[] convertOutput(float[][] out) {
        short[] shortBufferArray = new short[fftHopSize * numChannel];
        for(int i = 0; i < numChannel; i++) {
            for(int j = 0; j < fftHopSize; j++) {
                shortBufferArray[j * numChannel + i] = (short)(out[i][j]);
            }
        }

        int byteBufferSize = fftHopSize * numChannel * BYTE_PER_SHORT;
        byte[] byteBufferArray = new byte[byteBufferSize];

        ByteBuffer byteBuffer = ByteBuffer.wrap(byteBufferArray);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortBufferArray);

        return byteBufferArray;
    }

    //

    @Override
    public void create() {
        super.create();

        fft = new FFT(fftFrameSize, fftSizeLog);
        window = new Window(fftFrameSize);
        util = new Util();

        speed = 1f;
    }

    @Override
    public void start() {
        super.start();

        normalFrame = 0;
        frame = 0;
        removedFrame = 0;
        floorLeftSample = 0;
        ceilRightSample = fftFrameAndHopSize;

        outFloatBuffer = new FloatBuffer[numChannel];
        for(int i = 0; i < numChannel; i++) {
            outFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
            outFloatBuffer[i].put(new float[fftFrameSize]);
        }
        frameBuffer = FloatBuffer.allocate(BUFFER_CAPACITY);
        frameRecord = new float[BUFFER_CAPACITY];

        phase = new float[numChannel][];
    }

    @Override
    public void operation() {
        super.operation();

        requireInput(ceilRightSample);

        float[][] out = new float[numChannel][];
        for(int i = 0; i < numChannel; i++) {
            float[] in = getFrame(inFloatBuffer[i], floorLeftSample - startSample, fftFrameAndHopSize);
            float[] left = Arrays.copyOfRange(in, 0, fftFrameSize);
            float[] right = Arrays.copyOfRange(in, fftHopSize, fftFrameAndHopSize);
            float[] inter = util.interpolate(phase, i, left, right, fracFrame);

            out[i] = setOutput(outFloatBuffer[i], inter);
        }

        /*
        frameBuffer.put(frame);
        if (frameBuffer.position() == frameBuffer.limit()) {
            int thisRemovedSize = frameBuffer.position() - BUFFER_QUEUE_CAPACITY;
            removedFrame += thisRemovedSize;

            frameBuffer.position(thisRemovedSize);
            frameBuffer.compact();
        }*/

        frameRecord[normalFrame] = frame;
        if(normalFrame >= frameRecord.length) {
            frameRecord = Arrays.copyOf(frameRecord, 2 * frameRecord.length);
        }
        normalFrame++;
        frame += speed;
        //speed += 0.0001;
        startSample = floorLeftSample;
        floorFrame = (int)(Math.floor(frame));
        fracFrame = frame - floorFrame;
        floorLeftSample = floorFrame * fftHopSize;
        ceilRightSample = floorLeftSample + fftFrameAndHopSize;

        //
        byte[] byteBufferArray = convertOutput(out);
        next.queueInput(byteBufferArray);

        if(inputEOS && inputQueue.isEmpty() && isUnderFlow()) {
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

        public float[] interpolate(float[][] phase, int ch, float[] leftReal, float[] rightReal, float ratio) {
            float[] leftMag = new float[fftFrameSizeCompact];
            float[] rightMag = new float[fftFrameSizeCompact];
            float[] leftPhase = new float[fftFrameSizeCompact];
            float[] rightPhase = new float[fftFrameSizeCompact];

            process(leftMag, leftPhase, leftReal);
            process(rightMag, rightPhase, rightReal);

            if(phase[ch] == null) {
                phase[ch] = Arrays.copyOf(leftPhase, fftFrameSizeCompact);
            } else {
                for(int i = 0; i < fftFrameSizeCompact; i++) {
                    phase[ch][i] += (rightPhase[i] - leftPhase[i]);
                }
            }

            float[] magnitude = new float[fftFrameSizeCompact];
            for (int i = 0; i < fftFrameSizeCompact; i++) {
                magnitude[i] = (1 - ratio) * leftMag[i] + ratio * rightMag[i];
            }

            float[] interRealTrunc = new float[fftFrameSizeCompact];
            float[] interImagTrunc = new float[fftFrameSizeCompact];

            pol2cart(interRealTrunc, interImagTrunc, magnitude, phase[ch]);

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

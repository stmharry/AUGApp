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

    private static final int BUFFER_CAPACITY = 16384;
    private static final int SAMPLE_RATE_TARGET = 0;
    private static final float FFT_TIME = 0.01f;
    private static final int FFT_HOP_RATIO = 4;

    private Util util;
    private FloatBuffer[] outFloatBuffer;
    private FloatBuffer frameBuffer;

    private float[][] phase;

    private float speed;
    private float frame;
    private int removedFrame;

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
        float nextFrame = (float) nextTime / fftHopSizeUs - removedFrame;
        int floorNextFrame = (int) (Math.floor(nextFrame));
        float fracNextFrame = nextFrame - floorNextFrame;

        // TODO: fix this

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

        speed = 1f;
    }

    @Override
    public void start() {
        super.start();

        frame = 0;
        removedFrame = 0;

        outFloatBuffer = new FloatBuffer[numChannel];
        for(int i = 0; i < numChannel; i++) {
            outFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
            outFloatBuffer[i].put(new float[fftFrameSize]);
        }
        frameBuffer = FloatBuffer.allocate(BUFFER_CAPACITY);

        phase = new float[numChannel][];
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

        requireInput(floorLeftSample + fftFrameAndHopSize);

        float[][] out = new float[numChannel][];
        for(int i = 0; i < numChannel; i++) {
            float[] in = getFrame(inFloatBuffer[i], floorLeftSample - startSample, fftFrameAndHopSize);
            float[] left = Arrays.copyOfRange(in, 0, fftFrameSize);
            float[] right = Arrays.copyOfRange(in, fftHopSize, fftFrameAndHopSize);
            float[] inter = util.interpolate(phase, i, left, right, fracFrame);

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
        //speed += 0.0001;
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

package com.example.harry.aug;

import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by harry on 15/8/2.
 */
public class Decoder extends AUGComponent {
    private static final String TAG = "Decoder";

    private MediaCodec mediaCodec;
    private MediaCodec.BufferInfo bufferInfo;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    private int percentage;

    public static Decoder newInstance() {
        return new Decoder();
    }

    public Decoder() {
        super(TAG);
    }

    @Override
    public synchronized long getTime() {
        long nextTime = next.getTime();
        return (nextTime == AUGManager.UPDATE_FAIL)? AUGManager.UPDATE_FAIL : nextTime;
    }

    /////////////
    // PROCESS //
    /////////////

    @Override
    public void create() {
        super.create();

        // Media Codec
        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);
            mediaCodec.configure(mediaFormat, null, null, 0);
            mediaCodec.start();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        // Buffer
        augManager.getMediaExtractor().selectTrack(0);
        bufferInfo = new MediaCodec.BufferInfo();
    }

    @Override
    public void start() {
        mediaCodec.flush();

        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();
    }

    @Override
    public void operation() {
        super.operation();

        // Input
        if(!inputEOS) {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
            if(inputBufferIndex >= 0) {
                int sampleSize = augManager.getMediaExtractor().readSampleData(inputBuffers[inputBufferIndex], 0);

                long presentationTimeUs = 0;
                if(sampleSize >= 0) {
                    presentationTimeUs = augManager.getMediaExtractor().getSampleTime();
                    percentage = (int)(100 * presentationTimeUs / duration);
                    // TODO: AudioPlayer Update
                } else {
                    setInputEOS();
                    sampleSize = 0;
                }

                mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, inputEOS? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                if(!inputEOS) augManager.getMediaExtractor().advance();
            }
        }

        // Output
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);

        if(outputBufferIndex >= 0) {
            int bufferSize = bufferInfo.size;
            if(bufferSize > 0) {
                ByteBuffer byteBuffer = outputBuffers[outputBufferIndex];
                byte[] byteArray = new byte[bufferSize];
                byteBuffer.get(byteArray).clear();

                next.queueInput(byteArray);
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        } else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mediaCodec.getOutputBuffers();
        } else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            mediaFormat = mediaCodec.getOutputFormat();
        } else if(outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "Output buffer drained!");
        }

        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
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

        if(mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
    }
}
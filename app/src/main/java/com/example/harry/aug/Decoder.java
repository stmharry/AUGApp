package com.example.harry.aug;

import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by harry on 15/8/2.
 */
public class Decoder extends Component implements Runnable {
    private static final String TAG = "Decoder";

    private MediaCodec mediaCodec;
    private MediaCodec.BufferInfo bufferInfo;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    private int percentage;

    /////////////
    // UTILITY //
    /////////////

    public Decoder(AUGManager augManager) {
        super(TAG, augManager);
    }

    public long seek() {
        return 0L; // TODO
    }

    public void seek(long time) {
        synchronized(mediaCodec) {
            reset();
        }
    }

    /////////////
    // PROCESS //
    /////////////

    @Override
    protected void initialize() {
        super.initialize();

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

        initializeBuffer();
    }

    @Override
    protected void initializeBuffer() {
        mediaCodec.flush();
        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();
    }

    @Override
    protected boolean loop() {
        return super.loop();
    }

    @Override
    protected void action() {
        super.action();

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
    protected void terminate() {
        super.terminate();

        if(mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
    }
}
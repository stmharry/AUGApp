package com.example.harry.aug;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by harry on 15/8/2.
 */
public class AudioPlayer extends Component implements Runnable {
    private static final String TAG = "AudioPlayer";

    private AudioTrack audioTrack;

    private int streamType;
    private int channelConfiguration;
    private int audioFormat;
    private int bufferSize;
    private int mode;

    public AudioPlayer(AUGManager augManager) {
        super(TAG, augManager);
    }

    @Override
    public synchronized long getTime() {
        return (audioTrack.getPlaybackHeadPosition() * S_TO_US) / sampleRate;
    }

    /////////////
    // PROCESS //
    /////////////

    @Override
    protected void initializeElement() {
        super.initializeElement();

        // Audio Track
        streamType = AudioManager.STREAM_MUSIC;
        channelConfiguration = (numChannel == 1)? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, audioFormat);
        mode = AudioTrack.MODE_STREAM;

        audioTrack = new AudioTrack(streamType, sampleRate, channelConfiguration, audioFormat, bufferSize, mode);

        Log.d(TAG, "Track info: "
                + "sampleRate = " + String.valueOf(sampleRate) + ", "
                + "bufferSize = " + String.valueOf(bufferSize));
    }

    @Override
    protected void initializeBuffer() {
        super.initializeBuffer();

        audioTrack.pause();
        audioTrack.flush();
        audioTrack.play();
    }

    @Override
    protected void operation() {
        if (!inputEOS) {
            byte[] in = dequeueInput(TIMEOUT_US);

            if (in != null) {
                audioTrack.write(in, 0, in.length);
            }
        }

        if (inputEOS & inputQueue.isEmpty()) {
            setOutputEOS();
        }
    }

    @Override
    protected void terminate() {
        super.terminate();

        if(audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
        }
    }
}

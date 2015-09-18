package com.example.harry.aug;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Created by harry on 9/13/15.
 */
public class Accelerometer {
    private static final String TAG = "Accelerometer";
    private static final float PI = (float) Math.PI;
    private static final float MILLIS_TO_SECONDS = 1 / (float) TimeUnit.SECONDS.toMillis(1);

    private static final int NUM_CHANNEL = 3;
    private static final int NUM_CHANNEL_PLUS_ONE = 4;
    private static final int SENSOR_DELAY = 20;
    private static final float FFT_HOP_TIME = 0.1f;
    private static final float FFT_TIME = 4f;
    private static final int FFT_UPSAMPLE = 16;
    private static final int FFT_CUTOFF_BPM = 60;
    private static final float RATIO_CUTOFF = 20;
    private static final int BUFFER_CAPACITY = 512;

    private AUGActivity augActivity;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private AUGSensorEventListener augSensorEventListener;

    private FloatBuffer[] inFloatBuffer;
    private FFT fft;

    private float frequency;
    private float ratio;

    private float[] acceleration;
    private float magnitude;
    private float tMean;
    private float tStd;
    private float time;
    private float timeLast;
    private float timeClosestOnset;
    private float timeNextOnset;
    private float timeOnsetLast;
    private boolean tracking;
    private boolean onset;
    private boolean onsetFired;

    private int fftSize;
    private int fftHopSize;
    private int fftSizePadded;
    private int fftSizePaddedCompact;

    private long timeMs;
    private long timeLastMs;
    private long timeStartMs;
    private float sumN;
    private float sum;
    private float sumOfSquared;

    public Accelerometer(AUGActivity augActivity) {
        this.augActivity = augActivity;
        this.sensorManager = (SensorManager) augActivity.getSystemService(AUGActivity.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.augSensorEventListener = new AUGSensorEventListener();
        this.inFloatBuffer = new FloatBuffer[NUM_CHANNEL_PLUS_ONE];
        for(int i = 0; i < NUM_CHANNEL_PLUS_ONE; i++) {
            inFloatBuffer[i] = FloatBuffer.allocate(BUFFER_CAPACITY);
        }
        this.fftSize = (int)(1000 * FFT_TIME / SENSOR_DELAY);
        this.fftHopSize = (int)(1000 * FFT_HOP_TIME / SENSOR_DELAY);
        this.fft = new FFT(fftSize * FFT_UPSAMPLE);
        this.fftSizePadded = fft.getN();
        this.fftSizePaddedCompact = fft.getK();
    }

    //

    public float getFrequency() {
        return frequency;
    }

    public float getRatio() {
        return ratio;
    }

    public float[] getAcceleration() {
        return acceleration;
    }

    public float getMagnitude() {
        return magnitude;
    }

    public float getTMean() {
        return tMean;
    }

    public float getTStd() {
        return tStd;
    }

    public float getTimeNextOnset() {
        return timeNextOnset;
    }

    public float getRelativeTimeNextOnset() {
        return (timeNextOnset - time);
    }

    public boolean isTracking() {
        return tracking;
    }

    public boolean isOnset() {
        return onset;
    }

    public void register() {
        sensorManager.registerListener(augSensorEventListener, accelerometer, SENSOR_DELAY);
        timeStartMs = System.currentTimeMillis();
    }

    public void unregister() {
        sensorManager.unregisterListener(augSensorEventListener);
    }

    //

    private float[] getFrame(FloatBuffer buffer) {
        float[] in = new float[fftSizePadded];

        buffer.flip();
        buffer.get(in, 0, fftSize);
        buffer.position(fftHopSize);
        buffer.compact();

        return in;
    }

    private void processTime() {
        time = (timeMs - timeStartMs) * MILLIS_TO_SECONDS;
        timeLast = (timeLastMs - timeStartMs) * MILLIS_TO_SECONDS;

        if(timeLastMs != 0) {
            float diffTime = (timeMs - timeLastMs) * MILLIS_TO_SECONDS;

            sumN += 1;
            sum += diffTime;
            sumOfSquared += diffTime * diffTime;

            tMean = sum / sumN;
            tStd = (float) Math.sqrt(sumOfSquared / sumN - tMean * tMean);
        }
        timeLastMs = timeMs;
    }

    public void processOnset() {
        onset = !onsetFired && (timeClosestOnset != 0) && (timeLast < timeOnsetLast) && (time > timeClosestOnset);
        onsetFired |= onset;
        timeOnsetLast = timeClosestOnset;

        /*
        String str = "[ Onset detection ]";
        str += String.format("[ Time = %5.2f ]", time);
        str += String.format("[ Onset Time = %5.2f ]", timeClosestOnset);
        str += "[ onsetFired = " + String.valueOf(onsetFired) + " ]";
        str += "[ onset = " + String.valueOf(onset) + " ]";
        Log.d(TAG, str);
        */
    }

    private void processAcceleration() {
        magnitude = 0;
        for(int i = 0; i < NUM_CHANNEL; i++) {
            magnitude += (acceleration[i] * acceleration[i]);
            inFloatBuffer[i].put(acceleration[i]);
        }
        inFloatBuffer[NUM_CHANNEL].put(magnitude);

        if(inFloatBuffer[0].position() >= fftSize) {
            float[] inReal = null;
            float[] inImag = null;
            float[] fftSquaredSum = new float[fftSizePaddedCompact];

            for(int i = 0; i < NUM_CHANNEL_PLUS_ONE; i++) {
                inReal = getFrame(inFloatBuffer[i]);
                inImag = new float[fftSizePadded];

                Util.center(inReal, fftSize);
                fft.fft(inReal, inImag, true);

                if(i != NUM_CHANNEL)
                    for(int j = 0; j < fftSizePaddedCompact; j++)
                        fftSquaredSum[j] += (inReal[j] * inReal[j] + inImag[j] * inImag[j]);
            }

            int cutOffIndex = (int) Math.ceil(Util.bpmToFreq(FFT_CUTOFF_BPM) * tMean * fftSizePadded);;
            float[] maxStat = Util.max(fftSquaredSum, cutOffIndex);
            int maxIndex = (int) maxStat[1];

            frequency = maxStat[1] / fftSizePadded / tMean;
            ratio = maxStat[0] / Util.mean(fftSquaredSum);
            tracking = (maxIndex > cutOffIndex) && (ratio > RATIO_CUTOFF);

            if(tracking) {
                float maxAngularFrequency = 2 * PI * frequency;
                float maxPhase = (float) Math.atan2(inImag[maxIndex], inReal[maxIndex]);
                float timeToNextBeatFromShifted = - Util.smallestMagnitudeCoterminalAngle(maxPhase + maxAngularFrequency * FFT_TIME) / maxAngularFrequency;

                timeClosestOnset = time + timeToNextBeatFromShifted;
                timeNextOnset = timeClosestOnset + ((timeToNextBeatFromShifted > 0)? 0 : 1 / frequency);
                onsetFired = false;
            }

            /*
            String str = "[ Predicting beat ]";
            str += String.format("[ Time: %5.2f ~ %5.2f ]", time - FFT_TIME, time);
            str += String.format("[ Frequency = %5.3f ]", frequency);
            str += String.format("[ PhaseStart = %+6.1f˚ ]", Util.radToDeg(maxPhase));
            str += String.format("[ Ratio = %5.1f ]", ratio);
            str += String.format("[ Onset Time = %5.2f ]", timeClosestOnset);
            Log.d(TAG, str);*/
        }
    }

    //

    private class AUGSensorEventListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;

            timeMs = System.currentTimeMillis();
            acceleration = sensorEvent.values;

            processTime();
            processOnset();
            processAcceleration();
        }
    }
}

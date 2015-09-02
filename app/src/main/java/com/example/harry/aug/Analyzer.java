package com.example.harry.aug;

/**
 * Created by harry on 8/28/15.
 */
public abstract class Analyzer extends AUGComponent {
    protected static final int BUFFER_CAPACITY = 4096;
    protected static final float FFT_TIME = 0.01f;
    protected static final int HOP_RATIO = 4;

    public Analyzer(String TAG) {
        super(TAG);
    }
}

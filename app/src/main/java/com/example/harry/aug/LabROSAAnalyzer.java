package com.example.harry.aug;

/**
 * Created by harry on 15/8/29.
 */
public class LabROSAAnalyzer extends AUGAnalyzer {
    private static final String TAG = "LabROSAAnalyzer";

    //

    public static LabROSAAnalyzer newInstance() {
        return new LabROSAAnalyzer();
    }

    public LabROSAAnalyzer() {
        super(TAG);
    }

    @Override
    public synchronized long getTime() {
        return 0;
    }

    //

    @Override
    public void create() {
        super.create();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void operation() {
        super.operation();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

}

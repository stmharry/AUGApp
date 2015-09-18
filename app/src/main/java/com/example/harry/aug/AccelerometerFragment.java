package com.example.harry.aug;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AccelerometerFragment extends AUGFragment {
    static {
        //System.loadLibrary("AUG");
    }
    //public native String getStringNative();

    private static final String TAG = "AccelerometerFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_accelerometer;
    private static final int NAME_RESOURCE = R.string.fragment_accelerometer;

    private FrameLayout frameLayout;
    private TextView accelerometerView;
    private Accelerometer accelerometer;

    //

    public static AccelerometerFragment newInstance() {
        AccelerometerFragment fragment = new AccelerometerFragment();
        fragment.setLayoutResource(LAYOUT_RESOURCE);
        fragment.setTitleResource(NAME_RESOURCE);
        return fragment;
    }

    public AccelerometerFragment() {}

    //

    private int color = Color.WHITE; // TODO: remove
    public void updateView() {
        String str = "Accelerometer\n";
        str += ("Tracking =" + String.valueOf(accelerometer.isTracking()) + "\n");
        str += String.format("Frequency = %5.1f\n", Util.freqToBPM(accelerometer.getFrequency()));
        str += String.format("Ratio = %4.0f\n", accelerometer.getRatio());
        accelerometerView.setText(str);

        if(accelerometer.isOnset()) {
            color = Color.RED;
        } else {
            color = Color.argb((int)(Color.alpha(color) * 0.9), 255, 0, 0);
        }
        accelerometerView.setBackgroundColor(color);
    }

    //

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        frameLayout = (FrameLayout) augActivity.findViewById(R.id.accelerometer);
        accelerometerView = new TextView(augActivity);
        accelerometerView.setTextSize(30);

        frameLayout.addView(accelerometerView);
        //accelerometerView.setText(getStringNative());
        accelerometer = new Accelerometer(augActivity);
    }

    @Override
    public void onResume() {
        super.onResume();
        accelerometer.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        accelerometer.unregister();
    }
}

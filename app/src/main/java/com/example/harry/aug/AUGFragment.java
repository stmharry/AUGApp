package com.example.harry.aug;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AUGFragment extends Fragment {
    private static final String TAG = "AUGFragment";

    protected AUGActivity augActivity;
    protected int layoutResource;
    protected int titleResource;

    //

    public void setLayoutResource(int layoutResource) {
        this.layoutResource = layoutResource;
    }

    public int getLayoutResource() {
        return layoutResource;
    }

    public void setTitleResource(int titleResource) {
        this.titleResource = titleResource;
    }

    public int getTitleResource() {
        return titleResource;
    }

    public void setAugActivity(AUGActivity augActivity) {
        this.augActivity = augActivity;
    }

    //

    public void updateView() {}

    public void onAUGManagerDestroy() {}

    protected void startAUGManager() {}

    protected void pauseAUGManager() {}

    protected void stopAUGManager() {}

    //

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(layoutResource, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        startAUGManager();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseAUGManager();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopAUGManager();
    }
}

package com.example.harry.aug;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class PlayerFragment extends AUGFragment {
    private static final String TAG = "PlayerFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_player;
    private static final int NAME_RESOURCE = R.string.fragment_player;

    private AUGManager augManager;

    //

    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        fragment.setLayoutResource(LAYOUT_RESOURCE);
        fragment.setTitleResource(NAME_RESOURCE);

        return fragment;
    }

    public PlayerFragment() {}

    //

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        augActivity.replaceLayout(augActivity.AUG_LAYOUT_MINOR, null);

        AUGComponent[] AUGComponents = new AUGComponent[]{
                Decoder.newInstance(),
                PhaseVocoderAnalyzer.newInstance(),
                AudioPlayer.newInstance()};
        TimeUpdater timeUpdater = new TimeUpdater();

        SongManager songManager = augActivity.getSongManager();
        Song song = songManager.getSongByFragment(this);
        String songData = song.getData();

        augManager = new AUGManager(augActivity, AUGComponents, timeUpdater);
        augManager.setDataSource(songData);
        augManager.prepare();
    }

    @Override
    public void onStart() {
        super.onStart();
        augManager.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        augManager.pause();
        //augActivity.getSongManager().saveTitleKeyToPlay();
    }

    @Override
    public void onStop() {
        super.onStop();
        augManager.stop();
    }

    //

    private class TimeUpdater extends AUGTimeUpdater {
        private final int UPDATE_INTERVAL = 50;

        private LinearLayout playerLayout;
        private TextView allTimeView;
        private TextView currentTimeView;

        @Override
        public void run() {
            if(playerLayout == null) {
                playerLayout = (LinearLayout) augActivity.findViewById(R.id.player);
            }

            if(allTimeView == null) {
                allTimeView = new TextView(augActivity);
                allTimeView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                playerLayout.addView(allTimeView);

                float time = (float) augManager.getAllTime() / TimeUnit.SECONDS.toMicros(1);
                allTimeView.setText(String.format("All: %.2f s", time));
            }

            long timeUs = augManager.getTime();

            if(timeUs != AUGManager.UPDATE_FAIL) {
                if(currentTimeView == null) {
                    currentTimeView = new TextView(augActivity);
                    currentTimeView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    playerLayout.addView(currentTimeView);
                }

                float time = (float) timeUs / TimeUnit.SECONDS.toMicros(1);
                currentTimeView.setText(String.format("Current: %.2f s", time));
            }

            if(loop) {
                augManager.getHandler().postDelayed(this, UPDATE_INTERVAL);
            }
        }
    }
}
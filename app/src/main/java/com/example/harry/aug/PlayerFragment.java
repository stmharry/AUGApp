package com.example.harry.aug;

import android.os.Bundle;

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

        SongManager.Song song = augActivity.getSongManager().getSongToPlay();
        String songFieldData = (String) song.get(SongManager.FIELD_DATA);

        AUGComponent[] AUGComponents = new AUGComponent[]{
                Decoder.newInstance(),
                PhaseVocoderAnalyzer.newInstance(),
                AudioPlayer.newInstance()};

        augManager = new AUGManager(augActivity, AUGComponents);
        augManager.setDataSource(songFieldData);
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
        augActivity.getSongManager().saveTitleKeyToPlay();
    }

    @Override
    public void onStop() {
        super.onStop();
        augManager.stop();
    }
}
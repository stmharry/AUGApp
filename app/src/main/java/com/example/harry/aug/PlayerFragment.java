package com.example.harry.aug;

import android.os.Bundle;
import android.provider.MediaStore;

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

        augManager = new AUGManager(augActivity);
        augManager.setDataSource((String) (song.get(MediaStore.Audio.Media.DATA)));
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
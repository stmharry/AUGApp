package com.example.harry.aug;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

public class AnalyzerFragment extends AUGFragment {
    private static final String TAG = "AnalyzerFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_analyzer;
    private static final int NAME_RESOURCE = R.string.fragment_analyzer;

    private AUGManager augManager;
    private boolean noSong;

    //

    public static AnalyzerFragment newInstance() {
        AnalyzerFragment fragment = new AnalyzerFragment();
        fragment.setLayoutResource(LAYOUT_RESOURCE);
        fragment.setTitleResource(NAME_RESOURCE);
        return fragment;
    }

    public AnalyzerFragment() {}

    //

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SongManager.Song song = augActivity.getSongManager().getSongToAnalyze();

        if(song == null) {
            noSong = true;
            return;
        }

        String songTitle = (String) song.get(SongManager.FIELD_TITLE);
        String songFieldData = (String) song.get(SongManager.FIELD_DATA);

        TextView analyzerInfoTextView = (TextView) augActivity.findViewById(R.id.analyzer_info);
        analyzerInfoTextView.setText("Analyzing: " + songTitle);

        AUGComponent[] AUGComponents = new AUGComponent[]{
                Decoder.newInstance(),
                LabROSAAnalyzer.newInstance()};

        augManager = new AUGManager(augActivity, AUGComponents);
        augManager.setDataSource(songFieldData);
        augManager.prepare();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!noSong) {
            augManager.start();
        }
    }

    // TODO: onpause

    @Override
    public void onStop() {
        super.onStop();
        if(!noSong) {
            augManager.stop();
        }
    }
}
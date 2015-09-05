package com.example.harry.aug;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AnalyzerFragment extends AUGFragment {
    private static final String TAG = "AnalyzerFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_analyzer;
    private static final int NAME_RESOURCE = R.string.fragment_analyzer;

    public AUGComponent
            ANALYZER_DECODER,
            ANALYZER_ANALYZER;

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

        AUGComponent[] AUGComponents = new AUGComponent[]{
                ANALYZER_DECODER = Decoder.newInstance(),
                ANALYZER_ANALYZER = LabROSAAnalyzer.newInstance()};
        TimeUpdater timeUpdater = new TimeUpdater();

        SongManager.Song song = augActivity.getSongManager().getSongToAnalyze();

        if(song == null) {
            noSong = true;
            return;
        }

        String songTitle = (String) song.get(SongManager.FIELD_TITLE);
        String songFieldData = (String) song.get(SongManager.FIELD_DATA);

        TextView analyzerInfoTextView = (TextView) augActivity.findViewById(R.id.analyzer_info);
        analyzerInfoTextView.setText("Analyzing: " + songTitle); //

        augManager = new AUGManager(augActivity, AUGComponents, timeUpdater);
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

    //

    private class TimeUpdater extends AUGTimeUpdater {
        private final int UPDATE_INTERVAL = 100;

        private LinearLayout analyzerLayout;
        private TextView infoView;

        @Override
        public void run() {
            if(analyzerLayout == null) {
                analyzerLayout = (LinearLayout) augActivity.findViewById(R.id.analyzer);
            }

            String str;
            LabROSAAnalyzer labROSAAnalyzer = (LabROSAAnalyzer) ANALYZER_ANALYZER;
            LabROSAAnalyzer.State state = labROSAAnalyzer.getState();
            switch(state) {
                case STATE_ONSET:
                    long curTime = augManager.getTime();
                    long allTime = augManager.getAllTime();
                    str = String.format("STATE_ONSET: %.3f (%d/%d)", (float) curTime / allTime, curTime, allTime);
                    break;
                case STATE_TEMPO:
                    int curIteration = labROSAAnalyzer.getSelection();
                    int allIteration = labROSAAnalyzer.getSelectionSize();
                    str = String.format("STATE_TEMPO: %.2f (%d/%d)", (float) curIteration / allIteration, curIteration, allIteration);
                    break;
                case STATE_BEAT:
                    str = "STATE_BEAT";
                    break;
                default:
                    str = "default";
                    break;
            }

            if(infoView == null) {
                infoView = new TextView(augActivity);
                infoView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                analyzerLayout.addView(infoView);
            }

            infoView.setText(str);

            if(loop) {
                augManager.getHandler().postDelayed(this, UPDATE_INTERVAL);
            }
        }
    }
}
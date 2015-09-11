package com.example.harry.aug;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AnalyzerFragment extends AUGFragment {
    private static final String TAG = "AnalyzerFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_analyzer;
    private static final int NAME_RESOURCE = R.string.fragment_analyzer;

    public LabROSAAnalyzer ANALYZER_ANALYZER;

    private AUGManager augManager;
    private Song song;

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
    public void onAUGManagerStop() {
        song.setBPM(ANALYZER_ANALYZER.getBPM());
        song.setBeatScore(ANALYZER_ANALYZER.getBeatScore());
        long[] beatTime = ANALYZER_ANALYZER.getBeatTime();
        song.setBeatCount(beatTime.length);
        song.setBeatTime(beatTime);

        augActivity.getSongManager().dbUpdate(song);
    }

    @Override
    public void startAUGManager() {
        SongManager songManager = augActivity.getSongManager();
        song = songManager.getSongByFragment(this);

        if(song != null) {
            ((TextView) augActivity.findViewById(R.id.analyzer_info)).setText("Analyzing: " + song.getTitle()); //

            augManager.setSong(song);
            augManager.prepare();
            augManager.start();
        }
    }

    @Override
    public void pauseAUGManager() {
        if(song != null) {
            augManager.pause();
        }
    }

    @Override
    public void stopAUGManager() {
        if(song != null) {
            augManager.stop();
        }
    }

    //

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AUGComponent[] AUGComponents = new AUGComponent[]{
                Decoder.newInstance(),
                ANALYZER_ANALYZER = LabROSAAnalyzer.newInstance()};
        TimeUpdater timeUpdater = new TimeUpdater();

        augManager = new AUGManager(augActivity, this, AUGComponents, timeUpdater);
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
            LabROSAAnalyzer.State state = ANALYZER_ANALYZER.getState();
            switch(state) {
                case STATE_ONSET:
                    long curTime = augManager.getTime();
                    long allTime = augManager.getAllTime();
                    str = String.format("STATE_ONSET: %.3f (%d/%d)", (float) curTime / allTime, curTime, allTime);
                    break;
                case STATE_TEMPO:
                    int curIteration = ANALYZER_ANALYZER.getSelection();
                    int allIteration = ANALYZER_ANALYZER.getSelectionSize();
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
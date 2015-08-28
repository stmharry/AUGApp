package com.example.harry.aug;

import android.os.Bundle;

public class AnalyzerFragment extends BaseFragment {
    private static final String TAG = "AnalyzerFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_analyzer;
    private static final int NAME_RESOURCE = R.string.fragment_analyzer;

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
    }
}
package com.example.harry.aug;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ListFragment extends AUGFragment {
    private static final String TAG = "ListFragment";
    private static final int LAYOUT_RESOURCE = R.layout.fragment_list;
    private static final int NAME_RESOURCE = R.string.fragment_list;

    //

    public static ListFragment newInstance() {
        ListFragment fragment = new ListFragment();
        fragment.setLayoutResource(LAYOUT_RESOURCE);
        fragment.setTitleResource(NAME_RESOURCE);
        return fragment;
    }

    public ListFragment() {}

    //

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView songListView = (ListView) augActivity.findViewById(R.id.list_song_list);
        songListView.setAdapter(new SongAdapter(augActivity, augActivity.getSongManager()));
        songListView.setOnItemClickListener(new SongListOnItemClickListener());
    }

    //

    private class SongListOnItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SongManager songManager = augActivity.getSongManager();
            songManager.setSongByFragment(augActivity.AUG_FRAGMENT_PLAYER, songManager.getSongByTitleKey((String) view.getTag()));
            augActivity.replaceLayout(augActivity.AUG_LAYOUT_MAJOR, augActivity.AUG_FRAGMENT_PLAYER);
        }
    }
}

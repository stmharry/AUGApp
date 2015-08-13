package com.example.harry.aug;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by harry on 15/7/24.
 */
public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songList;
    private LayoutInflater songInflater;

    public SongAdapter(Context c, ArrayList<Song> songList) {
        this.songList = songList;
        this.songInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() { return songList.size(); }

    @Override
    public Object getItem(int arg0) { return null; }

    @Override
    public long getItemId(int arg0) { return 0; }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LinearLayout songLayout = (LinearLayout) songInflater.inflate(R.layout.song, viewGroup, false);
        TextView titleView = (TextView) songLayout.findViewById(R.id.song_title);
        TextView artistView = (TextView) songLayout.findViewById(R.id.song_artist);
        TextView durationView = (TextView) songLayout.findViewById(R.id.song_duration);

        Song song = songList.get(position);
        titleView.setText(song.getTitle());
        artistView.setText(song.getArtist());
        durationView.setText(song.getDuration());
        songLayout.setTag(song.getTitleKey());

        return songLayout;
    }
}
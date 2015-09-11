package com.example.harry.aug;

import android.content.Context;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by harry on 15/7/24.
 */
public class SongAdapter extends BaseAdapter {
    private SongManager songManager;
    private LayoutInflater songInflater;

    public SongAdapter(Context c, SongManager songManager) {
        this.songManager = songManager;
        this.songInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() { return songManager.getSongList().size(); }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LinearLayout songLayout = (LinearLayout) songInflater.inflate(R.layout.song, viewGroup, false);
        TextView titleView = (TextView) songLayout.findViewById(R.id.song_title);
        TextView artistView = (TextView) songLayout.findViewById(R.id.song_artist);
        TextView bpmView = (TextView) songLayout.findViewById(R.id.song_bpm);
        TextView durationView = (TextView) songLayout.findViewById(R.id.song_duration);

        Song song = songManager.getSongList().get(position);
        String text;

        titleView.setText(song.getTitle());
        artistView.setText(song.getArtist());

        float bpm = song.getBPM();
        text = (bpm == 0)? "N/A" : String.format("%.0f BPM", bpm);
        bpmView.setText(text);

        Time time = new Time();
        time.set(song.getDuration());
        durationView.setText(time.format("%M:%S"));

        songLayout.setTag(song.getTitleKey());
        return songLayout;
    }
}
package com.example.harry.aug;

import android.content.Context;
import android.provider.MediaStore.Audio.Media;
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
    public Object getItem(int arg0) { return null; }

    @Override
    public long getItemId(int arg0) { return 0; }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LinearLayout songLayout = (LinearLayout) songInflater.inflate(R.layout.song, viewGroup, false);
        TextView titleView = (TextView) songLayout.findViewById(R.id.song_title);
        TextView artistView = (TextView) songLayout.findViewById(R.id.song_artist);
        TextView durationView = (TextView) songLayout.findViewById(R.id.song_duration);

        SongManager.Song song = songManager.getSongList().get(position);
        titleView.setText((String) (song.get(Media.TITLE)));
        artistView.setText((String) (song.get(Media.ARTIST)));

        Time time = new Time();
        time.set((long)(song.get(Media.DURATION)));
        durationView.setText(time.format("%M:%S"));

        songLayout.setTag(song.get(Media.TITLE_KEY));
        return songLayout;
    }
}
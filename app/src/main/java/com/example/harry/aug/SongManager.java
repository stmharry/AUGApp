package com.example.harry.aug;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by harry on 8/27/15.
 */
public class SongManager {
    private static final int TITLE_KEY_RESOURCE = R.string.fragment_player_title_key;
    private static final String[] FIELD = new String[]{
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE_KEY,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION};

    private AUGActivity augActivity;
    private ArrayList<Song> songList;
    private String titleKeyToPlay;

    //

    public SongManager(AUGActivity augActivity) {
        this.augActivity = augActivity;
        this.makeSongList();
        this.loadTitleKeyToPlay();

    }

    //

    public ArrayList<Song> getSongList() {
        return songList;
    }

    public void setTitleKeyToPlay(String titleKeyToPlay) {
        this.titleKeyToPlay = titleKeyToPlay;
    }

    //

    public void loadTitleKeyToPlay() {
        titleKeyToPlay = augActivity.getPreferences(Context.MODE_PRIVATE).getString(
                augActivity.getString(TITLE_KEY_RESOURCE),
                (String) songList.get(0).get(MediaStore.Audio.Media.TITLE_KEY));
    }

    public void saveTitleKeyToPlay() {
        augActivity.getPreferences(Context.MODE_PRIVATE).edit().putString(augActivity.getString(TITLE_KEY_RESOURCE), titleKeyToPlay).apply();
    }

    public Song getSongToPlay() {
        for(Song song: songList) {
            if(titleKeyToPlay.equals(song.get(MediaStore.Audio.Media.TITLE_KEY))) {
                return song;
            }
        }
        return null;
    }

    private void makeSongList() {
        songList = new ArrayList<>();

        Cursor cursor = augActivity.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if(cursor.moveToFirst()) {
            do {
                //

                int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                long duration = cursor.getLong(durationColumn);
                if(duration > 6000) continue; // TODO: remove this constraint

                //

                songList.add(new Song(cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();

        Collections.sort(songList, new SongComparator());
    }

    //

    public class Song {
        private Map<String, Object> map;

        public Song(Cursor cursor) {
            map = new HashMap<>(FIELD.length);

            for (String field : FIELD) {
                int column = cursor.getColumnIndex(field);

                switch (cursor.getType(column)) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        map.put(field, cursor.getLong(column));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        map.put(field, cursor.getDouble(column));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        map.put(field, cursor.getString(column));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                    case Cursor.FIELD_TYPE_BLOB:
                        break;
                }
            }
        }

        public Object get(String key) {
            return map.get(key);
        }
    }

    private class SongComparator implements Comparator<Song> {
        @Override
        public int compare(Song lhs, Song rhs) {
            String titleL = (String) (lhs.get(MediaStore.Audio.Media.TITLE));
            String titleR = (String) (rhs.get(MediaStore.Audio.Media.TITLE));
            return titleL.compareTo(titleR);
        }
    }
}

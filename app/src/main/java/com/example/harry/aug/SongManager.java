package com.example.harry.aug;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by harry on 8/27/15.
 */
public class SongManager {
    private static final String TAG = "SongManager";
    private static final String TITLE_KEY = "TITLE_KEY";
    private static final String BPM = "BPM";

    public static String
            FIELD_DATA,
            FIELD_TITLE_KEY,
            FIELD_TITLE,
            FIELD_ARTIST,
            FIELD_DURATION;
    private static final String[] FIELD = new String[] {
            FIELD_DATA      = MediaStore.Audio.Media.DATA,
            FIELD_TITLE_KEY = MediaStore.Audio.Media.TITLE_KEY,
            FIELD_TITLE     = MediaStore.Audio.Media.TITLE,
            FIELD_ARTIST    = MediaStore.Audio.Media.ARTIST,
            FIELD_DURATION  = MediaStore.Audio.Media.DURATION};

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
                TITLE_KEY,
                (String) songList.get(0).get(FIELD_TITLE_KEY));
    }

    public void saveTitleKeyToPlay() {
        augActivity.getPreferences(Context.MODE_PRIVATE).edit().putString(TITLE_KEY, titleKeyToPlay).apply();
    }

    public Song getSongToPlay() {
        for(Song song: songList) {
            if(titleKeyToPlay.equals(song.get(FIELD_TITLE_KEY))) {
                return song;
            }
        }
        return null;
    }

    public Song getSongToAnalyze() {
        for(Song song: songList) {
            //if(augActivity.getPreferences(Context.MODE_PRIVATE).getFloat(BPM + song.get(FIELD_TITLE_KEY), 0) == 0) {\
            if(song.get(FIELD_TITLE).equals("Ringtone")) { // TODO: remove this
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

                String title = cursor.getString(cursor.getColumnIndex(FIELD_TITLE));
                if(title.startsWith("2015")) continue; // TODO: remove this

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

            for(String field : FIELD) {
                int column = cursor.getColumnIndex(field);

                switch(cursor.getType(column)) {
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
            String titleL = (String) (lhs.get(FIELD_TITLE));
            String titleR = (String) (rhs.get(FIELD_TITLE));
            return titleL.compareTo(titleR);
        }
    }
}

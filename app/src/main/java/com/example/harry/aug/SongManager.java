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
    private static final String FIELD_TITLE_KEY = "FIELD_TITLE_KEY";
    private static final String FIELD_BPM = "FIELD_BPM";

    private AUGActivity augActivity;
    private ArrayList<Song> songList;
    private Map <AUGFragment, Song> songMap;

    //

    public SongManager(AUGActivity augActivity) {
        this.augActivity = augActivity;
        this.songList = makeSongList();
        this.songMap = new HashMap<>(augActivity.AUG_FRAGMENT_MAJOR.length + augActivity.AUG_FRAGMENT_MINOR.length);
    }

    public ArrayList<Song> getSongList() {
        return songList;
    }

    public Song getSongByTitleKey(String titleKey) {
        for(Song song: songList) {
            if(titleKey.equals(song.get(Song.FIELD_TITLE_KEY))) {
                return song;
            }
        }
        return null;
    }

    public Song getSongByTitle(String title) {
        for(Song song: songList) {
            if(title.equals(song.get(Song.FIELD_TITLE))) {
                return song;
            }
        }
        return null;
    }

    public Song getSongByFragment(AUGFragment augFragment) {
        Song song = songMap.get(augFragment);
        if(song == null) {
            if(augFragment == augActivity.AUG_FRAGMENT_PLAYER) {
                String titleKey = augActivity.getPreferences(Context.MODE_PRIVATE).getString(FIELD_TITLE_KEY, null);
                song = (titleKey == null)? songList.get(0) : getSongByTitleKey(titleKey);
            } else if(augFragment == augActivity.AUG_FRAGMENT_ANALYZER) {
                for(Song s: songList) {
                    if(augActivity.getPreferences(Context.MODE_PRIVATE).getFloat(FIELD_BPM + s.get(Song.FIELD_TITLE_KEY), 0) == 0) {
                        song = s;
                        break;
                    }
                }
            }
            songMap.put(augFragment, song);
        }
        return song;
    }

    public void setSongByFragment(AUGFragment augFragment, Song song) {
        songMap.put(augFragment, song);
    }

    /*
    public void saveTitleKeyToPlay() {
        augActivity.getPreferences(Context.MODE_PRIVATE).edit().putString(FIELD_TITLE_KEY, titleKeyToPlay).apply();
    }
    */

    private ArrayList<Song> makeSongList() {
        ArrayList<Song> songList = new ArrayList<>();

        Cursor cursor = augActivity.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if(cursor.moveToFirst()) {
            do {
                if(cursor.getString(cursor.getColumnIndex(Song.FIELD_TITLE)).startsWith("2015")) continue; // TODO: remove constraint
                songList.add(new Song(cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        Collections.sort(songList, new SongComparator());

        return songList;
    }

    //

    private class SongComparator implements Comparator<Song> {
        @Override
        public int compare(Song lhs, Song rhs) {
            String titleL = (String) (lhs.get(Song.FIELD_TITLE));
            String titleR = (String) (rhs.get(Song.FIELD_TITLE));
            return titleL.compareTo(titleR);
        }
    }
}

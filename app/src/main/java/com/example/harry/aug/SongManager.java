package com.example.harry.aug;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
    private AUGActivity augActivity;
    private SQLiteDatabase database;
    private ArrayList<Song> songList;
    private Map <AUGFragment, Song> songMap;

    //

    public SongManager(AUGActivity augActivity) {
        this.augActivity = augActivity;
        this.database = AUGDBHelper.getDatabase(augActivity);
        this.songList = makeSongList();
        this.songMap = new HashMap<>(augActivity.AUG_FRAGMENT_MAJOR.length + augActivity.AUG_FRAGMENT_MINOR.length);
    }

    public ArrayList<Song> getSongList() {
        return songList;
    }

    public Song getSongByTitleKey(String titleKey) {
        for(Song song: songList) {
            if(titleKey.equals(song.getTitleKey())) {
                return song;
            }
        }
        return null;
    }

    public Song getSongByTitle(String title) {
        for(Song song: songList) {
            if(title.equals(song.getTitle())) {
                return song;
            }
        }
        return null;
    }

    public Song getSongByFragment(AUGFragment augFragment) {
        Song song = songMap.get(augFragment);
        if(song == null) {
            if(augFragment == augActivity.AUG_FRAGMENT_PLAYER) {
                String titleKey = augActivity.getPreferences(Context.MODE_PRIVATE).getString(Song.FIELD_TITLE_KEY, null);
                song = (titleKey == null)? songList.get(0) : getSongByTitleKey(titleKey);
            } else if(augFragment == augActivity.AUG_FRAGMENT_ANALYZER) {
                for(Song s: songList) {
                    if(augActivity.getPreferences(Context.MODE_PRIVATE).getFloat(Song.FIELD_BPM + s.getTitleKey(), 0) == 0) {
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

    public void dbClose() {
        database.close();
    }

    public void dbInsert(Song song) {
        ContentValues contentValues = new ContentValues();
    }


    //

    private class SongComparator implements Comparator<Song> {
        @Override
        public int compare(Song lhs, Song rhs) {
            return lhs.getTitle().compareTo(rhs.getTitle());
        }
    }
}

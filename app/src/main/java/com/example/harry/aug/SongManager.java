package com.example.harry.aug;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by harry on 8/27/15.
 */
public class SongManager {
    public static final String TABLE_NAME = "Song";
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    Song.FIELD_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Song.FIELD_DATA       + " TEXT NOT NULL, " +
                    Song.FIELD_TITLE_KEY  + " TEXT NOT NULL, " +
                    Song.FIELD_TITLE      + " TEXT NOT NULL, " +
                    Song.FIELD_ARTIST     + " TEXT NOT NULL, " +
                    Song.FIELD_DURATION   + " INTEGER NOT NULL, " +
                    Song.FIELD_BPM        + " REAL, " +
                    Song.FIELD_BEAT_SCORE + " REAL, " +
                    Song.FIELD_BEAT_COUNT + " INTEGER, " +
                    Song.FIELD_BEAT_TIME  + " BLOB)";
    public static final String UPGRADE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    private AUGActivity augActivity;
    private SQLiteDatabase database;
    private ArrayList<Song> songList;
    private Map<AUGFragment, Song> songMap;

    //

    public SongManager(AUGActivity augActivity) {
        this.augActivity = augActivity;
        this.database = AUGDBHelper.getDatabase(augActivity);
        makeSongList();
        this.songList = getSongListFromDB(new SongComparator(Song.FIELD_TITLE));
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

    public void saveSongOfPlayerFragment() {
        augActivity.getPreferences(Context.MODE_PRIVATE).edit().putString(Song.FIELD_TITLE_KEY, songMap.get(augActivity.AUG_FRAGMENT_PLAYER).getTitleKey()).apply();
    }

    private void makeSongList() {
        Cursor cursor = augActivity.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        while(cursor.moveToNext()) {
            if(cursor.getString(cursor.getColumnIndex(Song.FIELD_TITLE)).startsWith("2015")) continue; // TODO: remove constraint

            String title = cursor.getString(cursor.getColumnIndex(Song.FIELD_TITLE));
            String titleKey = cursor.getString(cursor.getColumnIndex(Song.FIELD_TITLE_KEY));
            if(dbQueryByTitleKey(titleKey) == null) {
                dbInsert(new Song(cursor, false));
            }
        }

        cursor.close();
    }

    private ArrayList<Song> getSongListFromDB(Comparator<Song> comparator) {
        ArrayList<Song> songList = dbQueryAll();
        Collections.sort(songList, comparator);

        return songList;
    }

    //

    private ContentValues songToContentValues(Song song) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Song.FIELD_DATA, song.getData());
        contentValues.put(Song.FIELD_TITLE_KEY, song.getTitleKey());
        contentValues.put(Song.FIELD_TITLE, song.getTitle());
        contentValues.put(Song.FIELD_ARTIST, song.getArtist());
        contentValues.put(Song.FIELD_DURATION, song.getDuration());
        contentValues.put(Song.FIELD_BPM, song.getBPM());
        contentValues.put(Song.FIELD_BEAT_SCORE, song.getBeatScore());
        contentValues.put(Song.FIELD_BEAT_COUNT, song.getBeatCount());

        long[] longBufferArray = song.getBeatTime();
        byte[] byteBufferArray = null;
        if(longBufferArray != null) {
            byteBufferArray = new byte[song.getBeatCount() * (Long.SIZE / 8)];

            ByteBuffer byteBuffer = ByteBuffer.wrap(byteBufferArray);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(longBufferArray);
        }
        contentValues.put(Song.FIELD_BEAT_TIME, byteBufferArray);

        return contentValues;
    }

    public void dbClose() {
        database.close();
    }

    public Song dbQueryByTitleKey(String titleKey) {
        Song song = null;
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + Song.FIELD_TITLE_KEY + "= ?";
        Cursor cursor = database.rawQuery(query, new String[] {titleKey});

        if(cursor.moveToFirst()) {
            song = new Song(cursor, true);
        }

        cursor.close();
        return song;
    }

    public ArrayList<Song> dbQueryAll() {
        ArrayList<Song> dbSongList = new ArrayList<>();

        Cursor cursor = database.query(TABLE_NAME, null, null, null, null, null, null, null);
        while(cursor.moveToNext()) {
            dbSongList.add(new Song(cursor, true));
        }

        cursor.close();
        return dbSongList;
    }

    public void dbInsert(Song song) {
        ContentValues contentValues = songToContentValues(song);
        try {
            long id = database.insertOrThrow(TABLE_NAME, null, contentValues);
            song.setId(id);
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean dbUpdate(Song song) {
        ContentValues contentValues = songToContentValues(song);
        String where = Song.FIELD_ID + "=" + song.getId();
        return database.update(TABLE_NAME, contentValues, where, null) > 0;
    }

    public boolean dbDelete(long id) {
        String where = Song.FIELD_ID + "=" + id;
        return database.delete(TABLE_NAME, where, null) > 0;
    }

    //

    private class SongComparator implements Comparator<Song> {
        private String sortBy;

        public SongComparator(String sortBy) {
            this.sortBy = sortBy;
        }

        @Override
        public int compare(Song lhs, Song rhs) {
            switch(sortBy) {
                case Song.FIELD_DATA:
                    return lhs.getData().compareTo(rhs.getData());
                case Song.FIELD_TITLE:
                    return lhs.getTitle().compareTo(rhs.getTitle());
                case Song.FIELD_ARTIST:
                    return lhs.getArtist().compareTo(rhs.getArtist());
                case Song.FIELD_DURATION:
                    return (lhs.getDuration() < rhs.getDuration()) ? -1 : +1;
                case Song.FIELD_BPM:
                    return (lhs.getBPM() < rhs.getBPM()) ? -1 : +1;
                default:
                    return 0;
            }
        }
    }
}

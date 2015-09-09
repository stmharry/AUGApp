package com.example.harry.aug;

import android.database.Cursor;
import android.provider.MediaStore;

/**
 * Created by harry on 15/9/9.
 */
public class Song {
    public static final String
            FIELD_DATA      = MediaStore.Audio.Media.DATA,
            FIELD_TITLE_KEY = MediaStore.Audio.Media.TITLE_KEY,
            FIELD_TITLE     = MediaStore.Audio.Media.TITLE,
            FIELD_ARTIST    = MediaStore.Audio.Media.ARTIST,
            FIELD_DURATION  = MediaStore.Audio.Media.DURATION;

    public static final String
            FIELD_ID        = "_id",
            FIELD_BPM       = "bpm",
            FIELD_BEAT_NUM  = "beat_num",
            FIELD_BEAT_TIME = "beat_time";

    private int     id;
    private String  data;
    private String  titleKey;
    private String  title;
    private String  artist;
    private long    duration;
    private float   bpm;
    private int     beatCount;
    private float[] beatTime;

    public Song(Cursor cursor) {
        this.data     = cursor.getString(cursor.getColumnIndex(FIELD_DATA));
        this.titleKey = cursor.getString(cursor.getColumnIndex(FIELD_TITLE_KEY));
        this.title    = cursor.getString(cursor.getColumnIndex(FIELD_TITLE));
        this.artist   = cursor.getString(cursor.getColumnIndex(FIELD_ARTIST));
        this.duration = cursor.getLong(cursor.getColumnIndex(FIELD_DURATION));
    }

    public String getData() {
        return data;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getDuration() {
        return duration;
    }
}

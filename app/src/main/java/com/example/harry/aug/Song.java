package com.example.harry.aug;

import android.database.Cursor;
import android.provider.MediaStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
            FIELD_ID         = "_id",
            FIELD_BPM        = "bpm",
            FIELD_BEAT_COUNT = "beat_count",
            FIELD_BEAT_TIME  = "beat_time";

    private long   id;
    private String data;
    private String titleKey;
    private String title;
    private String artist;
    private long   duration;
    private float  bpm;
    private int    beatCount;
    private long[] beatTime;

    public Song(Cursor cursor, boolean isDB) {
        this.data      = cursor.getString(cursor.getColumnIndex(FIELD_DATA));
        this.titleKey  = cursor.getString(cursor.getColumnIndex(FIELD_TITLE_KEY));
        this.title     = cursor.getString(cursor.getColumnIndex(FIELD_TITLE));
        this.artist    = cursor.getString(cursor.getColumnIndex(FIELD_ARTIST));
        this.duration  = cursor.getLong(cursor.getColumnIndex(FIELD_DURATION));

        if(isDB) {
            this.bpm = cursor.getFloat(cursor.getColumnIndex(FIELD_BPM));
            this.beatCount = cursor.getInt(cursor.getColumnIndex(FIELD_BEAT_COUNT));

            byte[] byteArray = cursor.getBlob(cursor.getColumnIndex(FIELD_BEAT_TIME));
            if(byteArray == null) {
                this.beatTime = null;
            } else {
                int longBufferSize = byteArray.length / (Long.SIZE / 8);
                this.beatTime = new long[longBufferSize];
                ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get(beatTime);
            }
        }
    }

    public long getId() {
        return id;
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

    public float getBPM() {
        return bpm;
    }

    public int getBeatCount() {
        return beatCount;
    }

    public long[] getBeatTime() {
        return beatTime;
    }

    public void setId(long id) {
        this.id = id;
    }
}

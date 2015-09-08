package com.example.harry.aug;

import android.database.Cursor;
import android.provider.MediaStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by harry on 9/8/15.
 */
public class Song {
    public static String
            FIELD_DATA,
            FIELD_TITLE_KEY,
            FIELD_TITLE,
            FIELD_ARTIST,
            FIELD_DURATION,
            FIELD_BPM,
            FIELD_BEAT_TIME;

    private static final String[] FIELD_MEDIASTORE = new String[] {
            FIELD_DATA      = MediaStore.Audio.Media.DATA,
            FIELD_TITLE_KEY = MediaStore.Audio.Media.TITLE_KEY,
            FIELD_TITLE     = MediaStore.Audio.Media.TITLE,
            FIELD_ARTIST    = MediaStore.Audio.Media.ARTIST,
            FIELD_DURATION  = MediaStore.Audio.Media.DURATION
    };

    private static final String[] FIELD_AUG = new String[] {
            FIELD_BPM       = "BPM",
            FIELD_BEAT_TIME = "BEAT_TIME"
    };

    private Map<String, Object> map;

    public Song(Cursor cursor) {
        map = new HashMap<>(FIELD_MEDIASTORE.length + FIELD_AUG.length);

        for(String key: FIELD_MEDIASTORE) {
            int column = cursor.getColumnIndex(key);

            switch(cursor.getType(column)) {
                case Cursor.FIELD_TYPE_INTEGER:
                    map.put(key, cursor.getLong(column));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    map.put(key, cursor.getDouble(column));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    map.put(key, cursor.getString(column));
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

    public void put(String key, Object value) {
        map.put(key, value);
    }
}
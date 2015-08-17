package com.example.harry.aug;

import android.database.Cursor;
import android.provider.MediaStore.Audio.Media;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by harry on 15/7/22.
 */
public class Song {
    private static final String[] FIELD = new String[]{
            Media.DATA,
            Media.TITLE_KEY,
            Media.TITLE,
            Media.ARTIST,
            Media.DURATION};

    private Map<String, Object> map;

    public Song(Cursor cursor) {
        map = new HashMap<String, Object>(FIELD.length);

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

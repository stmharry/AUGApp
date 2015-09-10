package com.example.harry.aug;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by harry on 9/8/15.
 */
public class AUGDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "AUG3.db";
    private static final int VERSION = 1;

    private static SQLiteDatabase database;

    public AUGDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public static SQLiteDatabase getDatabase(Context context) {
        if(database == null || !database.isOpen()) {
            database = new AUGDBHelper(context, DATABASE_NAME, null, VERSION).getWritableDatabase();
        }

        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(SongManager.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL(SongManager.UPGRADE_TABLE);
        onCreate(database);
    }
}
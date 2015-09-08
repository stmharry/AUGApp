package com.example.harry.aug;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by harry on 9/8/15.
 */
public class AUGDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "AUG.db";
    public static final int VERSION = 1;

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
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        onCreate(db);
    }
}
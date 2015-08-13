package com.example.harry.aug;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {
    private ArrayList<Song> songList;
    private ListView songView;

    private static final String MAIN_TAG = "Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songList = retrieveSongList();
        songView = (ListView) findViewById(R.id.song_list);
        songView.setAdapter(new SongAdapter(this, songList));
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, PlayerActivity.class);
                intent.putExtra("titleKey", (String) view.getTag());

                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public ArrayList<Song> retrieveSongList() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        ArrayList<Song> songList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            int titleKeyColumn = cursor.getColumnIndex(Media.TITLE_KEY);
            int titleColumn = cursor.getColumnIndex(Media.TITLE);
            int artistColumn = cursor.getColumnIndex(Media.ARTIST);
            int durationColumn = cursor.getColumnIndex(Media.DURATION);
            do {
                String titleKey = cursor.getString(titleKeyColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                long duration = cursor.getLong(durationColumn);

                if(duration < 10000) { //
                    songList.add(new Song(titleKey, title, artist, duration));
                }
            } while(cursor.moveToNext());
        }
        cursor.close();

        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        return songList;
    }
}

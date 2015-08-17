package com.example.harry.aug;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore.Audio.Media;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class PlayerActivity extends Activity {
    private static final String TAG = "PlayerActivity";

    private static final int UPDATE_INTERVAL = 50;

    private AUGManager augManager;
    private Handler handler;
    private TimeUpdater timeUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //

        String titleKey = getIntent().getStringExtra("TITLE_KEY");
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        int column = cursor.getColumnIndex(Media.TITLE_KEY);
        if(cursor.moveToFirst()) {
            do {
                if (cursor.getString(column).equals(titleKey)) {
                    break;
                }
            } while(cursor.moveToNext());
        }

        Song song = new Song(cursor);
        cursor.close();

        augManager = new AUGManager();
        String dataSource = (String) (song.get(Media.DATA));
        augManager.setDataSource(dataSource);
        augManager.prepare();

        //
        handler = new Handler(Looper.getMainLooper());
        timeUpdater = new TimeUpdater(augManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        augManager.start();
        handler.postDelayed(timeUpdater, UPDATE_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        augManager.pause();
        handler.removeCallbacks(timeUpdater);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    // TODO: onStop, onDestroy

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

    private class TimeUpdater implements Runnable {
        private AUGManager augManager;
        private TextView playerTimeView;

        public TimeUpdater(AUGManager augManager) {
            this.augManager = augManager;
            this.playerTimeView = (TextView) findViewById(R.id.player_time);
        }

        @Override
        public void run() {
            float time = (float) augManager.seek() / TimeUnit.SECONDS.toMicros(1);
            playerTimeView.setText(String.format("%.1f", time));
            handler.postDelayed(timeUpdater, UPDATE_INTERVAL);
        }
    }
}

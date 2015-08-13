package com.example.harry.aug;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.view.Menu;
import android.view.MenuItem;

public class PlayerActivity extends Activity {
    private static final String TAG = "PlayerActivity";

    private AUGManager augManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String titleKey = getIntent().getStringExtra("titleKey");
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        int titleKeyColumn = cursor.getColumnIndex(Media.TITLE_KEY);
        int titleColumn = cursor.getColumnIndex(Media.TITLE);
        int uriColumn = cursor.getColumnIndex(Media.DATA);

        if(cursor.moveToFirst()) {
            do {
                if(cursor.getString(titleKeyColumn).equals(titleKey)) {
                    break;
                }
            } while(cursor.moveToNext());
        }

        String title = cursor.getString(titleColumn);
        String uri = cursor.getString(uriColumn);
        cursor.close();

        augManager = new AUGManager();
        augManager.setDataSource(uri);
        augManager.prepare();
    }

    @Override
    protected void onResume() {
        super.onResume();
        augManager.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        augManager.pause();
    }

    // TODO: onStop, onDestroy

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
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
}

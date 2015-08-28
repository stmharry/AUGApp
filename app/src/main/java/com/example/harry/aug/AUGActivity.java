package com.example.harry.aug;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class AUGActivity extends AppCompatActivity {
    private static final String TAG = "AUGActivity";

    public static final int
            AUG_LAYOUT_MAJOR = 0,
            AUG_LAYOUT_MINOR = 1;

    public static BaseFragment
            AUG_FRAGMENT_MAJOR_CUR,
            AUG_FRAGMENT_ROOT,
            AUG_FRAGMENT_LIST,
            AUG_FRAGMENT_PLAYER,
            AUG_FRAGMENT_MINOR_CUR,
            AUG_FRAGMENT_ANALYZER;

    private static final BaseFragment[] AUG_FRAGMENT_MAJOR = new BaseFragment[] {
            AUG_FRAGMENT_ROOT = AUG_FRAGMENT_LIST = ListFragment.newInstance(),
            AUG_FRAGMENT_PLAYER = PlayerFragment.newInstance()};

    private static final BaseFragment[] AUG_FRAGMENT_MINOR = new BaseFragment[] {
            AUG_FRAGMENT_ANALYZER = AnalyzerFragment.newInstance()};

    private CharSequence appName;
    private FragmentManager fragmentManager;
    private SongManager songManager;

    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private String[] drawerListName;
    private ArrayAdapter<String> drawerAdapter;

    private ActionBar actionBar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private CharSequence actionBarTitle;

    //

    public SongManager getSongManager() {
        return songManager;
    }

    //

    private String getName(BaseFragment fragment) {
        return getString(fragment.getTitleResource());
    }

    private String[] getDrawerListName() {
        String[] drawerListName = new String[AUG_FRAGMENT_MAJOR.length];
        for(int i = 0; i < drawerListName.length; i++) {
            drawerListName[i] = getName(AUG_FRAGMENT_MAJOR[i]);
        }
        return drawerListName;
    }

    private void setLayout(int drawerContentResource, boolean isClosed, int drawerContentWeightClosedResource, int drawerContentWeightOpenResource) {
        findViewById(drawerContentResource).setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                getResources().getInteger(isClosed? drawerContentWeightClosedResource : drawerContentWeightOpenResource)));

    }

    public void replaceLayout(int layout, BaseFragment fragment) {
        BaseFragment AUG_FRAGMENT_CUR = null;
        int drawerContentResource = 0;

        switch(layout) {
            case AUG_LAYOUT_MAJOR:
                AUG_FRAGMENT_CUR = AUG_FRAGMENT_MAJOR_CUR;
                AUG_FRAGMENT_MAJOR_CUR = fragment;
                drawerContentResource = R.id.drawer_content_major;
                //
                drawerLayout.closeDrawer(drawerListView);
                actionBarTitle = getName(fragment);
                actionBar.setTitle(actionBarTitle);
                break;
            case AUG_LAYOUT_MINOR:
                AUG_FRAGMENT_CUR = AUG_FRAGMENT_MINOR_CUR;
                AUG_FRAGMENT_MINOR_CUR = fragment;
                drawerContentResource = R.id.drawer_content_minor;
                //
                boolean isClosed = (fragment == null);
                setLayout(R.id.drawer_content_major, isClosed, R.integer.drawer_content_major_weight_closed, R.integer.drawer_content_major_weight_open);
                setLayout(R.id.drawer_content_minor, isClosed, R.integer.drawer_content_minor_weight_closed, R.integer.drawer_content_minor_weight_open);
                if(isClosed) {
                    return;
                }
                break;
        }

        if(fragment != AUG_FRAGMENT_CUR) {
            fragment.setAugActivity(this);
            fragmentManager.beginTransaction().replace(drawerContentResource, fragment).commit();
        }
    }

    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aug);

        appName = getString(R.string.app_name);
        fragmentManager = getFragmentManager();
        songManager = new SongManager(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        drawerListView = (ListView) findViewById(R.id.drawer_list);
        drawerListName = getDrawerListName();
        drawerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, drawerListName);
        actionBar = getSupportActionBar();
        actionBarDrawerToggle = new AUGActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        actionBarTitle = actionBar.getTitle();

        //

        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        drawerListView.setAdapter(drawerAdapter);
        drawerListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        drawerListView.setOnItemClickListener(new AUGDrawerOnItemClickListener());
        actionBar.setElevation(0);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        //

        replaceLayout(AUG_LAYOUT_MAJOR, AUG_FRAGMENT_ROOT);
        replaceLayout(AUG_LAYOUT_MINOR, AUG_FRAGMENT_ANALYZER);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        actionBarDrawerToggle.onConfigurationChanged(configuration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if(actionBarDrawerToggle.onOptionsItemSelected(menuItem)) {
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if(AUG_FRAGMENT_MAJOR_CUR == AUG_FRAGMENT_ROOT) {
            finish();
        } else {
            AUG_FRAGMENT_MAJOR_CUR = AUG_FRAGMENT_ROOT;
            fragmentManager.beginTransaction().replace(R.id.drawer_content_major, AUG_FRAGMENT_ROOT).commit();
        }
    }

    //

    private class AUGActionBarDrawerToggle extends ActionBarDrawerToggle {
        public AUGActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View view) {
            actionBar.setTitle(appName);
        }

        @Override
        public void onDrawerClosed(View view) {
            actionBar.setTitle(actionBarTitle);
        }
    }

    private class AUGDrawerOnItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            replaceLayout(AUG_LAYOUT_MAJOR, AUG_FRAGMENT_MAJOR[position]);
        }
    }
}

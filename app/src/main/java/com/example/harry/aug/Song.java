package com.example.harry.aug;

import android.text.format.Time;

/**
 * Created by harry on 15/7/22.
 */
public class Song {
    private String titleKey;
    private String title;
    private String artist;
    private long duration;

    public Song(String titleKey, String title, String artist, long duration) {
        this.titleKey = titleKey;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
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
    public String getDuration() {
        Time time = new Time();
        time.set(duration);
        return time.format("%M:%S");
    }
}

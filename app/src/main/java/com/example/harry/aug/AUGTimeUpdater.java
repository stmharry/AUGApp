package com.example.harry.aug;

/**
 * Created by harry on 15/9/4.
 */
public abstract class AUGTimeUpdater implements Runnable {
    protected AUGActivity augActivity;
    protected AUGManager augManager;
    protected boolean loop;

    public void setAUGActivity(AUGActivity augActivity) {
        this.augActivity = augActivity;
    }

    public void setAUGManager(AUGManager augManager) {
        this.augManager = augManager;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }
}
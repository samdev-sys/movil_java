package com.icass.chatfirebase.managers;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

public class ResourceManager {
    private static volatile ResourceManager instance;
    private Application application;

    private ResourceManager() {

    }

    public static ResourceManager getInstance() {
        if(instance == null) {
            synchronized (ResourceManager.class) {
                if(instance == null) {
                    instance = new ResourceManager();
                }
            }
        }

        return instance;
    }

    public void initialize(@NonNull Application application) {
        this.application = application;
    }

    @NonNull
    public Context getContext() {
        return application.getApplicationContext();
    }
}
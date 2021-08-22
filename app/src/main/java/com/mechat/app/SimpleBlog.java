package com.mechat.app;


import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.google.firebase.database.FirebaseDatabase;

public class SimpleBlog extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

    }
}

package com.mnpos.distribution;

import android.app.Application;

import com.mnpos.distribution.data.Session;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Session.get().load(this);
    }
}

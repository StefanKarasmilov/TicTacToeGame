package com.androidavanzado.tictactoee.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}

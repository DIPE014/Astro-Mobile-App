package com.astro.app;

import android.app.Application;
import android.util.Log;

import com.astro.app.di.AppComponent;
import com.astro.app.di.AppModule;
import com.astro.app.di.DaggerAppComponent;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

/**
 * Application class for Astro Mobile App.
 * Initializes app-wide dependencies using Dagger 2.
 */
public class AstroApplication extends Application {

    private static final String TAG = "AstroApplication";

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        initializePython();
        initializeDagger();
    }

    /**
     * Initializes the Python runtime (Chaquopy).
     * This must be called before any Python code is used.
     */
    private void initializePython() {
        if (!Python.isStarted()) {
            Log.d(TAG, "Starting Python runtime...");
            Python.start(new AndroidPlatform(this));
            Log.d(TAG, "Python runtime started");
        }
    }

    /**
     * Initializes the Dagger dependency injection framework.
     * Creates the AppComponent with the application context.
     */
    private void initializeDagger() {
        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(getApplicationContext()))
                .build();
        appComponent.inject(this);
    }

    /**
     * Returns the application-level Dagger component.
     * Activities and other components can use this to obtain dependencies.
     *
     * @return the AppComponent for dependency injection
     */
    public AppComponent getAppComponent() {
        return appComponent;
    }
}

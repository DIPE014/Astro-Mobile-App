package com.astro.app;

import android.app.Application;

import com.astro.app.di.AppComponent;
import com.astro.app.di.AppModule;
import com.astro.app.di.DaggerAppComponent;

/**
 * Application class for Astro Mobile App.
 * Initializes app-wide dependencies using Dagger 2.
 */
public class AstroApplication extends Application {

    private AppComponent appComponent;

    /**
     * Performs application startup tasks and initializes the Dagger AppComponent for application-wide dependency injection.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        initializeDagger();
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
     * Access the application-level Dagger component.
     *
     * @return the AppComponent instance used for application-wide dependency injection
     */
    public AppComponent getAppComponent() {
        return appComponent;
    }
}
package com.astro.app.di;

import android.content.Context;
import android.content.res.AssetManager;

import com.astro.app.core.control.LocationController;
import com.astro.app.core.control.SensorController;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module that provides application-level dependencies.
 * All dependencies provided here are scoped to the application lifecycle.
 */
@Module
public class AppModule {

    private final Context applicationContext;

    /**
     * Creates an AppModule with the given application context.
     *
     * @param applicationContext the application context (not activity context)
     */
    public AppModule(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Provides the application context.
     * This is a singleton since the application context lives for the entire app lifecycle.
     *
     * @return the application context
     */
    @Provides
    @Singleton
    Context provideContext() {
        return applicationContext;
    }

    /**
     * Provides the LocationController for GPS location updates.
     * Singleton to ensure only one instance manages location services.
     *
     * @param context the application context
     * @return the LocationController instance
     */
    @Provides
    @Singleton
    LocationController provideLocationController(Context context) {
        return new LocationController(context);
    }

    /**
     * Provides the SensorController for device orientation sensors.
     * Singleton to ensure only one instance manages sensor services.
     *
     * @param context the application context
     * @return the SensorController instance
     */
    @Provides
    @Singleton
    SensorController provideSensorController(Context context) {
        return new SensorController(context);
    }

    /**
     * Provides the AssetManager for accessing app assets.
     * Used for loading star catalog data and other binary assets.
     *
     * @param context the application context
     * @return the AssetManager instance
     */
    @Provides
    @Singleton
    AssetManager provideAssetManager(Context context) {
        return context.getAssets();
    }
}

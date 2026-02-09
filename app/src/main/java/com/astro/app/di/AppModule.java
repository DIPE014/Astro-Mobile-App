package com.astro.app.di;

import android.content.Context;
import android.content.res.AssetManager;

import com.astro.app.core.control.AstronomerModel;
import com.astro.app.core.control.AstronomerModelImpl;
import com.astro.app.core.control.LocationController;
import com.astro.app.core.control.MagneticDeclinationCalculator;
import com.astro.app.core.control.RealMagneticDeclinationCalculator;
import com.astro.app.core.control.SensorController;
import com.astro.app.core.control.TimeTravelClock;
import com.astro.app.core.control.space.Universe;
import com.astro.app.data.parser.AssetDataSource;
import com.astro.app.data.parser.ProtobufParser;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.ConstellationRepositoryImpl;
import com.astro.app.data.repository.MessierRepository;
import com.astro.app.data.repository.MessierRepositoryImpl;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.data.repository.StarRepositoryImpl;

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

    /**
     * Provides the MagneticDeclinationCalculator for calculating the difference
     * between magnetic north and true north.
     *
     * @return the MagneticDeclinationCalculator instance
     */
    @Provides
    @Singleton
    MagneticDeclinationCalculator provideMagneticDeclinationCalculator() {
        return new RealMagneticDeclinationCalculator();
    }

    /**
     * Provides the TimeTravelClock for viewing the sky at different times.
     * Singleton to ensure consistent time state across the app.
     *
     * @return the TimeTravelClock instance
     */
    @Provides
    @Singleton
    TimeTravelClock provideTimeTravelClock() {
        return new TimeTravelClock();
    }

    /**
     * Provides the AstronomerModel for calculating celestial coordinates
     * from device sensor data.
     * Singleton to ensure consistent state across the app.
     *
     * @param magneticDeclinationCalculator the calculator for magnetic declination
     * @return the AstronomerModel instance
     */
    @Provides
    @Singleton
    AstronomerModel provideAstronomerModel(MagneticDeclinationCalculator magneticDeclinationCalculator) {
        return new AstronomerModelImpl(magneticDeclinationCalculator);
    }

    /**
     * Provides the StarRepository for accessing star data.
     * Uses StarRepositoryImpl which loads data from binary protobuf files.
     *
     * @param protobufParser the parser for reading binary star data
     * @return the StarRepository instance
     */
    @Provides
    @Singleton
    StarRepository provideStarRepository(ProtobufParser protobufParser,
                                         AssetDataSource assetDataSource) {
        return new StarRepositoryImpl(protobufParser, assetDataSource);
    }

    /**
     * Provides the ConstellationRepository for accessing constellation data.
     * Uses ConstellationRepositoryImpl which loads data from binary protobuf files.
     *
     * @param protobufParser the parser for reading binary constellation data
     * @return the ConstellationRepository instance
     */
    @Provides
    @Singleton
    ConstellationRepository provideConstellationRepository(ProtobufParser protobufParser) {
        return new ConstellationRepositoryImpl(protobufParser);
    }

    /**
     * Provides the Universe for calculating solar system body positions.
     * Singleton to ensure consistent state across the app.
     *
     * @return the Universe instance
     */
    @Provides
    @Singleton
    MessierRepository provideMessierRepository(ProtobufParser protobufParser) {
        return new MessierRepositoryImpl(protobufParser);
    }

    @Provides
    @Singleton
    Universe provideUniverse() {
        return new Universe();
    }
}

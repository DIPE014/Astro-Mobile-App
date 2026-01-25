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
import com.astro.app.data.parser.ProtobufParser;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.ConstellationRepositoryImpl;
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
     * Constructs an AppModule that retains the application Context for providing app-scoped dependencies.
     *
     * @param applicationContext the application Context to use (must be the application-level context, not an Activity context)
     */
    public AppModule(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Provide the application context.
     *
     * @return the application Context
     */
    @Provides
    @Singleton
    Context provideContext() {
        return applicationContext;
    }

    /**
     * Provides the application's LocationController.
     *
     * @return the LocationController configured with the application context
     */
    @Provides
    @Singleton
    LocationController provideLocationController(Context context) {
        return new LocationController(context);
    }

    /**
     * Provides the application's SensorController.
     *
     * @param context the application Context used to create the controller
     * @return the SensorController instance
     */
    @Provides
    @Singleton
    SensorController provideSensorController(Context context) {
        return new SensorController(context);
    }

    /**
     * Provides the application's AssetManager for accessing packaged assets.
     *
     * @param context the application Context used to obtain the AssetManager
     * @return the AssetManager for accessing the application's packaged assets
     */
    @Provides
    @Singleton
    AssetManager provideAssetManager(Context context) {
        return context.getAssets();
    }

    /**
     * Creates a MagneticDeclinationCalculator for computing the angular difference between magnetic north and true north.
     *
     * @return a MagneticDeclinationCalculator that computes magnetic declination
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
     * Creates an AstronomerModel that calculates celestial coordinates from device sensor data.
     *
     * @param magneticDeclinationCalculator calculator used to correct magnetic declination
     * @return the AstronomerModel instance
     */
    @Provides
    @Singleton
    AstronomerModel provideAstronomerModel(MagneticDeclinationCalculator magneticDeclinationCalculator) {
        return new AstronomerModelImpl(magneticDeclinationCalculator);
    }

    /**
         * Creates a StarRepository that provides access to star data.
         *
         * @param protobufParser parser used to read star data from protobuf binaries
         * @return the StarRepository instance
         */
    @Provides
    @Singleton
    StarRepository provideStarRepository(ProtobufParser protobufParser) {
        return new StarRepositoryImpl(protobufParser);
    }

    /**
     * Creates a ConstellationRepository for accessing constellation data.
     *
     * @param protobufParser parser for reading protobuf-encoded constellation data
     * @return a ConstellationRepository backed by protobuf data
     */
    @Provides
    @Singleton
    ConstellationRepository provideConstellationRepository(ProtobufParser protobufParser) {
        return new ConstellationRepositoryImpl(protobufParser);
    }

    /**
     * Create a Universe used to calculate solar system body positions.
     *
     * @return the Universe used to compute positions of solar system bodies
     */
    @Provides
    @Singleton
    Universe provideUniverse() {
        return new Universe();
    }
}
package com.astro.app.di;

import com.astro.app.AstroApplication;
import com.astro.app.ui.settings.SettingsActivity;
import com.astro.app.ui.skymap.SkyMapActivity;
import com.astro.app.ui.starinfo.StarInfoActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Main Dagger component for the Astro application.
 * This component is the root of the dependency graph.
 */
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    /**
     * Injects dependencies into the AstroApplication.
     *
     * @param application the application instance to inject into
     */
    void inject(AstroApplication application);

    /**
     * Injects dependencies into the SkyMapActivity.
     *
     * @param activity the activity instance to inject into
     */
    void inject(SkyMapActivity activity);

    /**
     * Injects dependencies into the StarInfoActivity.
     *
     * @param activity the activity instance to inject into
     */
    void inject(StarInfoActivity activity);

    /**
     * Injects dependencies into the SettingsActivity.
     *
     * @param activity the activity instance to inject into
     */
    void inject(SettingsActivity activity);

    /**
     * Builder interface for creating AppComponent instances.
     * Uses the application context to initialize the dependency graph.
     */
    @Component.Builder
    interface Builder {

        /**
         * Sets the AppModule for this component.
         *
         * @param appModule the module providing application-level dependencies
         * @return the builder instance for chaining
         */
        Builder appModule(AppModule appModule);

        /**
         * Builds the AppComponent instance.
         *
         * @return the constructed AppComponent
         */
        AppComponent build();
    }
}

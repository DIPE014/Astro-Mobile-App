package com.astro.app.data.parser;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Data source for reading binary asset files from the app's assets directory.
 * Provides a clean abstraction over Android's AssetManager for accessing
 * star catalog data and other binary resources.
 *
 * <p>This class is designed for use with Dagger dependency injection
 * and is scoped as a singleton to ensure efficient resource management.</p>
 */
@Singleton
public class AssetDataSource {

    private final AssetManager assetManager;

    /**
     * Constructs an AssetDataSource that uses the given AssetManager to access app assets.
     *
     * @param assetManager the Android AssetManager used to open asset files
     */
    @Inject
    public AssetDataSource(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Opens an InputStream for the specified asset file.
     *
     * <p>The caller is responsible for closing the returned InputStream
     * when finished. Consider using try-with-resources for automatic
     * resource management.</p>
     *
     * @param fileName the name of the asset file (e.g., "stars.binary")
     * @return an InputStream for reading the asset file
     * @throws IOException if the asset file cannot be found or opened
     */
    public InputStream openAsset(String fileName) throws IOException {
        return assetManager.open(fileName);
    }

    /**
     * Opens an InputStream for the stars catalog binary file.
     *
     * @return an InputStream for reading stars.binary
     * @throws IOException if the file cannot be found or opened
     */
    public InputStream openStarsCatalog() throws IOException {
        return openAsset("stars.binary");
    }

    /**
     * Open the constellations catalog asset.
     *
     * @return an InputStream for reading "constellations.binary"
     * @throws IOException if the asset cannot be found or opened
     */
    public InputStream openConstellationsCatalog() throws IOException {
        return openAsset("constellations.binary");
    }

    /**
     * Opens an InputStream for the Messier objects catalog binary file.
     *
     * @return an InputStream for reading messier.binary
     * @throws IOException if the file cannot be found or opened
     */
    public InputStream openMessierCatalog() throws IOException {
        return openAsset("messier.binary");
    }

    /**
     * Determines whether the named asset can be opened.
     *
     * @param fileName the asset file name to check
     * @return `true` if the asset can be opened, `false` otherwise
     */
    public boolean assetExists(String fileName) {
        try (InputStream ignored = assetManager.open(fileName)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
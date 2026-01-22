package com.astro.app.data.parser;

import android.util.Log;

import com.astro.app.data.proto.SourceProto.AstronomicalSourceProto;
import com.astro.app.data.proto.SourceProto.AstronomicalSourcesProto;
import com.astro.app.data.proto.SourceProto.PointElementProto;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Parser for reading binary star catalog files serialized as Protocol Buffers.
 *
 * <p>This class provides methods to parse the three main astronomical data files:</p>
 * <ul>
 *   <li>stars.binary - Contains individual star point elements</li>
 *   <li>constellations.binary - Contains constellation definitions with lines and labels</li>
 *   <li>messier.binary - Contains Messier deep-sky objects</li>
 * </ul>
 *
 * <p>The binary files contain serialized {@link AstronomicalSourcesProto} messages
 * which wrap collections of astronomical sources. This parser uses protobuf-lite
 * for efficient parsing on Android devices.</p>
 *
 * <p>This class is designed for use with Dagger dependency injection
 * and is scoped as a singleton for efficient memory usage.</p>
 */
@Singleton
public class ProtobufParser {

    private static final String TAG = "ProtobufParser";

    private final AssetDataSource assetDataSource;

    /**
     * Creates a ProtobufParser with the provided AssetDataSource.
     *
     * @param assetDataSource the data source for reading asset files
     */
    @Inject
    public ProtobufParser(AssetDataSource assetDataSource) {
        this.assetDataSource = assetDataSource;
    }

    /**
     * Parses the stars.binary file and extracts all star point elements.
     *
     * <p>The stars file contains {@link AstronomicalSourcesProto} where each
     * {@link AstronomicalSourceProto} typically contains a single
     * {@link PointElementProto} representing a star.</p>
     *
     * @return a list of PointElementProto objects representing stars,
     *         or an empty list if parsing fails
     */
    public List<PointElementProto> parseStars() {
        try (InputStream inputStream = assetDataSource.openStarsCatalog()) {
            return parseStarsFromStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse stars.binary", e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses star data from the provided InputStream.
     *
     * @param inputStream the input stream containing serialized AstronomicalSourcesProto
     * @return a list of PointElementProto objects representing stars
     * @throws IOException if reading or parsing fails
     */
    public List<PointElementProto> parseStarsFromStream(InputStream inputStream) throws IOException {
        AstronomicalSourcesProto sources = AstronomicalSourcesProto.parseFrom(inputStream);
        List<PointElementProto> stars = new ArrayList<>();

        for (AstronomicalSourceProto source : sources.getSourceList()) {
            stars.addAll(source.getPointList());
        }

        Log.d(TAG, "Parsed " + stars.size() + " stars from binary file");
        return stars;
    }

    /**
     * Parses the constellations.binary file and extracts all constellation sources.
     *
     * <p>Each {@link AstronomicalSourceProto} represents a complete constellation
     * with its lines, labels, and component stars.</p>
     *
     * @return a list of AstronomicalSourceProto objects representing constellations,
     *         or an empty list if parsing fails
     */
    public List<AstronomicalSourceProto> parseConstellations() {
        try (InputStream inputStream = assetDataSource.openConstellationsCatalog()) {
            return parseSourcesFromStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse constellations.binary", e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses the messier.binary file and extracts all Messier object sources.
     *
     * <p>Each {@link AstronomicalSourceProto} represents a Messier deep-sky object
     * (galaxies, nebulae, star clusters, etc.).</p>
     *
     * @return a list of AstronomicalSourceProto objects representing Messier objects,
     *         or an empty list if parsing fails
     */
    public List<AstronomicalSourceProto> parseMessierObjects() {
        try (InputStream inputStream = assetDataSource.openMessierCatalog()) {
            return parseSourcesFromStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse messier.binary", e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses astronomical sources from the provided InputStream.
     *
     * <p>This is a general-purpose method for parsing any binary file
     * containing serialized {@link AstronomicalSourcesProto} data.</p>
     *
     * @param inputStream the input stream containing serialized AstronomicalSourcesProto
     * @return a list of AstronomicalSourceProto objects
     * @throws IOException if reading or parsing fails
     */
    public List<AstronomicalSourceProto> parseSourcesFromStream(InputStream inputStream) throws IOException {
        AstronomicalSourcesProto sources = AstronomicalSourcesProto.parseFrom(inputStream);
        List<AstronomicalSourceProto> sourceList = new ArrayList<>(sources.getSourceList());
        Log.d(TAG, "Parsed " + sourceList.size() + " astronomical sources from binary file");
        return sourceList;
    }

    /**
     * Parses a generic binary asset file containing AstronomicalSourcesProto.
     *
     * @param fileName the name of the asset file to parse
     * @return a list of AstronomicalSourceProto objects,
     *         or an empty list if parsing fails
     */
    public List<AstronomicalSourceProto> parseAssetFile(String fileName) {
        try (InputStream inputStream = assetDataSource.openAsset(fileName)) {
            return parseSourcesFromStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse " + fileName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets the count of stars in the star catalog without fully parsing all data.
     * Useful for progress indicators and validation.
     *
     * @return the number of star sources in the catalog, or 0 if parsing fails
     */
    public int getStarCount() {
        try (InputStream inputStream = assetDataSource.openStarsCatalog()) {
            AstronomicalSourcesProto sources = AstronomicalSourcesProto.parseFrom(inputStream);
            int count = 0;
            for (AstronomicalSourceProto source : sources.getSourceList()) {
                count += source.getPointCount();
            }
            return count;
        } catch (IOException e) {
            Log.e(TAG, "Failed to count stars", e);
            return 0;
        }
    }

    /**
     * Gets the count of constellations in the catalog.
     *
     * @return the number of constellation sources, or 0 if parsing fails
     */
    public int getConstellationCount() {
        try (InputStream inputStream = assetDataSource.openConstellationsCatalog()) {
            AstronomicalSourcesProto sources = AstronomicalSourcesProto.parseFrom(inputStream);
            return sources.getSourceCount();
        } catch (IOException e) {
            Log.e(TAG, "Failed to count constellations", e);
            return 0;
        }
    }

    /**
     * Gets the count of Messier objects in the catalog.
     *
     * @return the number of Messier object sources, or 0 if parsing fails
     */
    public int getMessierCount() {
        try (InputStream inputStream = assetDataSource.openMessierCatalog()) {
            AstronomicalSourcesProto sources = AstronomicalSourcesProto.parseFrom(inputStream);
            return sources.getSourceCount();
        } catch (IOException e) {
            Log.e(TAG, "Failed to count Messier objects", e);
            return 0;
        }
    }
}

package com.astro.app.data.repository;

import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.MessierObjectData;
import com.astro.app.data.parser.ProtobufParser;
import com.astro.app.data.proto.SourceProto.AstronomicalSourceProto;
import com.astro.app.data.proto.SourceProto.GeocentricCoordinatesProto;
import com.astro.app.data.proto.SourceProto.LabelElementProto;
import com.astro.app.data.proto.SourceProto.PointElementProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link MessierRepository} that loads Messier/DSO data
 * from the messier.binary protobuf asset file.
 */
@Singleton
public class MessierRepositoryImpl implements MessierRepository {
    private static final String TAG = "MessierRepository";

    private final ProtobufParser protobufParser;
    private List<MessierObjectData> cachedObjects;

    @Inject
    public MessierRepositoryImpl(ProtobufParser protobufParser) {
        this.protobufParser = protobufParser;
    }

    @Override
    public List<MessierObjectData> getAllObjects() {
        ensureCacheLoaded();
        return cachedObjects;
    }

    @Override
    @Nullable
    public MessierObjectData getById(String id) {
        ensureCacheLoaded();
        for (MessierObjectData obj : cachedObjects) {
            if (obj.getId().equals(id)) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public List<MessierObjectData> search(String query) {
        ensureCacheLoaded();
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        List<MessierObjectData> results = new ArrayList<>();
        for (MessierObjectData obj : cachedObjects) {
            if (obj.getName().toLowerCase(Locale.ROOT).contains(lowerQuery) ||
                    obj.getId().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                results.add(obj);
            }
        }
        return results;
    }

    private synchronized void ensureCacheLoaded() {
        if (cachedObjects != null) {
            return;
        }

        Log.d(TAG, "Loading Messier objects from binary file...");

        List<AstronomicalSourceProto> protos = protobufParser.parseMessierObjects();
        List<MessierObjectData> objects = new ArrayList<>(protos.size());

        for (AstronomicalSourceProto proto : protos) {
            MessierObjectData obj = convertProto(proto);
            if (obj != null) {
                objects.add(obj);
            }
        }

        cachedObjects = Collections.unmodifiableList(objects);
        Log.d(TAG, "Loaded " + objects.size() + " Messier objects");
    }

    @Nullable
    private MessierObjectData convertProto(@NonNull AstronomicalSourceProto proto) {
        try {
            // Extract name from labels
            String name = null;
            if (proto.getLabelCount() > 0) {
                LabelElementProto label = proto.getLabel(0);
                if (label.hasStringsStrId()) {
                    name = formatName(label.getStringsStrId());
                }
            }
            if (name == null || name.isEmpty()) {
                return null;
            }

            // Extract location
            float ra = 0f, dec = 0f;
            if (proto.hasSearchLocation()) {
                GeocentricCoordinatesProto loc = proto.getSearchLocation();
                ra = loc.getRightAscension();
                dec = loc.getDeclination();
            } else if (proto.getPointCount() > 0) {
                PointElementProto point = proto.getPoint(0);
                if (point.hasLocation()) {
                    GeocentricCoordinatesProto loc = point.getLocation();
                    ra = loc.getRightAscension();
                    dec = loc.getDeclination();
                }
            }

            // Extract shape, color, size from first point element
            int shapeValue = 0;
            int color = Color.argb(200, 100, 180, 255); // Default blue
            int size = 3;

            if (proto.getPointCount() > 0) {
                PointElementProto point = proto.getPoint(0);
                shapeValue = point.getShape().getNumber();
                if (point.hasColor()) {
                    color = Color.argb(
                            (int) (point.getColor() >> 24 & 0xFF),
                            (int) (point.getColor() >> 16 & 0xFF),
                            (int) (point.getColor() >> 8 & 0xFF),
                            (int) (point.getColor() & 0xFF));
                }
                size = point.getSize();
                if (size <= 0) size = 3;
            }

            String id = "messier_" + name.toLowerCase(Locale.ROOT).replace(" ", "_");

            return new MessierObjectData(id, name, ra, dec, color, size, shapeValue);
        } catch (Exception e) {
            Log.w(TAG, "Failed to convert Messier proto: " + e.getMessage());
            return null;
        }
    }

    @NonNull
    private String formatName(@NonNull String snakeCaseId) {
        String[] parts = snakeCaseId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (builder.length() > 0) builder.append(" ");
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }
}

package com.astro.app.core.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * BACKEND - Person B
 *
 * Handles GPS location updates.
 * Provides user's latitude/longitude to AstronomerModel.
 */
public class LocationController {

    public interface LocationListener {
        void onLocationChanged(float latitude, float longitude);
    }

    private final FusedLocationProviderClient fusedLocationClient;
    private LocationListener listener;
    private LocationCallback locationCallback;

    public LocationController(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || listener == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    listener.onLocationChanged(
                            (float) location.getLatitude(),
                            (float) location.getLongitude()
                    );
                }
            }
        };
    }

    public void setListener(LocationListener listener) {
        this.listener = listener;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // Update every 10 seconds
                .setMinUpdateDistanceMeters(100)        // Or when moved 100m
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        // Get last known location immediately
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && listener != null) {
                listener.onLocationChanged(
                        (float) location.getLatitude(),
                        (float) location.getLongitude()
                );
            }
        });
    }

    public void stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}

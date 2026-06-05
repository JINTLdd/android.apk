package com.salah.app.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.salah.app.models.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Helper for getting the user's current location via Google Play Services + reverse-geocoding. */
public class LocationHelper {

    public interface Callback {
        void onResult(Location location);
        void onError(String message);
    }

    public static boolean hasPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public static void getCurrentLocation(Context ctx, Callback cb) {
        if (!hasPermission(ctx)) { cb.onError("NO_PERMISSION"); return; }
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(ctx);
        CancellationTokenSource cts = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
            .addOnSuccessListener(loc -> {
                if (loc == null) {
                    // Fall back to last known
                    client.getLastLocation()
                        .addOnSuccessListener(last -> {
                            if (last == null) { cb.onError("NO_LOCATION"); return; }
                            cb.onResult(toLocation(ctx, last.getLatitude(), last.getLongitude()));
                        })
                        .addOnFailureListener(e -> cb.onError(e.getMessage()));
                } else {
                    cb.onResult(toLocation(ctx, loc.getLatitude(), loc.getLongitude()));
                }
            })
            .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private static Location toLocation(Context ctx, double lat, double lng) {
        String city = "";
        try {
            Geocoder g = new Geocoder(ctx, new Locale("ar"));
            List<Address> list = g.getFromLocation(lat, lng, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                city = a.getLocality() != null ? a.getLocality()
                     : a.getSubAdminArea() != null ? a.getSubAdminArea()
                     : a.getAdminArea() != null ? a.getAdminArea() : "";
            }
        } catch (IOException ignored) {}
        return new Location(lat, lng, city, TimeZone.getDefault().getID());
    }
}

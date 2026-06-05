package com.salah.app.models;

/** Geographic coordinates + optional city name + timezone offset. */
public class Location {
    public final double latitude;
    public final double longitude;
    public final String cityName;
    public final String timezoneId; // e.g. "Asia/Riyadh"

    public Location(double latitude, double longitude, String cityName, String timezoneId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.cityName = cityName;
        this.timezoneId = timezoneId;
    }

    public static Location makkah() {
        return new Location(21.4225, 39.8262, "مكة المكرمة", "Asia/Riyadh");
    }
}

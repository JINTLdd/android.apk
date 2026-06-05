package com.salah.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.salah.app.models.Location;
import com.salah.app.models.UserSettings;

/** Single source of truth for persisted user settings. */
public class PreferencesManager {
    private static final String PREFS = "salah_prefs";

    // Keys
    private static final String K_LAT = "loc_lat";
    private static final String K_LNG = "loc_lng";
    private static final String K_CITY = "loc_city";
    private static final String K_TZ = "loc_tz";
    private static final String K_METHOD = "calc_method";
    private static final String K_MADHAB = "madhab";
    private static final String K_ADHAN_EN = "adhan_enabled";
    private static final String K_MORN_EN = "morning_athkar_enabled";
    private static final String K_EVE_EN = "evening_athkar_enabled";
    private static final String K_ADHAN_FILE = "adhan_file";
    private static final String K_MORN_HOUR = "morning_hour";
    private static final String K_MORN_MIN = "morning_min";
    private static final String K_EVE_HOUR = "evening_hour";
    private static final String K_EVE_MIN = "evening_min";
    private static final String K_DARK = "dark_mode";
    private static final String K_VIB = "vibrate";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static UserSettings load(Context ctx) {
        SharedPreferences p = prefs(ctx);
        UserSettings s = new UserSettings();
        s.calculationMethodId = p.getString(K_METHOD, s.calculationMethodId);
        s.madhabId = p.getString(K_MADHAB, s.madhabId);
        s.adhanEnabled = p.getBoolean(K_ADHAN_EN, s.adhanEnabled);
        s.morningAthkarEnabled = p.getBoolean(K_MORN_EN, s.morningAthkarEnabled);
        s.eveningAthkarEnabled = p.getBoolean(K_EVE_EN, s.eveningAthkarEnabled);
        s.selectedAdhanFile = p.getString(K_ADHAN_FILE, s.selectedAdhanFile);
        s.morningAthkarHour = p.getInt(K_MORN_HOUR, s.morningAthkarHour);
        s.morningAthkarMinute = p.getInt(K_MORN_MIN, s.morningAthkarMinute);
        s.eveningAthkarHour = p.getInt(K_EVE_HOUR, s.eveningAthkarHour);
        s.eveningAthkarMinute = p.getInt(K_EVE_MIN, s.eveningAthkarMinute);
        s.darkMode = p.getBoolean(K_DARK, s.darkMode);
        s.vibrateOnAlarm = p.getBoolean(K_VIB, s.vibrateOnAlarm);
        return s;
    }

    public static void save(Context ctx, UserSettings s) {
        prefs(ctx).edit()
            .putString(K_METHOD, s.calculationMethodId)
            .putString(K_MADHAB, s.madhabId)
            .putBoolean(K_ADHAN_EN, s.adhanEnabled)
            .putBoolean(K_MORN_EN, s.morningAthkarEnabled)
            .putBoolean(K_EVE_EN, s.eveningAthkarEnabled)
            .putString(K_ADHAN_FILE, s.selectedAdhanFile)
            .putInt(K_MORN_HOUR, s.morningAthkarHour)
            .putInt(K_MORN_MIN, s.morningAthkarMinute)
            .putInt(K_EVE_HOUR, s.eveningAthkarHour)
            .putInt(K_EVE_MIN, s.eveningAthkarMinute)
            .putBoolean(K_DARK, s.darkMode)
            .putBoolean(K_VIB, s.vibrateOnAlarm)
            .apply();
    }

    public static void saveLocation(Context ctx, Location loc) {
        prefs(ctx).edit()
            .putFloat(K_LAT, (float) loc.latitude)
            .putFloat(K_LNG, (float) loc.longitude)
            .putString(K_CITY, loc.cityName)
            .putString(K_TZ, loc.timezoneId)
            .apply();
    }

    public static Location loadLocation(Context ctx) {
        SharedPreferences p = prefs(ctx);
        if (!p.contains(K_LAT)) return null;
        return new Location(
            p.getFloat(K_LAT, 21.4225f),
            p.getFloat(K_LNG, 39.8262f),
            p.getString(K_CITY, "مكة المكرمة"),
            p.getString(K_TZ, "Asia/Riyadh")
        );
    }

    public static void applyTheme(Context ctx) {
        UserSettings s = load(ctx);
        AppCompatDelegate.setDefaultNightMode(
            s.darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}

package com.salah.app.models;

/** Lightweight POJO that mirrors the settings stored in SharedPreferences. */
public class UserSettings {
    public String calculationMethodId = "UmmAlQura"; // UmmAlQura | MuslimWorldLeague | Egyptian | Karachi
    public String madhabId = "Shafi";                // Shafi | Hanafi
    public boolean adhanEnabled = true;
    public boolean morningAthkarEnabled = true;
    public boolean eveningAthkarEnabled = true;
    public String selectedAdhanFile = "adhan_madinah"; // file name in res/raw (no extension)
    public int morningAthkarHour = 6;
    public int morningAthkarMinute = 30;
    public int eveningAthkarHour = 17;
    public int eveningAthkarMinute = 0;
    public boolean darkMode = false;
    public boolean vibrateOnAlarm = true;
}

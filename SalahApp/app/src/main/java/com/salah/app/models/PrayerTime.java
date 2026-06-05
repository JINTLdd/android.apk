package com.salah.app.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** A single prayer time entry. */
public class PrayerTime {
    public enum Prayer {
        FAJR("fajr", "الفجر"),
        SUNRISE("sunrise", "الشروق"),
        DHUHR("dhuhr", "الظهر"),
        ASR("asr", "العصر"),
        MAGHRIB("maghrib", "المغرب"),
        ISHA("isha", "العشاء");

        public final String id;
        public final String arabicName;
        Prayer(String id, String arabicName) {
            this.id = id;
            this.arabicName = arabicName;
        }
    }

    public final Prayer prayer;
    public final Date time;

    public PrayerTime(Prayer prayer, Date time) {
        this.prayer = prayer;
        this.time = time;
    }

    public String getArabicName() { return prayer.arabicName; }

    public String formatTime12h() {
        SimpleDateFormat fmt = new SimpleDateFormat("hh:mm a", new Locale("ar"));
        fmt.setTimeZone(TimeZone.getDefault());
        return fmt.format(time);
    }

    public long epochMs() { return time.getTime(); }

    public boolean isPast() { return time.getTime() < System.currentTimeMillis(); }
}

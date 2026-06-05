package com.salah.app.utils;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Calculates the 5 (+sunrise) daily prayer times using the Adhan library by Batoulapps.
 * Supports Umm al-Qura, Muslim World League, Egyptian, Karachi methods + Shafi/Hanafi madhab.
 */
public class PrayerTimesCalculator {

    public static List<PrayerTime> getTimesForDate(Location loc, UserSettings settings, Date date) {
        Coordinates coords = new Coordinates(loc.latitude, loc.longitude);
        CalculationParameters params = resolveMethod(settings.calculationMethodId).getParameters();
        params.madhab = resolveMadhab(settings.madhabId);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(loc.timezoneId));
        cal.setTime(date);
        DateComponents dc = new DateComponents(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        );

        PrayerTimes pt = new PrayerTimes(coords, dc, params);

        List<PrayerTime> out = new ArrayList<>(6);
        out.add(new PrayerTime(PrayerTime.Prayer.FAJR, pt.fajr));
        out.add(new PrayerTime(PrayerTime.Prayer.SUNRISE, pt.sunrise));
        out.add(new PrayerTime(PrayerTime.Prayer.DHUHR, pt.dhuhr));
        out.add(new PrayerTime(PrayerTime.Prayer.ASR, pt.asr));
        out.add(new PrayerTime(PrayerTime.Prayer.MAGHRIB, pt.maghrib));
        out.add(new PrayerTime(PrayerTime.Prayer.ISHA, pt.isha));
        return out;
    }

    public static List<PrayerTime> getTodayTimes(Location loc, UserSettings settings) {
        return getTimesForDate(loc, settings, new Date());
    }

    public static List<PrayerTime> getTomorrowTimes(Location loc, UserSettings settings) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        return getTimesForDate(loc, settings, c.getTime());
    }

    private static CalculationMethod resolveMethod(String id) {
        switch (id) {
            case "MuslimWorldLeague": return CalculationMethod.MUSLIM_WORLD_LEAGUE;
            case "Egyptian":          return CalculationMethod.EGYPTIAN;
            case "Karachi":           return CalculationMethod.KARACHI;
            case "NorthAmerica":      return CalculationMethod.NORTH_AMERICA;
            case "Dubai":             return CalculationMethod.DUBAI;
            case "Qatar":             return CalculationMethod.QATAR;
            case "Kuwait":            return CalculationMethod.KUWAIT;
            case "MoonsightingCommittee": return CalculationMethod.MOON_SIGHTING_COMMITTEE;
            case "UmmAlQura":
            default:                  return CalculationMethod.UMM_AL_QURA;
        }
    }

    private static Madhab resolveMadhab(String id) {
        return "Hanafi".equalsIgnoreCase(id) ? Madhab.HANAFI : Madhab.SHAFI;
    }

    /** Returns the next future prayer (or tomorrow's Fajr if all today's prayers have passed). */
    public static PrayerTime nextPrayer(Location loc, UserSettings settings) {
        long now = System.currentTimeMillis();
        for (PrayerTime p : getTodayTimes(loc, settings)) {
            if (p.prayer == PrayerTime.Prayer.SUNRISE) continue; // not an alarm prayer
            if (p.time.getTime() > now) return p;
        }
        for (PrayerTime p : getTomorrowTimes(loc, settings)) {
            if (p.prayer == PrayerTime.Prayer.FAJR) return p;
        }
        return null;
    }
}

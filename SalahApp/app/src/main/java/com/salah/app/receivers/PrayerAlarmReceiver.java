package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.salah.app.services.AdhanService;
import com.salah.app.services.PrayerForegroundService;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PrayerTimesCalculator;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;

/**
 * Fires when a scheduled prayer alarm goes off. Starts the AdhanService
 * (foreground media-playback) which plays the adhan + shows the full-screen
 * notification — then re-schedules the NEXT prayer.
 */
public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "PrayerAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerId = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_ID);
        String prayerAr = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR);
        Log.i(TAG, "PrayerAlarm fired: " + prayerId);

        // 1) Start the foreground Adhan service so audio + notification work
        //    even if app is fully killed.
        Intent svc = new Intent(context, AdhanService.class);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER_ID, prayerId);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR, prayerAr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }

        // 2) Schedule the NEXT prayer alarm immediately so chaining never breaks.
        try {
            UserSettings settings = PreferencesManager.load(context);
            Location loc = PreferencesManager.loadLocation(context);
            if (loc != null && settings.adhanEnabled) {
                PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, settings);
                if (next != null) AlarmScheduler.schedulePrayer(context, next);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to schedule next prayer", t);
        }
    }
}

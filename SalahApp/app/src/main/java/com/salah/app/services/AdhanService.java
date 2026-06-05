package com.salah.app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.WakeLockManager;
import com.salah.app.models.UserSettings;

/**
 * Foreground service that plays the adhan audio + shows the high-priority
 * full-screen notification. Designed to run reliably even when the app process
 * has been killed (started by the AlarmManager via PrayerAlarmReceiver).
 */
public class AdhanService extends Service {
    private static final String TAG = "AdhanService";
    public static final String ACTION_STOP = "com.salah.app.STOP_ADHAN";

    private MediaPlayer player;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlaybackAndSelf();
            return START_NOT_STICKY;
        }

        WakeLockManager.acquire(this, 10 * 60 * 1000L); // up to 10 min

        String prayerAr = intent != null
            ? intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR) : null;
        if (prayerAr == null) prayerAr = "الصلاة";

        // Build stop action PendingIntent
        Intent stopI = new Intent(this, AdhanService.class).setAction(ACTION_STOP);
        PendingIntent stopPI = PendingIntent.getService(
            this, 0, stopI,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification notif = NotificationHelper.buildPrayerNotification(this, prayerAr, stopPI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationHelper.NID_PRAYER, notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NotificationHelper.NID_PRAYER, notif);
        }

        startPlayback();
        return START_NOT_STICKY;
    }

    private void startPlayback() {
        try {
            UserSettings s = PreferencesManager.load(this);
            int resId = getResources().getIdentifier(
                s.selectedAdhanFile, "raw", getPackageName());
            if (resId == 0) resId = R.raw.adhan_madinah;
            player = MediaPlayer.create(this, resId);
            if (player == null) {
                Log.e(TAG, "MediaPlayer.create returned null");
                stopPlaybackAndSelf();
                return;
            }
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            player.setOnCompletionListener(mp -> {
                // After the adhan completes, launch the Post-Adhan dua popup over lock screen.
                try {
                    Intent post = new Intent(this, com.salah.app.activities.PostAdhanActivity.class);
                    post.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(post);
                } catch (Throwable ignored) {}
                stopPlaybackAndSelf();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error " + what + "/" + extra);
                stopPlaybackAndSelf();
                return true;
            });
            player.start();
        } catch (Throwable t) {
            Log.e(TAG, "Adhan playback failed", t);
            stopPlaybackAndSelf();
        }
    }

    private void stopPlaybackAndSelf() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Throwable ignored) {}
        try { stopForeground(STOP_FOREGROUND_REMOVE); } catch (Throwable ignored) {}
        try {
            NotificationManagerCompat.from(this).cancel(NotificationHelper.NID_PRAYER);
        } catch (SecurityException ignored) {}
        WakeLockManager.release();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Throwable ignored) {}
        WakeLockManager.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

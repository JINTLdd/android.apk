package com.salah.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;

/**
 * Builds notifications + manages NotificationChannels.
 */
public class NotificationHelper {

    public static final String CH_PRAYER = "salah_prayer_channel";
    public static final String CH_ATHKAR = "salah_athkar_channel";
    public static final String CH_FOREGROUND = "salah_foreground_channel";

    public static final int NID_PRAYER = 101;
    public static final int NID_ATHKAR = 102;
    public static final int NID_FOREGROUND = 999;

    public static void createAllChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Prayer channel (high importance, custom adhan sound, vibration, bypass DND)
        if (nm.getNotificationChannel(CH_PRAYER) == null) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            Uri sound = Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.adhan_madinah);
            NotificationChannel ch = new NotificationChannel(
                CH_PRAYER,
                ctx.getString(R.string.channel_prayer_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription(ctx.getString(R.string.channel_prayer_desc));
            ch.enableLights(true);
            ch.enableVibration(true);
            ch.setBypassDnd(true);
            ch.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            ch.setSound(sound, attrs);
            nm.createNotificationChannel(ch);
        }

        // Athkar channel
        if (nm.getNotificationChannel(CH_ATHKAR) == null) {
            NotificationChannel ch = new NotificationChannel(
                CH_ATHKAR,
                ctx.getString(R.string.channel_athkar_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription(ctx.getString(R.string.channel_athkar_desc));
            ch.enableVibration(true);
            ch.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }

        // Silent foreground-service channel
        if (nm.getNotificationChannel(CH_FOREGROUND) == null) {
            NotificationChannel ch = new NotificationChannel(
                CH_FOREGROUND,
                ctx.getString(R.string.channel_foreground_name),
                NotificationManager.IMPORTANCE_LOW
            );
            ch.setShowBadge(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
    }

    public static android.app.Notification buildPrayerNotification(
        Context ctx, String prayerArName, PendingIntent stopPI
    ) {
        Intent openApp = new Intent(ctx, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(
            ctx, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_PRAYER)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle(ctx.getString(R.string.notif_prayer_title))
            .setContentText(ctx.getString(R.string.notif_prayer_text_fmt, prayerArName))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(
                ctx.getString(R.string.notif_prayer_text_fmt, prayerArName) +
                "\n" + ctx.getString(R.string.notif_prayer_subtext)
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .setFullScreenIntent(openPI, true)
            .setOngoing(true)
            .setAutoCancel(false);

        if (stopPI != null) {
            b.addAction(R.drawable.ic_stop, ctx.getString(R.string.notif_action_stop), stopPI);
        }
        return b.build();
    }

    public static android.app.Notification buildAthkarNotification(Context ctx, String type) {
        boolean morning = "morning".equals(type);
        String title = ctx.getString(morning ? R.string.notif_morning_athkar_title
                                              : R.string.notif_evening_athkar_title);
        String text = ctx.getString(morning ? R.string.notif_morning_athkar_text
                                             : R.string.notif_evening_athkar_text);

        Intent openApp = new Intent(ctx, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openApp.putExtra("open_athkar", type);
        PendingIntent openPI = PendingIntent.getActivity(
            ctx, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(ctx, CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .setAutoCancel(true)
            .build();
    }

    public static android.app.Notification buildForegroundNotification(Context ctx, String prayerName) {
        return new NotificationCompat.Builder(ctx, CH_FOREGROUND)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle(ctx.getString(R.string.notif_fg_title))
            .setContentText(prayerName == null ? "" : prayerName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }
}

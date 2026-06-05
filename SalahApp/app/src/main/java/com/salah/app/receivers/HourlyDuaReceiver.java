package com.salah.app.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.AdhkarRepository;
import com.salah.app.utils.HourlyDuaScheduler;
import com.salah.app.utils.NotificationHelper;

import org.json.JSONArray;

/**
 * Fires every 2 hours — picks the next dua from the 20-item rotating list and shows it.
 */
public class HourlyDuaReceiver extends BroadcastReceiver {
    private static final String PREF_KEY = "hourly_dua_idx";
    private static final String NOTIF_TAG = "hourly_dua";
    private static final int NOTIF_ID = 401;

    @Override
    public void onReceive(Context context, Intent intent) {
        JSONArray arr = AdhkarRepository.root(context).optJSONArray("hourlyDuas");
        if (arr == null || arr.length() == 0) {
            HourlyDuaScheduler.scheduleNext(context);
            return;
        }
        android.content.SharedPreferences p = context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE);
        int idx = p.getInt(PREF_KEY, 0) % arr.length();
        String dua = arr.optString(idx, "");
        p.edit().putInt(PREF_KEY, (idx + 1) % arr.length()).apply();

        Intent open = new Intent(context, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(context, 401, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, NotificationHelper.CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle("🤲 دعاء من القرآن")
            .setContentText(dua)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(dua))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .setAutoCancel(true);

        try { NotificationManagerCompat.from(context).notify(NOTIF_TAG, NOTIF_ID, b.build()); }
        catch (SecurityException ignored) {}

        // Re-schedule the next 2-hour tick.
        HourlyDuaScheduler.scheduleNext(context);
    }
}

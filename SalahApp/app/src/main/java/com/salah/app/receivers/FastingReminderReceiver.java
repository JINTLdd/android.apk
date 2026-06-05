package com.salah.app.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.FastingReminderScheduler;
import com.salah.app.utils.NotificationHelper;

/**
 * Fires Sunday night (for Monday fasting) and Wednesday night (for Thursday fasting).
 * Shows a notification with the virtue of the upcoming fast + re-schedules for next week.
 */
public class FastingReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "FastingReminderReceiver";
    public static final String EXTRA_DAY = "fast_day"; // "monday" | "thursday"

    @Override
    public void onReceive(Context context, Intent intent) {
        String day = intent.getStringExtra(EXTRA_DAY);
        if (day == null) day = "monday";
        Log.i(TAG, "FastingReminder fired: " + day);

        boolean isMonday = "monday".equals(day);
        String title = isMonday
            ? "تذكير بصيام يوم الإثنين 🌙"
            : "تذكير بصيام يوم الخميس 🌙";

        String body = isMonday
            ? "قال ❟: «تُعرَضُ الأعمالُ يومَ الإثنين والخميس، فأحبُّ أن يُعرَضَ عملي وأنا صائم»، وسُئل ❟ عن صوم يوم الإثنين فقال: «ذلك يومٌ وُلدتُ فيهِ، ويومٌ بُعثتُ فيهِ أو أُنزِلَ عليَّ فيه». فلا تفوت الأجر غداً — بيت النيّة لصيام الإثنين."
            : "قال ❟: «تُعرَضُ الأعمالُ يومَ الإثنين والخميس، فأحبُّ أن يُعرَضَ عملي وأنا صائم» (رواه الترمذي). وفي الحديث: «تُفتحُ أبوابُ الجنةِ يومَ الإثنينِ ويومَ الخميسِ، فيُغفَرُ لكلِّ عبدٍ لا يُشرِكُ باللهِ شيئًا». فلا تفوت الأجر غداً — بيت النيّة لصيام الخميس.";

        Intent open = new Intent(context, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(context, isMonday ? 301 : 302, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle(title)
            .setContentText(body.substring(0, Math.min(80, body.length())) + "…")
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(isMonday ? 301 : 302, builder.build());
        } catch (SecurityException ignored) {}

        // Re-schedule next week.
        FastingReminderScheduler.scheduleAll(context);
    }
}

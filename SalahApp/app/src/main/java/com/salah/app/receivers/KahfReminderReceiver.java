package com.salah.app.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.KahfReminderScheduler;
import com.salah.app.utils.NotificationHelper;

/**
 * Fires every Friday morning — reminder to read/listen to Surat Al-Kahf.
 * Notification has 2 action buttons: تلاوة (open online reading) + استماع (open online recitation).
 */
public class KahfReminderReceiver extends BroadcastReceiver {
    private static final int NOTIF_ID = 501;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent open = new Intent(context, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(context, 501, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: read on quran.com
        Intent read = new Intent(Intent.ACTION_VIEW, Uri.parse("https://quran.com/ar/18"));
        read.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent readPI = PendingIntent.getActivity(context, 502, read,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: listen on Mishary Alafasy recitation
        Intent listen = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://server8.mp3quran.net/afs/018.mp3"));
        listen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent listenPI = PendingIntent.getActivity(context, 503, listen,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, NotificationHelper.CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle("📖 لا تنس سورة الكهف")
            .setContentText("من قرأها يوم الجمعة أضاء له من النور ما بين الجمعتين")
            .setStyle(new NotificationCompat.BigTextStyle().bigText(
                "من قرأ سورة الكهف يوم الجمعة أضاء له من النور ما بينه وبين الجمعتين (رواه الحاكم)."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .addAction(R.drawable.ic_athkar, "تلاوة", readPI)
            .addAction(R.drawable.ic_mosque, "استماع", listenPI)
            .setAutoCancel(true);

        try { NotificationManagerCompat.from(context).notify(NOTIF_ID, b.build()); }
        catch (SecurityException ignored) {}

        // Re-schedule for next Friday.
        KahfReminderScheduler.schedule(context);
    }
}

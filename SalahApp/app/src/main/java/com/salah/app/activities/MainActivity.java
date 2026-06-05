package com.salah.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.salah.app.R;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.AdhkarReminderScheduler;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.LocationHelper;
import com.salah.app.utils.PermissionHelper;
import com.salah.app.utils.PrayerTimesCalculator;
import com.salah.app.utils.PreferencesManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Home / hub screen — shows next prayer countdown + 6 large cards for the app sections.
 */
public class MainActivity extends AppCompatActivity {

    private TextView txtCity, txtDate, txtCountdown, txtNextPrayer;
    private android.os.Handler handler;
    private Runnable tickRunnable;
    private Location loc;
    private UserSettings settings;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        setContentView(R.layout.activity_main);

        txtCity = findViewById(R.id.txt_city);
        txtDate = findViewById(R.id.txt_date);
        txtCountdown = findViewById(R.id.txt_countdown);
        txtNextPrayer = findViewById(R.id.txt_next_prayer);

        bindCard(R.id.card_morning, R.string.tab_morning, R.drawable.ic_fajr, () -> openSession("morning"));
        bindCard(R.id.card_evening, R.string.tab_evening, R.drawable.ic_maghrib, () -> openSession("evening"));
        bindCard(R.id.card_after_salah, R.string.tab_after_salah, R.drawable.ic_mosque, () -> openSession("afterSalah"));
        bindCard(R.id.card_wakeup, R.string.tab_wakeup, R.drawable.ic_sunrise, () -> openSession("wakeup"));
        bindCard(R.id.card_duas, R.string.tab_duas, R.drawable.ic_athkar, () -> openSession("duas"));
        bindCard(R.id.card_iftitah, R.string.tab_iftitah, R.drawable.ic_dhuhr, () -> openSession("iftitah"));
        bindCard(R.id.card_tasbih, R.string.tab_tasbih, R.drawable.ic_athkar, () -> startActivity(new Intent(this, TasbihActivity.class)));
        bindCard(R.id.card_settings, R.string.tab_settings, R.drawable.ic_asr, () -> startActivity(new Intent(this, SettingsActivity.class)));

        // Runtime permissions on first launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PermissionHelper.needsNotifications(this)) {
            PermissionHelper.requestNotifications(this);
        }
        if (PermissionHelper.needsLocation(this)) {
            PermissionHelper.requestLocation(this);
        } else {
            Location cached = PreferencesManager.loadLocation(this);
            if (cached == null) fetchLocation();
            else { loc = cached; refresh(); }
        }

        // Schedule daily "we miss you" adhkar reminders
        AdhkarReminderScheduler.scheduleAll(this);
        com.salah.app.utils.SleepAdhkarScheduler.schedule(this);
    }

    private void bindCard(int id, int titleRes, int iconRes, Runnable action) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnClickListener(x -> action.run());
        TextView t = v.findViewById(R.id.title);
        if (t != null) t.setText(titleRes);
        android.widget.ImageView ic = v.findViewById(R.id.icon);
        if (ic != null) ic.setImageResource(iconRes);
    }

    private void openSession(String category) {
        Intent i = new Intent(this, AdhkarSessionActivity.class);
        i.putExtra(AdhkarSessionActivity.EXTRA_CATEGORY, category);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] perms, @NonNull int[] gr) {
        super.onRequestPermissionsResult(rc, perms, gr);
        if (rc == PermissionHelper.REQ_LOCATION) {
            if (gr.length > 0 && gr[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                loc = Location.makkah();
                refresh();
            }
        }
    }

    private void fetchLocation() {
        LocationHelper.getCurrentLocation(this, new LocationHelper.Callback() {
            @Override public void onResult(Location location) {
                PreferencesManager.saveLocation(MainActivity.this, location);
                loc = location;
                refresh();
            }
            @Override public void onError(String message) {
                Location cached = PreferencesManager.loadLocation(MainActivity.this);
                loc = cached != null ? cached : Location.makkah();
                refresh();
            }
        });
    }

    private void refresh() {
        settings = PreferencesManager.load(this);
        txtCity.setText(loc.cityName == null || loc.cityName.isEmpty()
            ? getString(R.string.city_unknown) : loc.cityName);
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("EEEE ، d MMMM yyyy", new Locale("ar"));
        txtDate.setText(df.format(Calendar.getInstance().getTime()));

        AlarmScheduler.rescheduleAll(this);
        startCountdown();
    }

    private void startCountdown() {
        if (handler == null) handler = new android.os.Handler(getMainLooper());
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        tickRunnable = new Runnable() {
            @Override public void run() {
                PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, settings);
                if (next == null) return;
                long diff = next.epochMs() - System.currentTimeMillis();
                if (diff < 0) { refresh(); return; }
                long h = TimeUnit.MILLISECONDS.toHours(diff);
                long m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                long sec = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                txtNextPrayer.setText(getString(R.string.next_prayer_fmt, next.getArabicName() + " · " + next.formatTime12h()));
                txtCountdown.setText(String.format(new Locale("ar"), "%02d:%02d:%02d", h, m, sec));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(tickRunnable);
    }

    @Override
    protected void onDestroy() {
        if (handler != null && tickRunnable != null) handler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }
}

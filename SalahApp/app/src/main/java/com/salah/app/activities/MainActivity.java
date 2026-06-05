package com.salah.app.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.adapters.PrayerTimesAdapter;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.LocationHelper;
import com.salah.app.utils.PermissionHelper;
import com.salah.app.utils.PrayerTimesCalculator;
import com.salah.app.utils.PreferencesManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView txtCity, txtDate, txtCountdown, txtNextPrayer;
    private RecyclerView listPrayers;
    private PrayerTimesAdapter adapter;
    private android.os.Handler handler;
    private Runnable tickRunnable;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        setContentView(R.layout.activity_main);

        txtCity = findViewById(R.id.txt_city);
        txtDate = findViewById(R.id.txt_date);
        txtCountdown = findViewById(R.id.txt_countdown);
        txtNextPrayer = findViewById(R.id.txt_next_prayer);
        listPrayers = findViewById(R.id.list_prayers);

        adapter = new PrayerTimesAdapter();
        listPrayers.setLayoutManager(new LinearLayoutManager(this));
        listPrayers.setAdapter(adapter);

        findViewById(R.id.btn_settings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btn_refresh).setOnClickListener(v -> fetchLocationAndRefresh());

        // Ask for runtime permissions on first launch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && PermissionHelper.needsNotifications(this)) {
            PermissionHelper.requestNotifications(this);
        }
        if (PermissionHelper.needsLocation(this)) {
            PermissionHelper.requestLocation(this);
        } else {
            // Already have permission — fetch fresh location or use cached.
            Location cached = PreferencesManager.loadLocation(this);
            if (cached == null) fetchLocationAndRefresh();
            else refresh(cached);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndRefresh();
            } else {
                Toast.makeText(this, R.string.toast_need_location, Toast.LENGTH_LONG).show();
                // Show default Makkah times as a fallback so the UI isn't empty.
                refresh(Location.makkah());
            }
        }
    }

    private void fetchLocationAndRefresh() {
        if (PermissionHelper.needsLocation(this)) {
            PermissionHelper.requestLocation(this);
            return;
        }
        LocationHelper.getCurrentLocation(this, new LocationHelper.Callback() {
            @Override public void onResult(Location location) {
                PreferencesManager.saveLocation(MainActivity.this, location);
                refresh(location);
            }
            @Override public void onError(String message) {
                Toast.makeText(MainActivity.this,
                    getString(R.string.toast_location_error, message == null ? "" : message),
                    Toast.LENGTH_LONG).show();
                Location cached = PreferencesManager.loadLocation(MainActivity.this);
                refresh(cached != null ? cached : Location.makkah());
            }
        });
    }

    private void refresh(Location loc) {
        UserSettings settings = PreferencesManager.load(this);
        List<PrayerTime> times = PrayerTimesCalculator.getTodayTimes(loc, settings);
        adapter.submit(times);

        txtCity.setText(loc.cityName == null || loc.cityName.isEmpty()
            ? getString(R.string.city_unknown) : loc.cityName);
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("EEEE ، d MMMM yyyy", new Locale("ar"));
        txtDate.setText(df.format(Calendar.getInstance().getTime()));

        // Re-arm the chain of prayer alarms (idempotent).
        AlarmScheduler.rescheduleAll(this);

        startCountdown(loc, settings);
    }

    private void startCountdown(Location loc, UserSettings settings) {
        if (handler == null) handler = new android.os.Handler(getMainLooper());
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        tickRunnable = new Runnable() {
            @Override public void run() {
                PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, settings);
                if (next == null) {
                    txtNextPrayer.setText("");
                    txtCountdown.setText("");
                    return;
                }
                long diff = next.epochMs() - System.currentTimeMillis();
                if (diff < 0) { refresh(loc); return; }
                long h = TimeUnit.MILLISECONDS.toHours(diff);
                long m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                long sec = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                txtNextPrayer.setText(getString(R.string.next_prayer_fmt, next.getArabicName()));
                txtCountdown.setText(String.format(new Locale("ar"),
                    "%02d:%02d:%02d", h, m, sec));
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

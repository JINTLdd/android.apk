package com.salah.app.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.models.DhikrItem;
import com.salah.app.services.AdhkarPlaybackService;
import com.salah.app.utils.AdhkarRepository;
import com.salah.app.utils.PreferencesManager;

import java.util.List;

/**
 * Step-by-step adhkar session.
 * - Loads the category list from JSON.
 * - Starts AdhkarPlaybackService (foreground) which plays audio of current item.
 * - User taps the big "counter" button to count repetitions. When count reaches target,
 *   double-vibrate + auto-advance to next item.
 * - When the last item finishes a completion screen is shown.
 * - Audio continues in background even if user backgrounds the app.
 */
public class AdhkarSessionActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY = "category";

    private String category;
    private List<DhikrItem> items;
    private int idx = 0;
    private int currentCount = 0;

    private TextView txtTitle, txtBody, txtPosition, txtCounter, txtTarget;
    private MaterialButton btnTap, btnSkip, btnPrev, btnStop, btnRestart;
    private ProgressBar progress;
    private View completionView, sessionView;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        setContentView(R.layout.activity_adhkar_session);

        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) category = "morning";

        items = AdhkarRepository.getCategory(this, category);
        if (items.isEmpty()) { finish(); return; }

        ((TextView) findViewById(R.id.txt_screen_title)).setText(AdhkarRepository.getCategoryTitle(category));
        findViewById(R.id.btn_close).setOnClickListener(v -> stopAndFinish());

        txtTitle = findViewById(R.id.txt_title);
        txtBody = findViewById(R.id.txt_body);
        txtPosition = findViewById(R.id.txt_position);
        txtCounter = findViewById(R.id.txt_counter);
        txtTarget = findViewById(R.id.txt_target);
        btnTap = findViewById(R.id.btn_tap);
        btnSkip = findViewById(R.id.btn_skip);
        btnPrev = findViewById(R.id.btn_prev);
        btnStop = findViewById(R.id.btn_stop_audio);
        btnRestart = findViewById(R.id.btn_restart);
        progress = findViewById(R.id.progress_session);
        completionView = findViewById(R.id.completion_view);
        sessionView = findViewById(R.id.session_view);

        btnTap.setOnClickListener(v -> onTap());
        btnSkip.setOnClickListener(v -> nextItem());
        btnPrev.setOnClickListener(v -> prevItem());
        btnStop.setOnClickListener(v -> AdhkarPlaybackService.stopNow(this));
        btnRestart.setOnClickListener(v -> {
            idx = 0; currentCount = 0;
            completionView.setVisibility(View.GONE);
            sessionView.setVisibility(View.VISIBLE);
            showCurrent();
        });

        showCurrent();
    }

    private void showCurrent() {
        if (idx >= items.size()) { showCompletion(); return; }
        DhikrItem d = items.get(idx);
        currentCount = 0;
        txtTitle.setText(d.title);
        txtBody.setText(d.text);
        txtPosition.setText(getString(R.string.session_position_fmt, idx + 1, items.size()));
        txtCounter.setText("0");
        txtTarget.setText(getString(R.string.session_target_fmt, d.count));
        progress.setMax(items.size());
        progress.setProgress(idx);
        // Auto-play audio for current item (via foreground service so it survives backgrounding)
        AdhkarPlaybackService.play(this, d.audioUrl, d.title);
    }

    private void onTap() {
        DhikrItem d = items.get(idx);
        currentCount++;
        txtCounter.setText(String.valueOf(currentCount));
        vibrate(40);
        if (currentCount >= d.count) {
            vibrate(180);
            new android.os.Handler(getMainLooper()).postDelayed(this::nextItem, 350);
        }
    }

    private void nextItem() {
        idx++;
        if (idx >= items.size()) { showCompletion(); }
        else { showCurrent(); }
    }

    private void prevItem() {
        if (idx > 0) { idx--; showCurrent(); }
    }

    private void showCompletion() {
        AdhkarPlaybackService.stopNow(this);
        sessionView.setVisibility(View.GONE);
        completionView.setVisibility(View.VISIBLE);
        TextView msg = findViewById(R.id.completion_text);
        msg.setText(getString(R.string.session_completed_fmt, AdhkarRepository.getCategoryTitle(category)));
        // Mark today's session as completed so we don't bother the user with reminder notifications.
        getSharedPreferences("salah_prefs", MODE_PRIVATE).edit()
            .putLong("last_completed_" + category, System.currentTimeMillis())
            .apply();
    }

    @Override
    public void onBackPressed() { stopAndFinish(); }

    private void stopAndFinish() {
        AdhkarPlaybackService.stopNow(this);
        finish();
    }

    private void vibrate(long ms) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Throwable ignored) {}
    }
}

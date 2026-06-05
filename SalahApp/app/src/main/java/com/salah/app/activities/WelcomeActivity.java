package com.salah.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.utils.AudioDownloader;
import com.salah.app.utils.PreferencesManager;

import java.util.concurrent.Executors;

/**
 * One-time welcome screen: offers to bulk-download all adhkar / adhan MP3s for offline use.
 * Skipped automatically on subsequent launches.
 */
public class WelcomeActivity extends AppCompatActivity {
    public static final String PREF_KEY_SHOWN = "welcome_shown";

    private ProgressBar progressBar;
    private TextView statusText;
    private TextView percentText;
    private MaterialButton btnDownload, btnSkip;
    private View progressLayout;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);

        // If already shown OR audio already downloaded, skip directly.
        SharedPreferences prefs = getSharedPreferences("salah_prefs", MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY_SHOWN, false) || AudioDownloader.isDownloaded(this)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_welcome);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        percentText = findViewById(R.id.percent_text);
        btnDownload = findViewById(R.id.btn_download);
        btnSkip = findViewById(R.id.btn_skip);
        progressLayout = findViewById(R.id.progress_layout);

        btnDownload.setOnClickListener(v -> startDownload());
        btnSkip.setOnClickListener(v -> {
            prefs.edit().putBoolean(PREF_KEY_SHOWN, true).apply();
            goToMain();
        });
    }

    private void startDownload() {
        btnDownload.setEnabled(false);
        btnSkip.setEnabled(false);
        progressLayout.setVisibility(View.VISIBLE);
        statusText.setText(R.string.welcome_preparing);

        Executors.newSingleThreadExecutor().execute(() ->
            AudioDownloader.downloadAll(this, new AudioDownloader.Progress() {
                @Override
                public void onProgress(int currentIndex, int total, String currentTitle) {
                    ui.post(() -> {
                        int pct = total == 0 ? 0 : (currentIndex * 100 / total);
                        progressBar.setProgress(pct);
                        percentText.setText(getString(R.string.welcome_pct_fmt, pct));
                        statusText.setText(getString(R.string.welcome_dl_fmt, currentIndex + 1, total));
                    });
                }
                @Override
                public void onComplete(int succeeded, int failed) {
                    ui.post(() -> {
                        progressBar.setProgress(100);
                        percentText.setText("100%");
                        statusText.setText(getString(R.string.welcome_done_fmt, succeeded, failed));
                        getSharedPreferences("salah_prefs", MODE_PRIVATE)
                            .edit().putBoolean(PREF_KEY_SHOWN, true).apply();
                        btnSkip.setEnabled(true);
                        btnSkip.setText(R.string.welcome_enter_app);
                        btnSkip.setOnClickListener(v -> goToMain());
                    });
                }
                @Override
                public void onError(String message) {
                    ui.post(() -> {
                        Toast.makeText(WelcomeActivity.this, message, Toast.LENGTH_LONG).show();
                        btnDownload.setEnabled(true);
                        btnSkip.setEnabled(true);
                    });
                }
            })
        );
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}

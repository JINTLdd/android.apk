package com.salah.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.PermissionHelper;
import com.salah.app.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private UserSettings s;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager.applyTheme(this);
        setContentView(R.layout.activity_settings);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }

        s = PreferencesManager.load(this);

        // Calculation method spinner
        Spinner spMethod = findViewById(R.id.spinner_method);
        ArrayAdapter<CharSequence> methodAdapter = ArrayAdapter.createFromResource(this,
            R.array.calc_method_labels, android.R.layout.simple_spinner_item);
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMethod.setAdapter(methodAdapter);
        spMethod.setSelection(indexForMethod(s.calculationMethodId));
        spMethod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                s.calculationMethodId = methodIdForIndex(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Madhab spinner
        Spinner spMadhab = findViewById(R.id.spinner_madhab);
        ArrayAdapter<CharSequence> madhabAdapter = ArrayAdapter.createFromResource(this,
            R.array.madhab_labels, android.R.layout.simple_spinner_item);
        madhabAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMadhab.setAdapter(madhabAdapter);
        spMadhab.setSelection("Hanafi".equalsIgnoreCase(s.madhabId) ? 1 : 0);
        spMadhab.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                s.madhabId = pos == 1 ? "Hanafi" : "Shafi";
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Adhan file spinner
        Spinner spAdhan = findViewById(R.id.spinner_adhan);
        ArrayAdapter<CharSequence> adhanAdapter = ArrayAdapter.createFromResource(this,
            R.array.adhan_labels, android.R.layout.simple_spinner_item);
        adhanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAdhan.setAdapter(adhanAdapter);
        spAdhan.setSelection(indexForAdhan(s.selectedAdhanFile));
        spAdhan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                s.selectedAdhanFile = adhanFileForIndex(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Switches
        Switch swAdhan = findViewById(R.id.sw_adhan);
        swAdhan.setChecked(s.adhanEnabled);
        swAdhan.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.adhanEnabled = v);

        Switch swMorning = findViewById(R.id.sw_morning);
        swMorning.setChecked(s.morningAthkarEnabled);
        swMorning.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.morningAthkarEnabled = v);

        Switch swEvening = findViewById(R.id.sw_evening);
        swEvening.setChecked(s.eveningAthkarEnabled);
        swEvening.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.eveningAthkarEnabled = v);

        Switch swDark = findViewById(R.id.sw_dark);
        swDark.setChecked(s.darkMode);
        swDark.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.darkMode = v);

        Switch swVib = findViewById(R.id.sw_vibrate);
        swVib.setChecked(s.vibrateOnAlarm);
        swVib.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.vibrateOnAlarm = v);

        TextView lblExactAlarm = findViewById(R.id.lbl_exact_alarm);
        MaterialButton btnExactAlarm = findViewById(R.id.btn_exact_alarm);
        if (PermissionHelper.canScheduleExactAlarms(this)) {
            lblExactAlarm.setText(R.string.exact_alarm_ok);
            btnExactAlarm.setVisibility(android.view.View.GONE);
        } else {
            lblExactAlarm.setText(R.string.exact_alarm_needed);
            btnExactAlarm.setVisibility(android.view.View.VISIBLE);
            btnExactAlarm.setOnClickListener(v -> PermissionHelper.openExactAlarmSettings(this));
        }

        MaterialButton btnBattery = findViewById(R.id.btn_battery);
        btnBattery.setOnClickListener(v -> PermissionHelper.requestIgnoreBatteryOptimizations(this));

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            PreferencesManager.save(this, s);
            PreferencesManager.applyTheme(this);
            AlarmScheduler.rescheduleAll(this);
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
            // Re-launch main activity to pick up new theme.
            Intent i = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ---- Helpers (keep arrays.xml in sync with these indexes) ----
    private int indexForMethod(String id) {
        switch (id) {
            case "MuslimWorldLeague": return 1;
            case "Egyptian": return 2;
            case "Karachi": return 3;
            case "Dubai": return 4;
            case "Qatar": return 5;
            case "Kuwait": return 6;
            case "NorthAmerica": return 7;
            case "MoonsightingCommittee": return 8;
            case "UmmAlQura":
            default: return 0;
        }
    }
    private String methodIdForIndex(int pos) {
        switch (pos) {
            case 1: return "MuslimWorldLeague";
            case 2: return "Egyptian";
            case 3: return "Karachi";
            case 4: return "Dubai";
            case 5: return "Qatar";
            case 6: return "Kuwait";
            case 7: return "NorthAmerica";
            case 8: return "MoonsightingCommittee";
            default: return "UmmAlQura";
        }
    }
    private int indexForAdhan(String fileName) {
        switch (fileName) {
            case "adhan_kuwait":      return 1;
            case "adhan_haram_makki": return 2;
            case "adhan_makkah":      return 3;
            case "adhan_quds":        return 4;
            case "adhan_brunei":      return 5;
            case "adhan_afasy":       return 6;
            case "adhan_other":       return 7;
            case "adhan_madinah":
            default:                  return 0;
        }
    }
    private String adhanFileForIndex(int pos) {
        switch (pos) {
            case 1: return "adhan_kuwait";
            case 2: return "adhan_haram_makki";
            case 3: return "adhan_makkah";
            case 4: return "adhan_quds";
            case 5: return "adhan_brunei";
            case 6: return "adhan_afasy";
            case 7: return "adhan_other";
            default: return "adhan_madinah";
        }
    }
}

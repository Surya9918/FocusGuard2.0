package com.focusguard.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.AppUsageSummary;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.databinding.ActivityMainBinding;
import com.focusguard.app.service.FocusTrackerService;
import com.focusguard.app.service.TrackingPolicy;
import com.focusguard.app.service.TrackingState;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private LiveData<DailyStats> stats;
    private LiveData<AppUsageSummary> topApp;
    private String observedDate;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferencesListener =
            (preferences, key) -> renderStatus();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!TrackingState.preferences(this).getBoolean("onboarding_complete", false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowPadding.apply(binding.getRoot());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_settings) return false;
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        });
        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_focus_breakers) {
                startActivity(new Intent(this, AppSelectorActivity.class));
            } else if (item.getItemId() == R.id.nav_ai_settings) {
                startActivity(new Intent(this, AiInsightsActivity.class));
            } else {
                return true;
            }
            return false;
        });
        binding.chipServiceStatus.setOnClickListener(view -> toggleTracking());
        TrackingState.preferences(this).registerOnSharedPreferenceChangeListener(preferencesListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) return;
        observeToday();
        renderStats(stats.getValue());
        renderStatus();
    }

    private void toggleTracking() {
        if (TrackingState.enabled(this)) {
            TrackingState.setEnabled(this, false);
            stopService(new Intent(this, FocusTrackerService.class));
            return;
        }
        if (!TrackingState.hasUsageAccess(this)) {
            Toast.makeText(this, R.string.tracking_permissions, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.tracking_start)
                .setMessage(R.string.tracking_explanation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.tracking_start, (dialog, which) -> startTracking())
                .show();
    }

    private void startTracking() {
        TrackingState.setEnabled(this, true);
        try {
            ContextCompat.startForegroundService(this, new Intent(this, FocusTrackerService.class));
        } catch (IllegalStateException | SecurityException error) {
            TrackingState.setEnabled(this, false);
            Toast.makeText(this, R.string.tracking_save_error, Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void observeToday() {
        String today = LocalDate.now().toString();
        if (today.equals(observedDate)) return;
        observedDate = today;
        if (stats != null) stats.removeObservers(this);
        if (topApp != null) topApp.removeObservers(this);
        AppDatabase db = AppDatabase.getInstance(this);
        stats = db.dailyStatsDao().getStatsLiveDataForDate(today);
        stats.observe(this, this::renderStats);
        topApp = db.appUsageDao().getTopFocusBreaker(today);
        topApp.observe(this, app -> {
            binding.tvMostUsedApp.setText(app == null ? getString(R.string.tracking_no_usage) : app.appName);
            binding.tvMostUsedTime.setText(app == null ? "" : formatDuration(app.totalSeconds));
        });
    }

    private void renderStats(DailyStats day) {
        long total = day == null ? 0 : day.totalScreenSeconds;
        long distractions = day == null ? 0 : day.distractionSeconds;
        int blocks = day == null ? 0 : day.blocksTriggered;
        long goalSeconds = TrackingState.preferences(this).getInt(SettingsActivity.PREF_DAILY_GOAL, 4) * 3600L;
        binding.tvScreenTime.setText(formatDuration(total));
        binding.progressScreenTime.setProgress((int) Math.min(100, total * 100 / Math.max(3600, goalSeconds)));
        binding.tvDistractionTime.setText(formatDuration(distractions));
        binding.tvBlocksToday.setText(String.valueOf(blocks));
        binding.tvFocusScore.setText(total == 0 ? getString(R.string.tracking_no_score)
                : String.valueOf(TrackingPolicy.focusScore(total, distractions)));
    }

    private void renderStatus() {
        if (binding == null) return;
        boolean active = TrackingState.enabled(this);
        binding.chipServiceStatus.setText(active ? R.string.tracking_stop : R.string.tracking_start);
        binding.tvFocusStatus.setText(active ? R.string.service_active : R.string.status_tracking_off);
        binding.tvFocusStatusSub.setText(active ? R.string.tracking_active_body
                : R.string.tracking_paused_body);
    }

    private String formatDuration(long seconds) {
        return getString(R.string.tracking_duration, seconds / 3600, seconds / 60 % 60);
    }

    @Override
    protected void onDestroy() {
        TrackingState.preferences(this).unregisterOnSharedPreferenceChangeListener(preferencesListener);
        super.onDestroy();
    }
}

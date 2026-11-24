package com.focusguard.app.ui;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.databinding.ActivitySettingsBinding;
import com.focusguard.app.databinding.ItemPermissionRowBinding;
import com.focusguard.app.service.FocusNotifications;
import com.focusguard.app.service.FocusTrackerService;
import com.focusguard.app.service.TrackingState;


public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "FocusGuardPrefs";
    public static final String PREF_STRICTNESS = "strictness_level"; // 0: Chill, 1: Balanced, 2: Strict
    public static final String PREF_DAILY_GOAL = "daily_goal_hours";
    public static final String PREF_SUMMARY = "enable_summary";
    public static final String PREF_REMINDERS = "enable_reminders";

    private ActivitySettingsBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowPadding.apply(binding.getRoot());

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupToolbar();
        setupStrictnessSlider();
        setupDailyGoal();
        setupNotificationSwitches();
        setupPrivacyActions();
        updatePermissionStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupStrictnessSlider() {
        int savedLevel = prefs.getInt(PREF_STRICTNESS, 1);
        binding.sliderStrictness.setValue(savedLevel);
        updateStrictnessDescription(savedLevel);

        binding.sliderStrictness.addOnChangeListener((slider, value, fromUser) -> {
            int level = (int) value;
            prefs.edit().putInt(PREF_STRICTNESS, level).apply();
            updateStrictnessDescription(level);
        });
    }

    private void updateStrictnessDescription(int level) {
        String desc;
        switch (level) {
            case 0:
                desc = getString(R.string.settings_strictness_desc_chill);
                break;
            case 2:
                desc = getString(R.string.settings_strictness_desc_strict);
                break;
            case 1:
            default:
                desc = getString(R.string.settings_strictness_desc_balanced);
                break;
        }
        binding.tvStrictnessDescription.setText(desc);
    }

    private void setupDailyGoal() {
        int goal = prefs.getInt(PREF_DAILY_GOAL, 4);
        updateGoalText(goal);

        binding.btnGoalDecrease.setOnClickListener(v -> {
            int currentGoal = prefs.getInt(PREF_DAILY_GOAL, 4);
            if (currentGoal > 1) {
                int newGoal = currentGoal - 1;
                prefs.edit().putInt(PREF_DAILY_GOAL, newGoal).apply();
                updateGoalText(newGoal);
            }
        });

        binding.btnGoalIncrease.setOnClickListener(v -> {
            int currentGoal = prefs.getInt(PREF_DAILY_GOAL, 4);
            if (currentGoal < 24) {
                int newGoal = currentGoal + 1;
                prefs.edit().putInt(PREF_DAILY_GOAL, newGoal).apply();
                updateGoalText(newGoal);
            }
        });
    }

    private void updateGoalText(int goal) {
        binding.tvGoalValue.setText(getString(R.string.settings_daily_goal_value, goal));
    }

    private void setupNotificationSwitches() {
        binding.switchDailySummary.setChecked(prefs.getBoolean(PREF_SUMMARY, false));
        binding.switchReminders.setChecked(prefs.getBoolean(PREF_REMINDERS, false));
        FocusNotifications.scheduleSummary(this, binding.switchDailySummary.isChecked());
        binding.switchDailySummary.setOnCheckedChangeListener((button, on) -> {
            prefs.edit().putBoolean(PREF_SUMMARY, on).apply();
            FocusNotifications.scheduleSummary(this, on);
            if (on) requestNotificationAccess();
        });
        binding.switchReminders.setOnCheckedChangeListener((button, on) -> {
            prefs.edit().putBoolean(PREF_REMINDERS, on).apply();
            if (on) requestNotificationAccess();
            else FocusNotifications.cancelReminder(this);
        });
    }

    private void requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void setupPrivacyActions() {
        binding.btnPrivacyPolicy.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.tracking_privacy)
                .setMessage(R.string.tracking_privacy_body)
                .setPositiveButton(android.R.string.ok, null)
                .show());
        binding.btnClearData.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.settings_clear_data_confirm_title)
                .setMessage(R.string.settings_clear_data_confirm_body)
                .setPositiveButton(R.string.settings_clear_data_confirm_yes, (dialog, which) -> clearData())
                .setNegativeButton(R.string.settings_clear_data_confirm_no, null)
                .show());
    }

    private void clearData() {
        TrackingState.setEnabled(this, false);
        stopService(new Intent(this, FocusTrackerService.class));
        TrackingState.clearSnoozes(this);
        prefs.edit().putBoolean(PREF_SUMMARY, false).putBoolean(PREF_REMINDERS, false)
                .remove("last_summary").remove("last_reminder").apply();
        binding.switchDailySummary.setChecked(false);
        binding.switchReminders.setChecked(false);
        FocusNotifications.scheduleSummary(this, false);
        FocusNotifications.cancelReminder(this);
        binding.btnClearData.setEnabled(false);
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        db.getQueryExecutor().execute(() -> {
            int message;
            try {
                db.clearAllTables();
                message = R.string.tracking_clear_done;
            } catch (RuntimeException error) {
                message = R.string.tracking_clear_error;
            }
            int result = message;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                binding.btnClearData.setEnabled(true);
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updatePermissionStatus() {
        // Usage Access
        boolean hasUsage = hasUsageStatsPermission();
        setupPermissionRow(binding.includePermUsage, 
                getString(R.string.settings_permission_usage_title),
                hasUsage ? getString(R.string.settings_permission_usage_sub_granted) : getString(R.string.settings_permission_usage_sub_missing),
                hasUsage,
                new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

        // Overlay
        boolean hasOverlay = Settings.canDrawOverlays(this);
        setupPermissionRow(binding.includePermOverlay,
                getString(R.string.settings_permission_overlay_title),
                hasOverlay ? getString(R.string.settings_permission_overlay_sub_granted) : getString(R.string.settings_permission_overlay_sub_missing),
                hasOverlay,
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));

        // Notifications
        boolean hasNotifications = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        setupPermissionRow(binding.includePermNotifications,
                getString(R.string.settings_permission_notifications_title),
                hasNotifications ? getString(R.string.settings_permission_notifications_sub_granted) : getString(R.string.settings_permission_notifications_sub_missing),
                hasNotifications,
                null);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifications) {
            binding.includePermNotifications.btnGrantPermission.setOnClickListener(v -> 
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101));
        }
    }

    private void setupPermissionRow(ItemPermissionRowBinding rowBinding, String title, String subtitle, boolean granted, Intent intent) {
        rowBinding.tvPermissionTitle.setText(title);
        rowBinding.tvPermissionSubtitle.setText(subtitle);
        rowBinding.imgPermissionStatus.setImageResource(granted ? R.drawable.ic_check_circle : android.R.drawable.ic_dialog_alert);
        rowBinding.imgPermissionStatus.setColorFilter(ContextCompat.getColor(this, granted ? R.color.status_focused_green : R.color.md_theme_light_error));
        rowBinding.btnGrantPermission.setVisibility(granted ? View.GONE : View.VISIBLE);
        
        if (!granted && intent != null) {
            rowBinding.btnGrantPermission.setOnClickListener(v -> startActivity(intent));
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}

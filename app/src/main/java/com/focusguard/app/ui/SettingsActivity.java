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

import java.util.concurrent.Executors;

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
        binding.switchDailySummary.setChecked(prefs.getBoolean(PREF_SUMMARY, true));
        binding.switchReminders.setChecked(prefs.getBoolean(PREF_REMINDERS, false));

        binding.switchDailySummary.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(PREF_SUMMARY, isChecked).apply());
        
        binding.switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(PREF_REMINDERS, isChecked).apply());
    }

    private void setupPrivacyActions() {
        binding.btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_clear_data_confirm_title)
                    .setMessage(R.string.settings_clear_data_confirm_body)
                    .setPositiveButton(R.string.settings_clear_data_confirm_yes, (dialog, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase.getInstance(getApplicationContext()).clearAllTables();
                            runOnUiThread(() -> Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show());
                        });
                    })
                    .setNegativeButton(R.string.settings_clear_data_confirm_no, null)
                    .show();
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

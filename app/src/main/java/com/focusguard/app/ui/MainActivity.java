package com.focusguard.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import io.flutter.embedding.android.FlutterActivity;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.databinding.ActivityMainBinding;
import com.focusguard.app.service.FocusTrackerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasUsageStatsPermission() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkPermissions();
        startTrackingService();

        setupUI();
        observeData();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_focus_breakers) {
                startActivity(new Intent(this, AppSelectorActivity.class));
                return true;
            } else if (id == R.id.nav_ai_settings) {
                // Launch the custom Flutter Activity that handles MethodChannels
                startActivity(
                    FlutterActivity.withNewEngine()
                        .initialRoute("/")
                        .activityClass(AiInsightsActivity.class)
                        .build(this)
                );
                return true;
            }
            return true;
        });

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void observeData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        AppDatabase db = AppDatabase.getInstance(this);

        // Observe Screen Time and Blocks from DailyStats
        db.dailyStatsDao().getStatsLiveDataForDate(today).observe(this, stats -> {
            if (stats != null) {
                // Update Screen Time
                long totalSeconds = stats.totalScreenSeconds;
                long minutes = totalSeconds / 60;
                long hours = minutes / 60;
                minutes = minutes % 60;
                binding.tvScreenTime.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));

                // Update progress ring (assuming 8 hours limit for 100%)
                int progress = (int) ((totalSeconds * 100) / (8 * 3600));
                binding.progressScreenTime.setProgress(Math.min(progress, 100));

                // Update Blocks Today
                binding.tvBlocksToday.setText(String.valueOf(stats.blocksTriggered));

                // Update Distraction Time
                long distMinutes = stats.distractionSeconds / 60;
                binding.tvDistractionTime.setText(String.format(Locale.getDefault(), "%dm", distMinutes));
                
                // Update Focus Score (simplified calculation)
                int focusScore = 100 - (int)(stats.distractionSeconds * 100 / Math.max(stats.totalScreenSeconds, 1));
                binding.tvFocusScore.setText(String.valueOf(Math.max(0, focusScore)));
            } else {
                binding.tvScreenTime.setText("0h 0m");
                binding.progressScreenTime.setProgress(0);
                binding.tvBlocksToday.setText("0");
                binding.tvDistractionTime.setText("0m");
                binding.tvFocusScore.setText("100");
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Please grant Usage Stats permission", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant Overlay permission", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, FocusTrackerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}

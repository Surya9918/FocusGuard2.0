package com.focusguard.app.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.databinding.LayoutBlockScreenBinding;
import com.focusguard.app.service.FocusTrackerService;
import com.focusguard.app.service.TrackingState;

import java.time.LocalDate;

public class BlockScreenActivity extends AppCompatActivity {
    private FocusTrackerService.Block block;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        block = FocusTrackerService.getBlock(getIntent().getLongExtra(FocusTrackerService.BLOCK_ID, 0));
        if (block == null || !TrackingState.enabled(this)) {
            finish();
            return;
        }
        LayoutBlockScreenBinding binding = LayoutBlockScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowPadding.apply(binding.getRoot());
        binding.tvBlockedAppName.setText(block.label);
        binding.tvBlockedDuration.setText(getString(R.string.tracking_duration,
                block.seconds / 3600, block.seconds / 60 % 60));
        try {
            binding.imgBlockedAppIcon.setImageDrawable(
                    getPackageManager().getApplicationIcon(block.packageName));
        } catch (PackageManager.NameNotFoundException ignored) {
            binding.imgBlockedAppIcon.setImageResource(R.drawable.ic_app_placeholder);
        }
        binding.btnCloseApp.setOnClickListener(view -> goHome());
        binding.btnSnooze.setOnClickListener(view -> {
            TrackingState.snooze(this, block.packageName);
            finish();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || block == null) return;
        if (FocusTrackerService.getBlock(block.id) == null || !TrackingState.enabled(this)) {
            finish();
            return;
        }
        if (!FocusTrackerService.markShown(block)) return;
        String date = LocalDate.now().toString();
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        db.getTransactionExecutor().execute(() -> db.runInTransaction(() -> {
            if (!TrackingState.enabled(getApplicationContext())) return;
            DailyStats stats = db.dailyStatsDao().getStatsForDate(date);
            if (stats == null) stats = new DailyStats(date);
            stats.blocksTriggered++;
            db.dailyStatsDao().insertOrReplace(stats);
        }));
    }

    private void goHome() {
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override
    protected void onDestroy() {
        if (block != null && !isChangingConfigurations()) FocusTrackerService.dismiss(block);
        super.onDestroy();
    }
}

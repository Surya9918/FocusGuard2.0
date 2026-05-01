package com.focusguard.app.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.focusguard.app.FocusGuardApp;
import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.TrackedApp;
import com.focusguard.app.ml.MLPredictor;
import com.focusguard.app.ui.BlockScreenActivity;
import com.focusguard.app.ui.MainActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * FocusTrackerService — The Heart of FocusGuard
 * ════════════════════════════════════════════
 * This foreground service monitors app usage in real-time and triggers the block screen.
 */
public class FocusTrackerService extends Service {
    private static final String TAG = "FocusTrackerService";
    private static final int NOTIFICATION_ID = 1001;
    private static final long POLL_INTERVAL = 1000L; // 1 second

    private Handler mHandler;
    private UsageStatsManager mUsageStatsManager;
    private Map<String, Long> mAppStartTimes = new HashMap<>();
    private String mCurrentPackage = "";

    private final Runnable mTrackerTask = new Runnable() {
        @Override
        public void run() {
            checkUsage();
            mHandler.postDelayed(this, POLL_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service", e);
        }
        mHandler.post(mTrackerTask);
    }

    private void checkUsage() {
        long time = System.currentTimeMillis();
        UsageEvents events = mUsageStatsManager.queryEvents(time - POLL_INTERVAL * 2, time);
        UsageEvents.Event event = new UsageEvents.Event();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                String newPackage = event.getPackageName();
                if (!newPackage.equals(mCurrentPackage)) {
                    handleAppExit(mCurrentPackage, event.getTimeStamp());
                    mCurrentPackage = newPackage;
                    mAppStartTimes.put(mCurrentPackage, event.getTimeStamp());
                    processAppSwitch(mCurrentPackage);
                }
            } else if (event.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED) {
                if (event.getPackageName().equals(mCurrentPackage)) {
                    handleAppExit(mCurrentPackage, event.getTimeStamp());
                    mCurrentPackage = "";
                }
            }
        }

        // Continuous monitoring of the current foreground app
        if (!mCurrentPackage.isEmpty()) {
            evaluateRisk(mCurrentPackage);
        }
    }

    private void handleAppExit(String packageName, long exitTime) {
        if (packageName == null || packageName.isEmpty()) return;
        Long startTime = mAppStartTimes.remove(packageName);
        if (startTime == null) return;

        long durationMs = exitTime - startTime;
        if (durationMs < 1000) return; // Ignore micro-sessions

        new Thread(() -> {
            AppDatabase db = FocusGuardApp.getInstance().getDatabase();
            TrackedApp tracked = db.trackedAppDao().getByPackageName(packageName);
            
            boolean isMonitored = tracked != null && tracked.isMonitored;
            int mlAppId = tracked != null ? tracked.mlAppId : 4;
            String appName = tracked != null ? tracked.appName : packageName;

            // Save record
            com.focusguard.app.data.AppUsageRecord record = new com.focusguard.app.data.AppUsageRecord(
                    packageName, appName, startTime, exitTime, 0.0f, false, mlAppId
            );
            db.appUsageDao().insertRecord(record);

            // Update DailyStats
            String today = record.sessionDate;
            com.focusguard.app.data.DailyStats stats = db.dailyStatsDao().getStatsForDate(today);
            if (stats == null) {
                stats = new com.focusguard.app.data.DailyStats(today);
            }
            stats.totalScreenSeconds += (durationMs / 1000);
            if (isMonitored) {
                stats.distractionSeconds += (durationMs / 1000);
            }
            stats.sessionsCount++;
            db.dailyStatsDao().insertOrReplace(stats);
            
            Log.d(TAG, "Saved session for " + packageName + " (" + (durationMs/1000) + "s)");
        }).start();
    }

    private void processAppSwitch(String packageName) {
        Log.d(TAG, "Switched to: " + packageName);
    }

    private void evaluateRisk(String packageName) {
        new Thread(() -> {
            AppDatabase db = FocusGuardApp.getInstance().getDatabase();
            TrackedApp trackedApp = db.trackedAppDao().getByPackageName(packageName);

            if (trackedApp != null && trackedApp.isMonitored) {
                Long startTime = mAppStartTimes.get(packageName);
                if (startTime == null) return;

                long durationSeconds = (System.currentTimeMillis() - startTime) / 1000;
                int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                MLPredictor predictor = FocusGuardApp.getInstance().getMLPredictor();
                if (predictor != null) {
                    float risk = predictor.predictRisk(trackedApp.mlAppId, durationSeconds, hourOfDay);
                    Log.d(TAG, "Risk for " + packageName + ": " + risk);

                    // Read strictness threshold from SharedPreferences
                    android.content.SharedPreferences prefs = getSharedPreferences("FocusGuardPrefs", Context.MODE_PRIVATE);
                    int strictnessLevel = prefs.getInt("strictness_level", 1);
                    float threshold = 0.60f; // Default (Balanced)
                    if (strictnessLevel == 0) threshold = 0.80f; // Chill
                    else if (strictnessLevel == 2) threshold = 0.40f; // Strict

                    if (risk > threshold) {
                        triggerBlock(packageName);
                    }
                }
            }
        }).start();
    }

    private void triggerBlock(String packageName) {
        new Thread(() -> {
            AppDatabase db = FocusGuardApp.getInstance().getDatabase();
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            com.focusguard.app.data.DailyStats stats = db.dailyStatsDao().getStatsForDate(today);
            if (stats != null) {
                stats.blocksTriggered++;
                db.dailyStatsDao().insertOrReplace(stats);
            }
        }).start();

        Intent intent = new Intent(this, BlockScreenActivity.class);
        intent.putExtra("package_name", packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, FocusGuardApp.CHANNEL_ID_TRACKING)
                .setContentTitle("FocusGuard Active")
                .setContentText("Monitoring your focus...")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // Placeholder icon
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mTrackerTask);
        super.onDestroy();
    }
}

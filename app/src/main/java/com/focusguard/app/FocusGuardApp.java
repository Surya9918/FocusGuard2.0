package com.focusguard.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.ml.MLPredictor;

/**
 * FocusGuardApp — Application singleton.
 *
 * Responsibilities:
 *   1. Create notification channels (required before any notification is shown)
 *   2. Pre-warm the ML model in a background thread so inference is instant
 *      when the user first opens a tracked app
 *   3. Provide global accessors for Room Database and MLPredictor
 *
 * Threading note:
 *   All initialization that involves I/O (DB creation, TFLite model loading)
 *   is deliberately offloaded to background threads to keep onCreate() fast
 *   and avoid Application Not Responding (ANR) errors.
 */
public class FocusGuardApp extends Application {

    private static final String TAG = "FocusGuardApp";

    // ─── Notification Channel IDs ─────────────────────────────────────────────
    /** Persistent channel for the "FocusGuard is active" foreground notification. */
    public static final String CHANNEL_ID_TRACKING   = "focusguard_tracking";

    /** Channel for daily usage summary nudges (low importance, no sound). */
    public static final String CHANNEL_ID_SUMMARY    = "focusguard_summary";

    /** Channel for urgent block alerts (high importance, shows heads-up). */
    public static final String CHANNEL_ID_ALERT      = "focusguard_alert";

    // ─── Global Singletons ────────────────────────────────────────────────────
    private static FocusGuardApp sInstance;
    private AppDatabase  mDatabase;
    private MLPredictor  mMLPredictor;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // Step 1: Create notification channels (no-op below Android 8.0)
        createNotificationChannels();

        // Step 2: Initialize Room Database in background
        // Room.databaseBuilder().build() on the main thread causes StrictMode
        // violations and potential ANR on slow devices with large databases.
        new Thread(() -> {
            mDatabase = AppDatabase.getInstance(this);
            Log.d(TAG, "Room database initialized.");
        }, "FocusGuard-DBInit").start();

        // Step 3: Pre-warm the TFLite model in background
        // First inference has a cold-start cost (~50–200ms) for interpreter setup.
        // Pre-warming here ensures the first real prediction is instant.
        new Thread(() -> {
            try {
                mMLPredictor = MLPredictor.getInstance(this);
                Log.d(TAG, "MLPredictor pre-warmed successfully.");
            } catch (Exception e) {
                // Model loading failure is non-fatal here — MLPredictor handles
                // graceful degradation internally (falls back to rule-based logic).
                Log.e(TAG, "MLPredictor pre-warm failed. Will retry on first use.", e);
            }
        }, "FocusGuard-MLInit").start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // NOTIFICATION CHANNELS
    // Required on Android 8.0+ (API 26). Must be created before posting any
    // notification. Creating an existing channel is a safe no-op.
    // ──────────────────────────────────────────────────────────────────────────
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        // Channel 1: Persistent tracking notification (low importance = no sound/badge)
        NotificationChannel trackingChannel = new NotificationChannel(
            CHANNEL_ID_TRACKING,
            "Active Monitoring",
            NotificationManager.IMPORTANCE_LOW
        );
        trackingChannel.setDescription("Shown while FocusGuard is monitoring your screen time.");
        trackingChannel.setShowBadge(false);
        trackingChannel.enableVibration(false);
        trackingChannel.setSound(null, null);

        // Channel 2: Daily summary (min importance — appears silently in shade)
        NotificationChannel summaryChannel = new NotificationChannel(
            CHANNEL_ID_SUMMARY,
            "Daily Check-In",
            NotificationManager.IMPORTANCE_MIN
        );
        summaryChannel.setDescription("A gentle daily summary of how you used your phone.");
        summaryChannel.setShowBadge(true);
        summaryChannel.enableVibration(false);

        // Channel 3: Block alerts (default importance — enough for heads-up popup)
        NotificationChannel alertChannel = new NotificationChannel(
            CHANNEL_ID_ALERT,
            "Focus Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        alertChannel.setDescription("Alerts when FocusGuard detects a potential distraction.");
        alertChannel.setShowBadge(true);
        alertChannel.enableVibration(true);
        alertChannel.setVibrationPattern(new long[]{0, 200, 100, 200});

        manager.createNotificationChannel(trackingChannel);
        manager.createNotificationChannel(summaryChannel);
        manager.createNotificationChannel(alertChannel);

        Log.d(TAG, "Notification channels created.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GLOBAL ACCESSORS
    // ──────────────────────────────────────────────────────────────────────────

    public static FocusGuardApp getInstance() {
        return sInstance;
    }

    /**
     * Returns the Room Database instance.
     * May be null briefly during cold start (background init not yet complete).
     * Callers should handle null gracefully.
     */
    public AppDatabase getDatabase() {
        if (mDatabase == null) {
            // Synchronous fallback — only hits on very first cold start
            mDatabase = AppDatabase.getInstance(this);
        }
        return mDatabase;
    }

    /**
     * Returns the MLPredictor singleton.
     * May be null if model loading failed. Callers must null-check.
     */
    public MLPredictor getMLPredictor() {
        return mMLPredictor;
    }
}

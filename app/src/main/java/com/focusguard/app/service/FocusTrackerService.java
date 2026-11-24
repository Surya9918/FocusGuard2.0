package com.focusguard.app.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.focusguard.app.FocusGuardApp;
import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.AppUsageRecord;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.data.TrackedApp;
import com.focusguard.app.ml.MLPredictor;
import com.focusguard.app.ui.BlockScreenActivity;
import com.focusguard.app.ui.MainActivity;

import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FocusTrackerService extends Service {
    public static final String BLOCK_ID = "focus_block_id";
    public static final String ACTION_STOP = "com.focusguard.app.STOP_TRACKING";
    private static final String TAG = "FocusTrackerService";
    private static final int NOTIFICATION_ID = 1001;
    private static final long CHECKPOINT_MS = 15_000;
    private static final long RETRY_MS = 30_000;
    private static volatile Block currentBlock;

    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private UsageStatsManager usage;
    private PowerManager power;
    private KeyguardManager keyguard;
    private AppDatabase db;
    private volatile boolean running;
    private long cursor;
    private String foregroundApp;
    private long sessionStart;
    private long storedThrough;
    private boolean countedSession;
    private long retryAt;

    public static final class Block {
        public final long id = SystemClock.elapsedRealtimeNanos();
        public final String packageName;
        public final String label;
        public final long seconds;
        private final long createdAt = SystemClock.elapsedRealtime();
        private boolean shown;

        private Block(TrackedApp app, long seconds) {
            packageName = app.packageName;
            label = app.appName;
            this.seconds = seconds;
        }
    }

    public static synchronized Block getBlock(long id) {
        Block block = currentBlock;
        return block != null && block.id == id
                && (block.shown || SystemClock.elapsedRealtime() - block.createdAt < RETRY_MS)
                ? block : null;
    }

    public static synchronized boolean markShown(Block block) {
        if (currentBlock != block || block.shown) return false;
        block.shown = true;
        return true;
    }

    public static synchronized void dismiss(Block block) {
        if (currentBlock == block) currentBlock = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        usage = getSystemService(UsageStatsManager.class);
        power = getSystemService(PowerManager.class);
        keyguard = getSystemService(KeyguardManager.class);
        db = AppDatabase.getInstance(this);
        cursor = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())
                || !TrackingState.enabled(this) || !TrackingState.hasUsageAccess(this)) {
            stopTracking();
            return START_NOT_STICKY;
        }
        if (!running) {
            try {
                Notification notification = trackingNotification();
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIFICATION_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "Tracking could not start", error);
                stopTracking();
                return START_NOT_STICKY;
            }
            running = true;
            worker.scheduleWithFixedDelay(this::poll, 0, 1, TimeUnit.SECONDS);
        }
        return START_STICKY;
    }

    private void poll() {
        if (!running) return;
        try {
            if (!TrackingState.enabled(this) || !TrackingState.hasUsageAccess(this)) {
                stopTracking();
                return;
            }
            long now = System.currentTimeMillis();
            if (now < cursor) {
                closeSession(cursor);
                cursor = now;
                return;
            }
            if (now == cursor) return;
            UsageEvents events = usage == null ? null : usage.queryEvents(cursor, now);
            if (events == null) {
                closeSession(cursor);
                cursor = now;
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent() && events.getNextEvent(event)) handleEvent(event);
            if (power == null || !power.isInteractive()
                    || keyguard != null && keyguard.isKeyguardLocked()) {
                closeSession(cursor);
                currentBlock = null;
            }
            cursor = now;
            if (foregroundApp == null) return;
            if (now - storedThrough >= CHECKPOINT_MS) saveThrough(now);
            evaluateRisk(now);
        } catch (RuntimeException error) {
            Log.e(TAG, "Tracking stopped after an unexpected error", error);
            stopTracking();
        }
    }

    private void handleEvent(UsageEvents.Event event) {
        int type = event.getEventType();
        String app = event.getPackageName();
        long time = event.getTimeStamp();
        if (type == UsageEvents.Event.ACTIVITY_RESUMED && app != null) {
            if (app.equals(foregroundApp)) return;
            closeSession(time);
            if (app.equals(getPackageName())) return;
            Block block = currentBlock;
            if (block != null && !app.equals(block.packageName)) dismiss(block);
            foregroundApp = app;
            sessionStart = storedThrough = time;
            countedSession = false;
            retryAt = 0;
        } else if (type == UsageEvents.Event.ACTIVITY_PAUSED && app != null
                && app.equals(foregroundApp)) {
            closeSession(time);
        } else if (type == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                || type == UsageEvents.Event.KEYGUARD_SHOWN) {
            closeSession(time);
            currentBlock = null;
        }
    }

    private void closeSession(long end) {
        if (foregroundApp == null) return;
        saveThrough(end);
        foregroundApp = null;
    }

    private void saveThrough(long end) {
        ZoneId zone = ZoneId.systemDefault();
        while (storedThrough < end) {
            long start = storedThrough;
            var date = Instant.ofEpochMilli(start).atZone(zone).toLocalDate();
            long midnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
            long boundary = Math.min(end, midnight);
            long seconds = (boundary - start) / 1000;
            if (seconds > 0) {
                db.runInTransaction(() -> {
                    if (!running || !TrackingState.enabled(this)) return;
                    TrackedApp app = db.trackedAppDao().getByPackageName(foregroundApp);
                    boolean monitored = app != null && app.isMonitored;
                    if (monitored) {
                        AppUsageRecord record = new AppUsageRecord(app.packageName, app.appName,
                                start, start + seconds * 1000, 0f, false, app.mlAppId);
                        record.sessionDate = date.toString();
                        db.appUsageDao().insertRecord(record);
                    }
                    DailyStats stats = db.dailyStatsDao().getStatsForDate(date.toString());
                    if (stats == null) stats = new DailyStats(date.toString());
                    stats.totalScreenSeconds += seconds;
                    if (monitored) stats.distractionSeconds += seconds;
                    if (!countedSession) stats.sessionsCount++;
                    db.dailyStatsDao().insertOrReplace(stats);
                });
                countedSession = true;
                storedThrough = start + seconds * 1000;
                FocusNotifications.maybeRemind(this, db);
            }
            if (boundary != midnight) return;
            storedThrough = midnight;
            countedSession = false;
        }
    }

    private void evaluateRisk(long now) {
        long seconds = (now - sessionStart) / 1000;
        if (seconds < 60 || now < retryAt || !running || !TrackingState.enabled(this)
                || !Settings.canDrawOverlays(this) || TrackingState.isSnoozed(this, foregroundApp, now)) {
            return;
        }
        Block prior = currentBlock;
        if (prior != null && getBlock(prior.id) != null) return;
        TrackedApp app = db.trackedAppDao().getByPackageName(foregroundApp);
        if (app == null || !app.isMonitored) return;
        int hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).getHour();
        float risk = MLPredictor.getInstance(this).predictRisk(app.mlAppId, seconds, hour);
        int strictness = TrackingState.preferences(this).getInt("strictness_level", 1);
        if (!TrackingPolicy.shouldBlock(seconds, risk, strictness)) return;

        Block block = new Block(app, seconds);
        currentBlock = block;
        retryAt = now + RETRY_MS;
        try {
            startActivity(new Intent(this, BlockScreenActivity.class)
                    .putExtra(BLOCK_ID, block.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException error) {
            dismiss(block);
            Log.w(TAG, "Focus nudge could not open", error);
        }
    }

    private Notification trackingNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 0,
                new Intent(this, FocusTrackerService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, FocusGuardApp.CHANNEL_ID_TRACKING)
                .setSmallIcon(R.drawable.ic_lock)
                .setContentTitle(getString(R.string.notification_tracking_title))
                .setContentText(getString(R.string.tracking_active_body))
                .setContentIntent(open)
                .addAction(0, getString(R.string.tracking_stop), stop)
                .setOngoing(true)
                .build();
    }

    private void stopTracking() {
        TrackingState.setEnabled(this, false);
        FocusNotifications.cancelReminder(this);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        currentBlock = null;
        worker.shutdown();
        super.onDestroy();
    }
}

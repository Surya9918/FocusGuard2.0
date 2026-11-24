package com.focusguard.app.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.focusguard.app.FocusGuardApp;
import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.ui.MainActivity;
import com.focusguard.app.ui.SettingsActivity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class FocusNotifications {
    static final int SUMMARY_HOUR = 20;
    private static final int SUMMARY_ID = 1002;
    private static final int REMINDER_ID = 1003;
    private static final String SUMMARY = "daily-summary";

    private FocusNotifications() {}

    public static void scheduleSummary(Context context, boolean enabled) {
        WorkManager work = WorkManager.getInstance(context);
        if (!enabled) {
            work.cancelUniqueWork(SUMMARY);
            NotificationManagerCompat.from(context).cancel(SUMMARY_ID);
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(SUMMARY_HOUR).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        work.enqueueUniquePeriodicWork(SUMMARY, ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(DailySummaryWorker.class, 1, TimeUnit.DAYS)
                        .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                        .build());
    }

    static boolean postSummary(Context context, DailyStats stats) {
        return post(context, SUMMARY_ID, FocusGuardApp.CHANNEL_ID_SUMMARY,
                R.string.notification_daily_summary_title,
                context.getString(R.string.tracking_summary, stats.date,
                        stats.totalScreenSeconds / 3600, stats.totalScreenSeconds / 60 % 60));
    }

    public static void cancelReminder(Context context) {
        NotificationManagerCompat.from(context).cancel(REMINDER_ID);
    }

    static void maybeRemind(Context context, AppDatabase db) {
        var prefs = TrackingState.preferences(context);
        if (!TrackingState.enabled(context) || !prefs.getBoolean(SettingsActivity.PREF_REMINDERS, false)) return;
        String today = LocalDate.now().toString();
        if (today.equals(prefs.getString("last_reminder", ""))) return;
        DailyStats stats = db.dailyStatsDao().getStatsForDate(today);
        long goal = Math.max(1, prefs.getInt(SettingsActivity.PREF_DAILY_GOAL, 4)) * 3600L;
        if (stats != null && stats.totalScreenSeconds >= goal * 9 / 10
                && post(context, REMINDER_ID, FocusGuardApp.CHANNEL_ID_ALERT, R.string.tracking_goal_title,
                context.getString(R.string.tracking_goal_body))) {
            prefs.edit().putString("last_reminder", today).apply();
        }
    }

    private static boolean post(Context context, int id, String channel, int title, String text) {
        NotificationManagerCompat notifications = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        if (!notifications.areNotificationsEnabled()) return false;
        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        try {
            notifications.notify(id, new NotificationCompat.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_lock)
                    .setContentTitle(context.getString(title))
                    .setContentText(text)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build());
            return true;
        } catch (SecurityException ignored) {
            return false;
        }
    }
}

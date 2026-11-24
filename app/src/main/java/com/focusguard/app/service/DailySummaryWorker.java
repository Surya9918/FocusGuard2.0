package com.focusguard.app.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.ui.SettingsActivity;

import java.time.ZonedDateTime;

public class DailySummaryWorker extends Worker {
    public DailySummaryWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        var prefs = TrackingState.preferences(context);
        if (!prefs.getBoolean(SettingsActivity.PREF_SUMMARY, false)) return Result.success();
        ZonedDateTime now = ZonedDateTime.now();
        String date = (now.getHour() < FocusNotifications.SUMMARY_HOUR
                ? now.toLocalDate().minusDays(1) : now.toLocalDate()).toString();
        if (date.equals(prefs.getString("last_summary", ""))) return Result.success();
        DailyStats stats = AppDatabase.getInstance(context).dailyStatsDao().getStatsForDate(date);
        if (stats != null && stats.totalScreenSeconds > 0
                && prefs.getBoolean(SettingsActivity.PREF_SUMMARY, false)
                && FocusNotifications.postSummary(context, stats)) {
            prefs.edit().putString("last_summary", date).apply();
        }
        return Result.success();
    }
}

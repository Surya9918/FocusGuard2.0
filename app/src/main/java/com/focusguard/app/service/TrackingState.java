package com.focusguard.app.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

public final class TrackingState {
    private TrackingState() {}

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("FocusGuardPrefs", Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) {
        return preferences(context).getBoolean("tracking_enabled", false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean("tracking_enabled", enabled).apply();
    }

    public static boolean hasUsageAccess(Context context) {
        AppOpsManager ops = context.getSystemService(AppOpsManager.class);
        return ops != null && ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private static SharedPreferences snoozes(Context context) {
        return context.getSharedPreferences("focus_snoozes", Context.MODE_PRIVATE);
    }

    public static void snooze(Context context, String app) {
        snoozes(context).edit().putLong(app, System.currentTimeMillis()
                + TrackingPolicy.SNOOZE_MS).apply();
    }

    public static boolean isSnoozed(Context context, String app, long now) {
        SharedPreferences prefs = snoozes(context);
        long expiry = prefs.getLong(app, 0);
        if (TrackingPolicy.snoozed(now, expiry)) return true;
        if (expiry != 0) prefs.edit().remove(app).apply();
        return false;
    }

    public static void clearSnoozes(Context context) {
        snoozes(context).edit().clear().apply();
    }
}

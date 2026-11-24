package com.focusguard.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class FocusGuardApp extends Application {
    public static final String CHANNEL_ID_TRACKING = "focusguard_tracking";
    public static final String CHANNEL_ID_SUMMARY = "focusguard_summary";
    public static final String CHANNEL_ID_ALERT = "focusguard_alert";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel tracking = new NotificationChannel(CHANNEL_ID_TRACKING,
                "Active monitoring", NotificationManager.IMPORTANCE_LOW);
        tracking.setShowBadge(false);
        manager.createNotificationChannel(tracking);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID_SUMMARY,
                "Daily check-in", NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID_ALERT,
                "Focus reminders", NotificationManager.IMPORTANCE_DEFAULT));
    }
}

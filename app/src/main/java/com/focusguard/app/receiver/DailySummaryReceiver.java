package com.focusguard.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * DailySummaryReceiver — Placeholder for daily summary logic.
 */
public class DailySummaryReceiver extends BroadcastReceiver {
    private static final String TAG = "DailySummaryReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Daily summary broadcast received.");
        // Logic to trigger a WorkManager job for daily summary would go here.
    }
}

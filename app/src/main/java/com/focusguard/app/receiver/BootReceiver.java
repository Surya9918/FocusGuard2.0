package com.focusguard.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.focusguard.app.service.FocusTrackerService;
import com.focusguard.app.service.TrackingState;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        if (!TrackingState.enabled(context)) return;
        if (!TrackingState.hasUsageAccess(context)) {
            TrackingState.setEnabled(context, false);
            return;
        }
        try {
            ContextCompat.startForegroundService(context, new Intent(context, FocusTrackerService.class));
        } catch (IllegalStateException | SecurityException failure) {
            TrackingState.setEnabled(context, false);
            Log.w("FocusGuard", "Tracking could not resume after restart", failure);
        }
    }
}

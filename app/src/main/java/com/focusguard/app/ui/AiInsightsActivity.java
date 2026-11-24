package com.focusguard.app.ui;

import androidx.annotation.NonNull;

import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.service.TrackingPolicy;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class AiInsightsActivity extends FlutterActivity {
    private static final String CHANNEL = "com.focusguard.app/stats";
    private MethodChannel channel;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine engine) {
        super.configureFlutterEngine(engine);
        channel = new MethodChannel(engine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler((call, result) -> {
            if (call.method.equals("getDailyStats")) {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.getQueryExecutor().execute(() -> fetchDailyStats(db, result));
            } else {
                result.notImplemented();
            }
        });
    }

    @Override
    public void cleanUpFlutterEngine(@NonNull FlutterEngine engine) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        super.cleanUpFlutterEngine(engine);
    }

    private void fetchDailyStats(AppDatabase db, MethodChannel.Result result) {
        try {
            DailyStats day = db.dailyStatsDao().getStatsForDate(LocalDate.now().toString());
            long total = day == null ? 0 : day.totalScreenSeconds;
            long distraction = day == null ? 0 : day.distractionSeconds;
            Map<String, Object> values = new HashMap<>();
            values.put("totalScreenSeconds", total);
            values.put("distractionSeconds", distraction);
            values.put("blocksTriggered", day == null ? 0 : day.blocksTriggered);
            values.put("focusScore", total == 0 ? null : TrackingPolicy.focusScore(total, distraction));
            runOnUiThread(() -> {
                if (channel != null) result.success(values);
            });
        } catch (RuntimeException error) {
            runOnUiThread(() -> {
                if (channel != null) result.error("STATS_UNAVAILABLE", null, null);
            });
        }
    }
}

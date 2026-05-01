package com.focusguard.app.ui;

import androidx.annotation.NonNull;

import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.DailyStats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class AiInsightsActivity extends FlutterActivity {
    private static final String CHANNEL = "com.focusguard.app/stats";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    if (call.method.equals("getDailyStats")) {
                        fetchDailyStats(result);
                    } else {
                        result.notImplemented();
                    }
                });
    }

    private void fetchDailyStats(MethodChannel.Result result) {
        executorService.execute(() -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            AppDatabase db = AppDatabase.getInstance(this);
            DailyStats stats = db.dailyStatsDao().getStatsForDate(today);

            Map<String, Object> statsMap = new HashMap<>();
            if (stats != null) {
                statsMap.put("totalScreenSeconds", stats.totalScreenSeconds);
                statsMap.put("distractionSeconds", stats.distractionSeconds);
                statsMap.put("blocksTriggered", stats.blocksTriggered);
                
                // Calculate Focus Score
                int focusScore = 100 - (int)(stats.distractionSeconds * 100 / Math.max(stats.totalScreenSeconds, 1));
                statsMap.put("focusScore", Math.max(0, focusScore));
            } else {
                statsMap.put("totalScreenSeconds", 0L);
                statsMap.put("distractionSeconds", 0L);
                statsMap.put("blocksTriggered", 0);
                statsMap.put("focusScore", 100);
            }

            runOnUiThread(() -> result.success(statsMap));
        });
    }
}

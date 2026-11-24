package com.focusguard.app.ui;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.AppUsageRecord;
import com.focusguard.app.data.DailyStats;
import com.focusguard.app.data.TrackedApp;
import androidx.room.Room;
import com.focusguard.app.service.TrackingState;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class ReleaseSmokeTest {
    @Test
    public void dashboardLoadsWhenTrackingIsOff() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackingState.preferences(context).edit().putBoolean("onboarding_complete", true)
                .putBoolean("tracking_enabled", false).commit();
        try (ActivityScenario<MainActivity> app = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.tracking_start)).check(matches(isDisplayed()));
            onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void settingsDisplaysNotificationAndPrivacyControls() {
        try (ActivityScenario<SettingsActivity> app = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.switch_daily_summary)).check(matches(
                    androidx.test.espresso.matcher.ViewMatchers.isEnabled()));
            onView(withId(R.id.btn_privacy_policy)).check(matches(
                    androidx.test.espresso.matcher.ViewMatchers.isEnabled()));
        }
    }
    @Test
    public void focusBreakersLoads() {
        try (ActivityScenario<AppSelectorActivity> app = ActivityScenario.launch(AppSelectorActivity.class)) {
            onView(withId(R.id.fab_add_app)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void roomWritesAndClearsLocalHistory() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (AppDatabase database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build()) {
            String date = "2026-11-24";
            String packageName = "com.example.focusbreaker";
            AppUsageRecord usage = new AppUsageRecord(packageName, "Example",
                    1_000, 181_000, 0, false, 0);
            usage.sessionDate = date;
            database.runInTransaction(() -> {
                database.trackedAppDao().insertOrUpdate(new TrackedApp(packageName, "Example", 0));
                database.appUsageDao().insertRecord(usage);
                DailyStats day = new DailyStats(date);
                day.totalScreenSeconds = 180;
                day.distractionSeconds = 180;
                database.dailyStatsDao().insertOrReplace(day);
            });
            assertEquals(180L, database.dailyStatsDao().getStatsForDate(date).distractionSeconds);
            assertEquals(1, database.trackedAppDao().getMonitoredApps().size());
            database.clearAllTables();
            assertNull(database.dailyStatsDao().getStatsForDate(date));
            assertEquals(0, database.trackedAppDao().getMonitoredApps().size());
        }
    }
}

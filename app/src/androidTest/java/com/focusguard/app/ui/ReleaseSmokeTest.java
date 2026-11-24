package com.focusguard.app.ui;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.focusguard.app.R;
import com.focusguard.app.service.TrackingState;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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
}

package com.focusguard.app.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrackingPolicyTest {
    @Test
    public void snoozeExpiresAtFiveMinutes() {
        long start = 100_000;
        long expiry = start + TrackingPolicy.SNOOZE_MS;
        assertTrue(TrackingPolicy.snoozed(start, expiry));
        assertTrue(TrackingPolicy.snoozed(expiry - 1, expiry));
        assertFalse(TrackingPolicy.snoozed(expiry, expiry));
        assertFalse(TrackingPolicy.snoozed(start - 1, expiry));
    }

    @Test
    public void strictnessControlsDurationFallback() {
        assertFalse(TrackingPolicy.shouldBlock(59, 1, 2));
        assertTrue(TrackingPolicy.shouldBlock(240, TrackingPolicy.durationRisk(240), 2));
        assertFalse(TrackingPolicy.shouldBlock(359, TrackingPolicy.durationRisk(359), 1));
        assertTrue(TrackingPolicy.shouldBlock(360, TrackingPolicy.durationRisk(360), 1));
        assertFalse(TrackingPolicy.shouldBlock(479, TrackingPolicy.durationRisk(479), 0));
        assertTrue(TrackingPolicy.shouldBlock(480, TrackingPolicy.durationRisk(480), 0));
        assertFalse(TrackingPolicy.shouldBlock(480, Float.NaN, 0));
    }

    @Test
    public void focusScoreUsesObservedScreenTimeAndStaysInRange() {
        assertEquals(75, TrackingPolicy.focusScore(3600, 900));
        assertEquals(0, TrackingPolicy.focusScore(60, 120));
        assertEquals(100, TrackingPolicy.focusScore(60, -1));
        assertEquals(100, TrackingPolicy.focusScore(0, 0));
    }
}

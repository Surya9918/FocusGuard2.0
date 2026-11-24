package com.focusguard.app.service;

public final class TrackingPolicy {
    public static final long SNOOZE_MS = 300_000;

    private TrackingPolicy() {}

    public static boolean snoozed(long now, long expiry) {
        return expiry > now && expiry - now <= SNOOZE_MS;
    }

    public static int focusScore(long total, long distraction) {
        if (total <= 0) return 100;
        double ratio = Math.max(0, Math.min(1, distraction / (double) total));
        return 100 - (int) (ratio * 100);
    }

    public static boolean shouldBlock(long seconds, float risk, int strictness) {
        float threshold = strictness == 0 ? 0.8f : strictness == 2 ? 0.4f : 0.6f;
        return seconds >= 60 && risk >= threshold && risk <= 1f;
    }

    public static float durationRisk(long seconds) {
        return Math.min(1f, Math.max(0f, seconds / 600f));
    }
}

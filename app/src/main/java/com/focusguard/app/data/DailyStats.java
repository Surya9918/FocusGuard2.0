package com.focusguard.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * DailyStats — Room Entity
 * ════════════════════════
 * Aggregated screen time statistics per day. Updated by FocusTrackerService
 * at the end of each tracked session and via a nightly WorkManager job.
 *
 * Purpose: Powers the dashboard's "Total Screen Time Today" progress ring
 * and the 7-day bar chart without needing expensive live DB aggregations.
 */
@Entity(tableName = "daily_stats")
public class DailyStats {

    /** Date in "YYYY-MM-DD" format — the natural primary key for a day. */
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "date")
    public String date;

    /** Total screen time in seconds across ALL apps (tracked + untracked). */
    @ColumnInfo(name = "total_screen_seconds")
    public long totalScreenSeconds;

    /** Total screen time in seconds spent on apps marked as "Focus Breakers". */
    @ColumnInfo(name = "distraction_seconds")
    public long distractionSeconds;

    /** Number of times the block screen was triggered today. */
    @ColumnInfo(name = "blocks_triggered")
    public int blocksTriggered;

    /** Number of distinct app sessions tracked today. */
    @ColumnInfo(name = "sessions_count")
    public int sessionsCount;

    /** The single most-used app package name today (for "Biggest distraction" UI). */
    @ColumnInfo(name = "top_distraction_package")
    public String topDistractionPackage;

    public DailyStats() {}

    public DailyStats(String date) {
        this.date = date;
        this.totalScreenSeconds    = 0;
        this.distractionSeconds    = 0;
        this.blocksTriggered       = 0;
        this.sessionsCount         = 0;
        this.topDistractionPackage = null;
    }
}

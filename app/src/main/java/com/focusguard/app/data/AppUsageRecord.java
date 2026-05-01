package com.focusguard.app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * AppUsageRecord — Room Entity
 * ════════════════════════════
 * Represents a single tracked app usage session.
 *
 * One record = one continuous app session (from ACTIVITY_RESUMED to ACTIVITY_PAUSED).
 *
 * Index on (package_name, session_start) for fast date-range queries used
 * by the dashboard and weekly report features.
 */
@Entity(
    tableName = "app_usage_records",
    indices = {
        @Index(value = {"package_name", "session_start"}),
        @Index(value = {"session_date"})
    }
)
public class AppUsageRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Package name of the app, e.g. "com.instagram.android" */
    @ColumnInfo(name = "package_name")
    public String packageName;

    /** Human-readable app name, e.g. "Instagram". Cached to avoid PackageManager calls. */
    @ColumnInfo(name = "app_name")
    public String appName;

    /** Unix timestamp (ms) when the session started (ACTIVITY_RESUMED event time). */
    @ColumnInfo(name = "session_start")
    public long sessionStartMs;

    /** Unix timestamp (ms) when the session ended (ACTIVITY_PAUSED event time). */
    @ColumnInfo(name = "session_end")
    public long sessionEndMs;

    /** Session duration in seconds, derived from (sessionEnd - sessionStart). */
    @ColumnInfo(name = "duration_seconds")
    public long durationSeconds;

    /** Date string "YYYY-MM-DD" for easy daily aggregation queries. */
    @ColumnInfo(name = "session_date")
    public String sessionDate;

    /** The ML-predicted distraction risk for this session, range [0.0, 1.0]. */
    @ColumnInfo(name = "distraction_risk")
    public float distractionRisk;

    /** Whether this session was flagged as a distraction (risk > user threshold). */
    @ColumnInfo(name = "was_blocked")
    public boolean wasBlocked;

    /** The app_id integer that was passed to MLPredictor for this session. */
    @ColumnInfo(name = "ml_app_id")
    public int mlAppId;

    public AppUsageRecord() {}

    // Convenience constructor for creating records from FocusTrackerService
    public AppUsageRecord(String packageName, String appName, long sessionStartMs,
                          long sessionEndMs, float distractionRisk, boolean wasBlocked,
                          int mlAppId) {
        this.packageName     = packageName;
        this.appName         = appName;
        this.sessionStartMs  = sessionStartMs;
        this.sessionEndMs    = sessionEndMs;
        this.durationSeconds = (sessionEndMs - sessionStartMs) / 1000;
        this.distractionRisk = distractionRisk;
        this.wasBlocked      = wasBlocked;
        this.mlAppId         = mlAppId;

        // Generate date string for the session_date column
        java.text.SimpleDateFormat sdf =
            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        this.sessionDate = sdf.format(new java.util.Date(sessionStartMs));
    }
}

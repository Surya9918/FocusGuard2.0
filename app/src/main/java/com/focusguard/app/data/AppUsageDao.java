package com.focusguard.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecord(AppUsageRecord record);

    /**
     * Total screen time in seconds for a given date.
     * Returns 0 if no records found (COALESCE prevents null).
     */
    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM app_usage_records " +
           "WHERE session_date = :date")
    LiveData<Long> getTotalScreenTimeForDate(String date);

    /**
     * All usage records for a given date, ordered by most recent first.
     * LiveData automatically notifies observers when new sessions are inserted.
     */
    @Query("SELECT * FROM app_usage_records WHERE session_date = :date " +
           "ORDER BY session_start DESC")
    LiveData<List<AppUsageRecord>> getRecordsForDate(String date);

    /**
     * Per-app usage totals for a date range (for the weekly bar chart).
     * Returns package_name and sum of duration.
     */
    @Query("SELECT package_name AS packageName, app_name AS appName, SUM(duration_seconds) AS totalSeconds " +
           "FROM app_usage_records " +
           "WHERE session_date BETWEEN :startDate AND :endDate " +
           "GROUP BY package_name ORDER BY totalSeconds DESC")
    LiveData<List<AppUsageSummary>> getAppUsageSummaryForRange(String startDate, String endDate);

    /**
     * Delete records older than 30 days (privacy-preserving data retention policy).
     * Called by a monthly WorkManager job.
     */
    @Query("DELETE FROM app_usage_records WHERE session_date < :cutoffDate")
    void deleteRecordsOlderThan(String cutoffDate);

    @Query("SELECT COUNT(*) FROM app_usage_records WHERE session_date = :date AND was_blocked = 1")
    int getBlockCountForDate(String date);
    @Query("SELECT usage.package_name AS packageName, tracked.app_name AS appName, "
            + "SUM(usage.duration_seconds) AS totalSeconds FROM app_usage_records AS usage "
            + "JOIN tracked_apps AS tracked ON tracked.package_name = usage.package_name "
            + "WHERE usage.session_date = :date AND tracked.is_monitored = 1 "
            + "GROUP BY usage.package_name, tracked.app_name "
            + "ORDER BY totalSeconds DESC, usage.package_name LIMIT 1")
    LiveData<AppUsageSummary> getTopFocusBreaker(String date);
}

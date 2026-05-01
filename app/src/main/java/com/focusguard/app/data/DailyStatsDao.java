package com.focusguard.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(DailyStats stats);

    @Update
    void update(DailyStats stats);

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    LiveData<DailyStats> getStatsLiveDataForDate(String date);

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    DailyStats getStatsForDate(String date);

    /** Last 7 days for the weekly trend chart. */
    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    LiveData<List<DailyStats>> getLast7Days();

    @Query("SELECT COALESCE(SUM(distraction_seconds), 0) FROM daily_stats " +
           "WHERE date BETWEEN :startDate AND :endDate")
    long getTotalDistractionSecondsForRange(String startDate, String endDate);
}

package com.focusguard.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TrackedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TrackedApp app);

    @Update
    void update(TrackedApp app);

    @Delete
    void delete(TrackedApp app);

    /** All tracked apps — backs the RecyclerView in AppSelectorActivity. */
    @Query("SELECT * FROM tracked_apps ORDER BY app_name ASC")
    LiveData<List<TrackedApp>> getAllTrackedApps();

    /** Only the apps that are actively being monitored by the ML engine. */
    @Query("SELECT * FROM tracked_apps WHERE is_monitored = 1")
    List<TrackedApp> getMonitoredApps();

    @Query("SELECT * FROM tracked_apps WHERE package_name = :packageName LIMIT 1")
    TrackedApp getByPackageName(String packageName);

    @Query("UPDATE tracked_apps SET is_monitored = :isMonitored WHERE package_name = :packageName")
    void setMonitored(String packageName, boolean isMonitored);
}

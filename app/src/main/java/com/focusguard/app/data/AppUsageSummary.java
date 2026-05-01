package com.focusguard.app.data;

import androidx.room.ColumnInfo;

public class AppUsageSummary {
    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "appName")
    public String appName;

    @ColumnInfo(name = "totalSeconds")
    public long   totalSeconds;
}

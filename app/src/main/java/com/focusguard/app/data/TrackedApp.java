package com.focusguard.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * TrackedApp — Room Entity
 * ════════════════════════
 * Represents an app the user has designated as a potential "Focus Breaker."
 *
 * The ml_app_id field maps the installed app to one of the 10 category IDs
 * that the ML model was trained on. This mapping is handled by AppIdMapper.java.
 *
 * When is_monitored = true, FocusTrackerService will run ML predictions
 * on usage sessions for this app. When false, the app is ignored.
 */
@Entity(tableName = "tracked_apps")
public class TrackedApp {

    /** Package name is the unique, stable identifier for an Android app. */
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "package_name")
    public String packageName;

    /** Cached app display name for the UI (avoids PackageManager calls in lists). */
    @ColumnInfo(name = "app_name")
    public String appName;

    /**
     * The ML category ID (0–9) this app maps to in the TFLite model.
     * Set by AppIdMapper.categorize(packageName).
     * Default: 4 (news_feeds) — a reasonable middle-ground for unknown apps.
     */
    @ColumnInfo(name = "ml_app_id")
    public int mlAppId;

    /**
     * Whether the user has toggled "Is this app a distraction for you?" to ON.
     * Only apps with is_monitored = true are checked by the ML engine.
     */
    @ColumnInfo(name = "is_monitored")
    public boolean isMonitored;

    /**
     * Base64-encoded small app icon (48dp), cached at registration time.
     * Stored in DB to avoid repeated PackageManager icon loading in RecyclerView.
     * Null if icon caching failed — UI falls back to a generic placeholder.
     */
    @ColumnInfo(name = "icon_base64")
    public String iconBase64;

    /** Timestamp when this app was added to the tracked list. */
    @ColumnInfo(name = "added_timestamp")
    public long addedTimestamp;

    public TrackedApp() {}

    public TrackedApp(String packageName, String appName, int mlAppId) {
        this.packageName      = packageName;
        this.appName          = appName;
        this.mlAppId          = mlAppId;
        this.isMonitored      = true; // Default: monitor when user adds it
        this.addedTimestamp   = System.currentTimeMillis();
    }
}

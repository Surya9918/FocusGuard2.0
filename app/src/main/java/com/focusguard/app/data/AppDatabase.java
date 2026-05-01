package com.focusguard.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * FocusGuard — AppDatabase (Room)
 * ═══════════════════════════════════════════════════════════════════════════
 * Central Room database. Declared as a singleton to prevent multiple
 * instances causing I/O conflicts and resource waste.
 *
 * Entities:
 *   - AppUsageRecord  : One row per detected app session (start time, duration, risk)
 *   - TrackedApp      : User's selected "distraction apps" with their app_id mapping
 *   - DailyStats      : Aggregated per-day totals for the dashboard charts
 *
 * Version history:
 *   v1 → Initial schema. If schema changes in future, add a Migration object.
 *        Never change the schema without incrementing version + adding Migration.
 *        Calling fallbackToDestructiveMigration() would wipe all user data.
 */
@Database(
    entities = {
        AppUsageRecord.class,
        TrackedApp.class,
        DailyStats.class
    },
    version = 1,
    exportSchema = true   // Exports schema JSON to app/schemas/ for migration tracking
)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "focusguard_db";
    private static volatile AppDatabase sInstance;

    // ─── DAO Accessors ────────────────────────────────────────────────────────
    public abstract AppUsageDao    appUsageDao();
    public abstract TrackedAppDao  trackedAppDao();
    public abstract DailyStatsDao  dailyStatsDao();

    // ─── Singleton ────────────────────────────────────────────────────────────
    /**
     * Returns the singleton database instance.
     * Thread-safe via double-checked locking.
     *
     * @param context Use application context to avoid Activity memory leaks.
     */
    public static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                        )
                        // ── IMPORTANT: Never use allowMainThreadQueries() in production. ──
                        // All DB operations must be on background threads (use AsyncTask,
                        // ExecutorService, or LiveData which handles threading automatically).
                        .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // Better concurrent read performance
                        .build();
                }
            }
        }
        return sInstance;
    }
}

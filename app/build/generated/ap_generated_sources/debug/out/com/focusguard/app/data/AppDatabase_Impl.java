package com.focusguard.app.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AppUsageDao _appUsageDao;

  private volatile TrackedAppDao _trackedAppDao;

  private volatile DailyStatsDao _dailyStatsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_usage_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `package_name` TEXT, `app_name` TEXT, `session_start` INTEGER NOT NULL, `session_end` INTEGER NOT NULL, `duration_seconds` INTEGER NOT NULL, `session_date` TEXT, `distraction_risk` REAL NOT NULL, `was_blocked` INTEGER NOT NULL, `ml_app_id` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_usage_records_package_name_session_start` ON `app_usage_records` (`package_name`, `session_start`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_usage_records_session_date` ON `app_usage_records` (`session_date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tracked_apps` (`package_name` TEXT NOT NULL, `app_name` TEXT, `ml_app_id` INTEGER NOT NULL, `is_monitored` INTEGER NOT NULL, `icon_base64` TEXT, `added_timestamp` INTEGER NOT NULL, PRIMARY KEY(`package_name`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `total_screen_seconds` INTEGER NOT NULL, `distraction_seconds` INTEGER NOT NULL, `blocks_triggered` INTEGER NOT NULL, `sessions_count` INTEGER NOT NULL, `top_distraction_package` TEXT, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd6427752a19d6088f2cc714c7f8b50c0')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `app_usage_records`");
        db.execSQL("DROP TABLE IF EXISTS `tracked_apps`");
        db.execSQL("DROP TABLE IF EXISTS `daily_stats`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAppUsageRecords = new HashMap<String, TableInfo.Column>(10);
        _columnsAppUsageRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("package_name", new TableInfo.Column("package_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("app_name", new TableInfo.Column("app_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("session_start", new TableInfo.Column("session_start", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("session_end", new TableInfo.Column("session_end", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("duration_seconds", new TableInfo.Column("duration_seconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("session_date", new TableInfo.Column("session_date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("distraction_risk", new TableInfo.Column("distraction_risk", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("was_blocked", new TableInfo.Column("was_blocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppUsageRecords.put("ml_app_id", new TableInfo.Column("ml_app_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppUsageRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppUsageRecords = new HashSet<TableInfo.Index>(2);
        _indicesAppUsageRecords.add(new TableInfo.Index("index_app_usage_records_package_name_session_start", false, Arrays.asList("package_name", "session_start"), Arrays.asList("ASC", "ASC")));
        _indicesAppUsageRecords.add(new TableInfo.Index("index_app_usage_records_session_date", false, Arrays.asList("session_date"), Arrays.asList("ASC")));
        final TableInfo _infoAppUsageRecords = new TableInfo("app_usage_records", _columnsAppUsageRecords, _foreignKeysAppUsageRecords, _indicesAppUsageRecords);
        final TableInfo _existingAppUsageRecords = TableInfo.read(db, "app_usage_records");
        if (!_infoAppUsageRecords.equals(_existingAppUsageRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "app_usage_records(com.focusguard.app.data.AppUsageRecord).\n"
                  + " Expected:\n" + _infoAppUsageRecords + "\n"
                  + " Found:\n" + _existingAppUsageRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsTrackedApps = new HashMap<String, TableInfo.Column>(6);
        _columnsTrackedApps.put("package_name", new TableInfo.Column("package_name", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackedApps.put("app_name", new TableInfo.Column("app_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackedApps.put("ml_app_id", new TableInfo.Column("ml_app_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackedApps.put("is_monitored", new TableInfo.Column("is_monitored", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackedApps.put("icon_base64", new TableInfo.Column("icon_base64", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackedApps.put("added_timestamp", new TableInfo.Column("added_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrackedApps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrackedApps = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrackedApps = new TableInfo("tracked_apps", _columnsTrackedApps, _foreignKeysTrackedApps, _indicesTrackedApps);
        final TableInfo _existingTrackedApps = TableInfo.read(db, "tracked_apps");
        if (!_infoTrackedApps.equals(_existingTrackedApps)) {
          return new RoomOpenHelper.ValidationResult(false, "tracked_apps(com.focusguard.app.data.TrackedApp).\n"
                  + " Expected:\n" + _infoTrackedApps + "\n"
                  + " Found:\n" + _existingTrackedApps);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyStats = new HashMap<String, TableInfo.Column>(6);
        _columnsDailyStats.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("total_screen_seconds", new TableInfo.Column("total_screen_seconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("distraction_seconds", new TableInfo.Column("distraction_seconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("blocks_triggered", new TableInfo.Column("blocks_triggered", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("sessions_count", new TableInfo.Column("sessions_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("top_distraction_package", new TableInfo.Column("top_distraction_package", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyStats = new TableInfo("daily_stats", _columnsDailyStats, _foreignKeysDailyStats, _indicesDailyStats);
        final TableInfo _existingDailyStats = TableInfo.read(db, "daily_stats");
        if (!_infoDailyStats.equals(_existingDailyStats)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_stats(com.focusguard.app.data.DailyStats).\n"
                  + " Expected:\n" + _infoDailyStats + "\n"
                  + " Found:\n" + _existingDailyStats);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "d6427752a19d6088f2cc714c7f8b50c0", "fc1a797a6968e50ff70063cd391ddb92");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "app_usage_records","tracked_apps","daily_stats");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `app_usage_records`");
      _db.execSQL("DELETE FROM `tracked_apps`");
      _db.execSQL("DELETE FROM `daily_stats`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AppUsageDao.class, AppUsageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TrackedAppDao.class, TrackedAppDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DailyStatsDao.class, DailyStatsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AppUsageDao appUsageDao() {
    if (_appUsageDao != null) {
      return _appUsageDao;
    } else {
      synchronized(this) {
        if(_appUsageDao == null) {
          _appUsageDao = new AppUsageDao_Impl(this);
        }
        return _appUsageDao;
      }
    }
  }

  @Override
  public TrackedAppDao trackedAppDao() {
    if (_trackedAppDao != null) {
      return _trackedAppDao;
    } else {
      synchronized(this) {
        if(_trackedAppDao == null) {
          _trackedAppDao = new TrackedAppDao_Impl(this);
        }
        return _trackedAppDao;
      }
    }
  }

  @Override
  public DailyStatsDao dailyStatsDao() {
    if (_dailyStatsDao != null) {
      return _dailyStatsDao;
    } else {
      synchronized(this) {
        if(_dailyStatsDao == null) {
          _dailyStatsDao = new DailyStatsDao_Impl(this);
        }
        return _dailyStatsDao;
      }
    }
  }
}

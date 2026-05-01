package com.focusguard.app.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppUsageDao_Impl implements AppUsageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AppUsageRecord> __insertionAdapterOfAppUsageRecord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRecordsOlderThan;

  public AppUsageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAppUsageRecord = new EntityInsertionAdapter<AppUsageRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_usage_records` (`id`,`package_name`,`app_name`,`session_start`,`session_end`,`duration_seconds`,`session_date`,`distraction_risk`,`was_blocked`,`ml_app_id`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final AppUsageRecord entity) {
        statement.bindLong(1, entity.id);
        if (entity.packageName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.packageName);
        }
        if (entity.appName == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.appName);
        }
        statement.bindLong(4, entity.sessionStartMs);
        statement.bindLong(5, entity.sessionEndMs);
        statement.bindLong(6, entity.durationSeconds);
        if (entity.sessionDate == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.sessionDate);
        }
        statement.bindDouble(8, entity.distractionRisk);
        final int _tmp = entity.wasBlocked ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindLong(10, entity.mlAppId);
      }
    };
    this.__preparedStmtOfDeleteRecordsOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM app_usage_records WHERE session_date < ?";
        return _query;
      }
    };
  }

  @Override
  public void insertRecord(final AppUsageRecord record) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfAppUsageRecord.insert(record);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteRecordsOlderThan(final String cutoffDate) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRecordsOlderThan.acquire();
    int _argIndex = 1;
    if (cutoffDate == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, cutoffDate);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteRecordsOlderThan.release(_stmt);
    }
  }

  @Override
  public LiveData<Long> getTotalScreenTimeForDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(duration_seconds), 0) FROM app_usage_records WHERE session_date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"app_usage_records"}, false, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<AppUsageRecord>> getRecordsForDate(final String date) {
    final String _sql = "SELECT * FROM app_usage_records WHERE session_date = ? ORDER BY session_start DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"app_usage_records"}, false, new Callable<List<AppUsageRecord>>() {
      @Override
      @Nullable
      public List<AppUsageRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "app_name");
          final int _cursorIndexOfSessionStartMs = CursorUtil.getColumnIndexOrThrow(_cursor, "session_start");
          final int _cursorIndexOfSessionEndMs = CursorUtil.getColumnIndexOrThrow(_cursor, "session_end");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_seconds");
          final int _cursorIndexOfSessionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "session_date");
          final int _cursorIndexOfDistractionRisk = CursorUtil.getColumnIndexOrThrow(_cursor, "distraction_risk");
          final int _cursorIndexOfWasBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "was_blocked");
          final int _cursorIndexOfMlAppId = CursorUtil.getColumnIndexOrThrow(_cursor, "ml_app_id");
          final List<AppUsageRecord> _result = new ArrayList<AppUsageRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppUsageRecord _item;
            _item = new AppUsageRecord();
            _item.id = _cursor.getLong(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _item.packageName = null;
            } else {
              _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _item.appName = null;
            } else {
              _item.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            _item.sessionStartMs = _cursor.getLong(_cursorIndexOfSessionStartMs);
            _item.sessionEndMs = _cursor.getLong(_cursorIndexOfSessionEndMs);
            _item.durationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            if (_cursor.isNull(_cursorIndexOfSessionDate)) {
              _item.sessionDate = null;
            } else {
              _item.sessionDate = _cursor.getString(_cursorIndexOfSessionDate);
            }
            _item.distractionRisk = _cursor.getFloat(_cursorIndexOfDistractionRisk);
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfWasBlocked);
            _item.wasBlocked = _tmp != 0;
            _item.mlAppId = _cursor.getInt(_cursorIndexOfMlAppId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<AppUsageSummary>> getAppUsageSummaryForRange(final String startDate,
      final String endDate) {
    final String _sql = "SELECT package_name AS packageName, app_name AS appName, SUM(duration_seconds) AS totalSeconds FROM app_usage_records WHERE session_date BETWEEN ? AND ? GROUP BY package_name ORDER BY totalSeconds DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, startDate);
    }
    _argIndex = 2;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, endDate);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"app_usage_records"}, false, new Callable<List<AppUsageSummary>>() {
      @Override
      @Nullable
      public List<AppUsageSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = 0;
          final int _cursorIndexOfAppName = 1;
          final int _cursorIndexOfTotalSeconds = 2;
          final List<AppUsageSummary> _result = new ArrayList<AppUsageSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppUsageSummary _item;
            _item = new AppUsageSummary();
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _item.packageName = null;
            } else {
              _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            if (_cursor.isNull(_cursorIndexOfAppName)) {
              _item.appName = null;
            } else {
              _item.appName = _cursor.getString(_cursorIndexOfAppName);
            }
            _item.totalSeconds = _cursor.getLong(_cursorIndexOfTotalSeconds);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public int getBlockCountForDate(final String date) {
    final String _sql = "SELECT COUNT(*) FROM app_usage_records WHERE session_date = ? AND was_blocked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

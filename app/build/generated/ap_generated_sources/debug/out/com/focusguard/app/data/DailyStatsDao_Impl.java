package com.focusguard.app.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class DailyStatsDao_Impl implements DailyStatsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyStats> __insertionAdapterOfDailyStats;

  private final EntityDeletionOrUpdateAdapter<DailyStats> __updateAdapterOfDailyStats;

  public DailyStatsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyStats = new EntityInsertionAdapter<DailyStats>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_stats` (`date`,`total_screen_seconds`,`distraction_seconds`,`blocks_triggered`,`sessions_count`,`top_distraction_package`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final DailyStats entity) {
        if (entity.date == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.date);
        }
        statement.bindLong(2, entity.totalScreenSeconds);
        statement.bindLong(3, entity.distractionSeconds);
        statement.bindLong(4, entity.blocksTriggered);
        statement.bindLong(5, entity.sessionsCount);
        if (entity.topDistractionPackage == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.topDistractionPackage);
        }
      }
    };
    this.__updateAdapterOfDailyStats = new EntityDeletionOrUpdateAdapter<DailyStats>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `daily_stats` SET `date` = ?,`total_screen_seconds` = ?,`distraction_seconds` = ?,`blocks_triggered` = ?,`sessions_count` = ?,`top_distraction_package` = ? WHERE `date` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final DailyStats entity) {
        if (entity.date == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.date);
        }
        statement.bindLong(2, entity.totalScreenSeconds);
        statement.bindLong(3, entity.distractionSeconds);
        statement.bindLong(4, entity.blocksTriggered);
        statement.bindLong(5, entity.sessionsCount);
        if (entity.topDistractionPackage == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.topDistractionPackage);
        }
        if (entity.date == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.date);
        }
      }
    };
  }

  @Override
  public void insertOrReplace(final DailyStats stats) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfDailyStats.insert(stats);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final DailyStats stats) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfDailyStats.handle(stats);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<DailyStats> getStatsLiveDataForDate(final String date) {
    final String _sql = "SELECT * FROM daily_stats WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"daily_stats"}, false, new Callable<DailyStats>() {
      @Override
      @Nullable
      public DailyStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalScreenSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "total_screen_seconds");
          final int _cursorIndexOfDistractionSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "distraction_seconds");
          final int _cursorIndexOfBlocksTriggered = CursorUtil.getColumnIndexOrThrow(_cursor, "blocks_triggered");
          final int _cursorIndexOfSessionsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "sessions_count");
          final int _cursorIndexOfTopDistractionPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "top_distraction_package");
          final DailyStats _result;
          if (_cursor.moveToFirst()) {
            _result = new DailyStats();
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _result.date = null;
            } else {
              _result.date = _cursor.getString(_cursorIndexOfDate);
            }
            _result.totalScreenSeconds = _cursor.getLong(_cursorIndexOfTotalScreenSeconds);
            _result.distractionSeconds = _cursor.getLong(_cursorIndexOfDistractionSeconds);
            _result.blocksTriggered = _cursor.getInt(_cursorIndexOfBlocksTriggered);
            _result.sessionsCount = _cursor.getInt(_cursorIndexOfSessionsCount);
            if (_cursor.isNull(_cursorIndexOfTopDistractionPackage)) {
              _result.topDistractionPackage = null;
            } else {
              _result.topDistractionPackage = _cursor.getString(_cursorIndexOfTopDistractionPackage);
            }
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
  public DailyStats getStatsForDate(final String date) {
    final String _sql = "SELECT * FROM daily_stats WHERE date = ? LIMIT 1";
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
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfTotalScreenSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "total_screen_seconds");
      final int _cursorIndexOfDistractionSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "distraction_seconds");
      final int _cursorIndexOfBlocksTriggered = CursorUtil.getColumnIndexOrThrow(_cursor, "blocks_triggered");
      final int _cursorIndexOfSessionsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "sessions_count");
      final int _cursorIndexOfTopDistractionPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "top_distraction_package");
      final DailyStats _result;
      if (_cursor.moveToFirst()) {
        _result = new DailyStats();
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _result.date = null;
        } else {
          _result.date = _cursor.getString(_cursorIndexOfDate);
        }
        _result.totalScreenSeconds = _cursor.getLong(_cursorIndexOfTotalScreenSeconds);
        _result.distractionSeconds = _cursor.getLong(_cursorIndexOfDistractionSeconds);
        _result.blocksTriggered = _cursor.getInt(_cursorIndexOfBlocksTriggered);
        _result.sessionsCount = _cursor.getInt(_cursorIndexOfSessionsCount);
        if (_cursor.isNull(_cursorIndexOfTopDistractionPackage)) {
          _result.topDistractionPackage = null;
        } else {
          _result.topDistractionPackage = _cursor.getString(_cursorIndexOfTopDistractionPackage);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<DailyStats>> getLast7Days() {
    final String _sql = "SELECT `daily_stats`.`date` AS `date`, `daily_stats`.`total_screen_seconds` AS `total_screen_seconds`, `daily_stats`.`distraction_seconds` AS `distraction_seconds`, `daily_stats`.`blocks_triggered` AS `blocks_triggered`, `daily_stats`.`sessions_count` AS `sessions_count`, `daily_stats`.`top_distraction_package` AS `top_distraction_package` FROM daily_stats ORDER BY date DESC LIMIT 7";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"daily_stats"}, false, new Callable<List<DailyStats>>() {
      @Override
      @Nullable
      public List<DailyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = 0;
          final int _cursorIndexOfTotalScreenSeconds = 1;
          final int _cursorIndexOfDistractionSeconds = 2;
          final int _cursorIndexOfBlocksTriggered = 3;
          final int _cursorIndexOfSessionsCount = 4;
          final int _cursorIndexOfTopDistractionPackage = 5;
          final List<DailyStats> _result = new ArrayList<DailyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStats _item;
            _item = new DailyStats();
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _item.date = null;
            } else {
              _item.date = _cursor.getString(_cursorIndexOfDate);
            }
            _item.totalScreenSeconds = _cursor.getLong(_cursorIndexOfTotalScreenSeconds);
            _item.distractionSeconds = _cursor.getLong(_cursorIndexOfDistractionSeconds);
            _item.blocksTriggered = _cursor.getInt(_cursorIndexOfBlocksTriggered);
            _item.sessionsCount = _cursor.getInt(_cursorIndexOfSessionsCount);
            if (_cursor.isNull(_cursorIndexOfTopDistractionPackage)) {
              _item.topDistractionPackage = null;
            } else {
              _item.topDistractionPackage = _cursor.getString(_cursorIndexOfTopDistractionPackage);
            }
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
  public long getTotalDistractionSecondsForRange(final String startDate, final String endDate) {
    final String _sql = "SELECT COALESCE(SUM(distraction_seconds), 0) FROM daily_stats WHERE date BETWEEN ? AND ?";
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
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final long _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getLong(0);
      } else {
        _result = 0L;
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

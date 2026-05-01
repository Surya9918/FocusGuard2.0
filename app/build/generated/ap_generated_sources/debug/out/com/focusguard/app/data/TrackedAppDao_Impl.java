package com.focusguard.app.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
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
public final class TrackedAppDao_Impl implements TrackedAppDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrackedApp> __insertionAdapterOfTrackedApp;

  private final EntityDeletionOrUpdateAdapter<TrackedApp> __deletionAdapterOfTrackedApp;

  private final EntityDeletionOrUpdateAdapter<TrackedApp> __updateAdapterOfTrackedApp;

  private final SharedSQLiteStatement __preparedStmtOfSetMonitored;

  public TrackedAppDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrackedApp = new EntityInsertionAdapter<TrackedApp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tracked_apps` (`package_name`,`app_name`,`ml_app_id`,`is_monitored`,`icon_base64`,`added_timestamp`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final TrackedApp entity) {
        if (entity.packageName == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.packageName);
        }
        if (entity.appName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.appName);
        }
        statement.bindLong(3, entity.mlAppId);
        final int _tmp = entity.isMonitored ? 1 : 0;
        statement.bindLong(4, _tmp);
        if (entity.iconBase64 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.iconBase64);
        }
        statement.bindLong(6, entity.addedTimestamp);
      }
    };
    this.__deletionAdapterOfTrackedApp = new EntityDeletionOrUpdateAdapter<TrackedApp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `tracked_apps` WHERE `package_name` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final TrackedApp entity) {
        if (entity.packageName == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.packageName);
        }
      }
    };
    this.__updateAdapterOfTrackedApp = new EntityDeletionOrUpdateAdapter<TrackedApp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tracked_apps` SET `package_name` = ?,`app_name` = ?,`ml_app_id` = ?,`is_monitored` = ?,`icon_base64` = ?,`added_timestamp` = ? WHERE `package_name` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final TrackedApp entity) {
        if (entity.packageName == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.packageName);
        }
        if (entity.appName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.appName);
        }
        statement.bindLong(3, entity.mlAppId);
        final int _tmp = entity.isMonitored ? 1 : 0;
        statement.bindLong(4, _tmp);
        if (entity.iconBase64 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.iconBase64);
        }
        statement.bindLong(6, entity.addedTimestamp);
        if (entity.packageName == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.packageName);
        }
      }
    };
    this.__preparedStmtOfSetMonitored = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tracked_apps SET is_monitored = ? WHERE package_name = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertOrUpdate(final TrackedApp app) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTrackedApp.insert(app);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final TrackedApp app) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfTrackedApp.handle(app);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final TrackedApp app) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfTrackedApp.handle(app);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void setMonitored(final String packageName, final boolean isMonitored) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfSetMonitored.acquire();
    int _argIndex = 1;
    final int _tmp = isMonitored ? 1 : 0;
    _stmt.bindLong(_argIndex, _tmp);
    _argIndex = 2;
    if (packageName == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, packageName);
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
      __preparedStmtOfSetMonitored.release(_stmt);
    }
  }

  @Override
  public LiveData<List<TrackedApp>> getAllTrackedApps() {
    final String _sql = "SELECT `tracked_apps`.`package_name` AS `package_name`, `tracked_apps`.`app_name` AS `app_name`, `tracked_apps`.`ml_app_id` AS `ml_app_id`, `tracked_apps`.`is_monitored` AS `is_monitored`, `tracked_apps`.`icon_base64` AS `icon_base64`, `tracked_apps`.`added_timestamp` AS `added_timestamp` FROM tracked_apps ORDER BY app_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"tracked_apps"}, false, new Callable<List<TrackedApp>>() {
      @Override
      @Nullable
      public List<TrackedApp> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = 0;
          final int _cursorIndexOfAppName = 1;
          final int _cursorIndexOfMlAppId = 2;
          final int _cursorIndexOfIsMonitored = 3;
          final int _cursorIndexOfIconBase64 = 4;
          final int _cursorIndexOfAddedTimestamp = 5;
          final List<TrackedApp> _result = new ArrayList<TrackedApp>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackedApp _item;
            _item = new TrackedApp();
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
            _item.mlAppId = _cursor.getInt(_cursorIndexOfMlAppId);
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMonitored);
            _item.isMonitored = _tmp != 0;
            if (_cursor.isNull(_cursorIndexOfIconBase64)) {
              _item.iconBase64 = null;
            } else {
              _item.iconBase64 = _cursor.getString(_cursorIndexOfIconBase64);
            }
            _item.addedTimestamp = _cursor.getLong(_cursorIndexOfAddedTimestamp);
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
  public List<TrackedApp> getMonitoredApps() {
    final String _sql = "SELECT `tracked_apps`.`package_name` AS `package_name`, `tracked_apps`.`app_name` AS `app_name`, `tracked_apps`.`ml_app_id` AS `ml_app_id`, `tracked_apps`.`is_monitored` AS `is_monitored`, `tracked_apps`.`icon_base64` AS `icon_base64`, `tracked_apps`.`added_timestamp` AS `added_timestamp` FROM tracked_apps WHERE is_monitored = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfPackageName = 0;
      final int _cursorIndexOfAppName = 1;
      final int _cursorIndexOfMlAppId = 2;
      final int _cursorIndexOfIsMonitored = 3;
      final int _cursorIndexOfIconBase64 = 4;
      final int _cursorIndexOfAddedTimestamp = 5;
      final List<TrackedApp> _result = new ArrayList<TrackedApp>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final TrackedApp _item;
        _item = new TrackedApp();
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
        _item.mlAppId = _cursor.getInt(_cursorIndexOfMlAppId);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsMonitored);
        _item.isMonitored = _tmp != 0;
        if (_cursor.isNull(_cursorIndexOfIconBase64)) {
          _item.iconBase64 = null;
        } else {
          _item.iconBase64 = _cursor.getString(_cursorIndexOfIconBase64);
        }
        _item.addedTimestamp = _cursor.getLong(_cursorIndexOfAddedTimestamp);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public TrackedApp getByPackageName(final String packageName) {
    final String _sql = "SELECT * FROM tracked_apps WHERE package_name = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (packageName == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, packageName);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
      final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "app_name");
      final int _cursorIndexOfMlAppId = CursorUtil.getColumnIndexOrThrow(_cursor, "ml_app_id");
      final int _cursorIndexOfIsMonitored = CursorUtil.getColumnIndexOrThrow(_cursor, "is_monitored");
      final int _cursorIndexOfIconBase64 = CursorUtil.getColumnIndexOrThrow(_cursor, "icon_base64");
      final int _cursorIndexOfAddedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "added_timestamp");
      final TrackedApp _result;
      if (_cursor.moveToFirst()) {
        _result = new TrackedApp();
        if (_cursor.isNull(_cursorIndexOfPackageName)) {
          _result.packageName = null;
        } else {
          _result.packageName = _cursor.getString(_cursorIndexOfPackageName);
        }
        if (_cursor.isNull(_cursorIndexOfAppName)) {
          _result.appName = null;
        } else {
          _result.appName = _cursor.getString(_cursorIndexOfAppName);
        }
        _result.mlAppId = _cursor.getInt(_cursorIndexOfMlAppId);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsMonitored);
        _result.isMonitored = _tmp != 0;
        if (_cursor.isNull(_cursorIndexOfIconBase64)) {
          _result.iconBase64 = null;
        } else {
          _result.iconBase64 = _cursor.getString(_cursorIndexOfIconBase64);
        }
        _result.addedTimestamp = _cursor.getLong(_cursorIndexOfAddedTimestamp);
      } else {
        _result = null;
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

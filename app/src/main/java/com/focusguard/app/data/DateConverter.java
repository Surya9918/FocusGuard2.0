package com.focusguard.app.data;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * DateConverter — Room TypeConverter
 * ════════════════════════════════════
 * Room cannot store java.util.Date directly in SQLite.
 * This converter tells Room to store Date objects as Long (Unix timestamps in ms),
 * which is the most space-efficient and query-friendly representation.
 */
public class DateConverter {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}

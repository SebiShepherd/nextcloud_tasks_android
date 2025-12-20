package com.nextcloud.tasks.data.database.converter

import androidx.room.TypeConverter
import java.time.Instant

class InstantTypeConverter {
    @TypeConverter
    fun fromEpochMillis(epochMillis: Long?): Instant? {
        return epochMillis?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}

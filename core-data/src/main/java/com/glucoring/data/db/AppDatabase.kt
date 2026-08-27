package com.glucoring.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.glucoring.data.db.dao.CalibrationModelDao
import com.glucoring.data.db.dao.GlucoseReferenceDao
import com.glucoring.data.db.dao.PpgWindowDao
import com.glucoring.data.db.entity.CalibrationModelEntity
import com.glucoring.data.db.entity.GlucoseReferenceEntity
import com.glucoring.data.db.entity.PpgWindowEntity

@Database(
    entities = [PpgWindowEntity::class, GlucoseReferenceEntity::class, CalibrationModelEntity::class],
    version = 1,
    // Schema export is off for this skeleton (no migration history to track
    // yet). Once you start shipping schema changes, set this back to true
    // and point KSP at an export directory via the Room Gradle plugin (see
    // Room's migration-testing docs) so old schemas are versioned for tests.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ppgWindowDao(): PpgWindowDao
    abstract fun glucoseReferenceDao(): GlucoseReferenceDao
    abstract fun calibrationModelDao(): CalibrationModelDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glucoring.db",
                ).build().also { instance = it }
            }
    }
}

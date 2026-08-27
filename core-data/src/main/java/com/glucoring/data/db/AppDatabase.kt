package com.glucoring.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.glucoring.data.db.dao.CalibrationModelDao
import com.glucoring.data.db.dao.GlucoseReferenceDao
import com.glucoring.data.db.dao.PpgWindowDao
import com.glucoring.data.db.dao.UserProfileDao
import com.glucoring.data.db.entity.CalibrationModelEntity
import com.glucoring.data.db.entity.GlucoseReferenceEntity
import com.glucoring.data.db.entity.PpgWindowEntity
import com.glucoring.data.db.entity.UserProfileEntity

@Database(
    entities = [
        PpgWindowEntity::class,
        GlucoseReferenceEntity::class,
        CalibrationModelEntity::class,
        UserProfileEntity::class,
    ],
    version = 2,
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
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glucoring.db",
                )
                    // No real users on version 1 yet in this skeleton — fall
                    // back to destructive migration rather than writing a
                    // migration path for a schema nobody has shipped.
                    // Replace this with a real Migration once the app has
                    // actual installs to preserve data for.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}

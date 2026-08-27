package com.glucoring.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.glucoring.data.db.entity.GlucoseReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseReferenceDao {
    @Insert
    suspend fun insert(reading: GlucoseReferenceEntity): Long

    @Query("SELECT * FROM glucose_reference_readings ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<GlucoseReferenceEntity>>

    @Query("SELECT COUNT(*) FROM glucose_reference_readings")
    suspend fun count(): Int

    /** Spread of reference values — a calibration set that's all near one value produces a useless regression. */
    @Query("SELECT MAX(glucoseMgDl) - MIN(glucoseMgDl) FROM glucose_reference_readings")
    suspend fun valueRangeMgDl(): Int?

    @Query("SELECT * FROM glucose_reference_readings WHERE linkedPpgWindowId IS NOT NULL ORDER BY timestampMs ASC")
    suspend fun allWithLinkedWindow(): List<GlucoseReferenceEntity>
}

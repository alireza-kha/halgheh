package com.glucoring.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.glucoring.data.db.entity.CalibrationModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibrationModelDao {
    @Insert
    suspend fun insert(model: CalibrationModelEntity): Long

    @Query("UPDATE calibration_models SET isActive = 0")
    suspend fun deactivateAll()

    @Query("SELECT * FROM calibration_models WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): CalibrationModelEntity?

    @Query("SELECT * FROM calibration_models WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<CalibrationModelEntity?>

    @Query("SELECT * FROM calibration_models ORDER BY trainedAtMs DESC")
    fun observeHistory(): Flow<List<CalibrationModelEntity>>
}

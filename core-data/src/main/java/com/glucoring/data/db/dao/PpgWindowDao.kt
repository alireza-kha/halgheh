package com.glucoring.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.glucoring.data.db.entity.PpgWindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PpgWindowDao {
    @Insert
    suspend fun insert(window: PpgWindowEntity): Long

    @Query("SELECT * FROM ppg_windows WHERE timestampMs BETWEEN :fromMs AND :toMs ORDER BY timestampMs ASC")
    fun observeBetween(fromMs: Long, toMs: Long): Flow<List<PpgWindowEntity>>

    @Query("""
        SELECT * FROM ppg_windows
        ORDER BY ABS(timestampMs - :targetTimestampMs) ASC
        LIMIT 1
    """)
    suspend fun findClosestTo(targetTimestampMs: Long): PpgWindowEntity?

    @Query("SELECT * FROM ppg_windows ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<PpgWindowEntity>>
}

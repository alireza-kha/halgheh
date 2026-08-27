package com.glucoring.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A manual fingerstick reading the user entered — this is ground truth used
 * both to calibrate the personal model and to show the user how their model
 * is tracking. `linkedPpgWindowId` ties it to the nearest PPG feature window
 * in time (set by the repository at insert time) so the calibrator has a
 * matching (features, label) pair.
 */
@Entity(tableName = "glucose_reference_readings")
data class GlucoseReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val glucoseMgDl: Int,
    val context: String, // e.g. "fasting", "post_meal", "before_bed", "other"
    val note: String? = null,
    val linkedPpgWindowId: Long? = null,
)

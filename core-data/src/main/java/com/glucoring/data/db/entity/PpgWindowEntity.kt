package com.glucoring.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One extracted feature-vector window (typically ~20-60s of PPG averaged into
 * morphology features by core-signal) — NOT the raw waveform itself, to keep
 * the local DB small. If you want to keep raw samples too for later
 * re-processing, add a separate RawPpgFrameEntity + a foreign key here.
 */
@Entity(tableName = "ppg_windows")
data class PpgWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    /** JSON-encoded FeatureVector from core-signal (kept opaque here to avoid a hard dependency edge). */
    val featureVectorJson: String,
    val heartRateBpm: Int?,
    val spo2Percent: Int?,
    val motionArtifactScore: Float,
)

package com.glucoring.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A trained personal-calibration snapshot (see core-ml/RidgeRegressionCalibrator).
 * We keep a history of models rather than overwriting, so the app can show
 * "your model was last retrained on ... using N calibration points, MARD ~X%"
 * and roll back if a retrain makes things worse.
 */
@Entity(tableName = "calibration_models")
data class CalibrationModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainedAtMs: Long,
    val weightsJson: String,
    val calibrationPointCount: Int,
    val estimatedMardPercent: Float?,
    val isActive: Boolean,
)

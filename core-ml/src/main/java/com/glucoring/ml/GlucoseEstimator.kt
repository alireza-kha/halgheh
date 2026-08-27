package com.glucoring.ml

import com.glucoring.data.db.entity.CalibrationModelEntity
import com.glucoring.data.repository.GlucoRepository
import com.glucoring.signal.FeatureVector

/**
 * Facade the app layer talks to: retrain from the repository's calibration
 * dataset, persist the result, and produce estimates gated by model quality.
 */
class GlucoseEstimator(private val repository: GlucoRepository) {

    private var activeModel: CalibrationModel? = null

    suspend fun loadActiveModel() {
        val entity = repository.getActiveModel() ?: return
        activeModel = RidgeRegressionCalibrator.fromSerialized(entity.weightsJson)
    }

    /** Returns null if there isn't enough / varied enough calibration data to train responsibly. */
    suspend fun retrain(): RetrainResult {
        val readiness = repository.calibrationReadiness()
        if (!readiness.isReady) return RetrainResult.NotEnoughData(readiness)

        val dataset = repository.buildCalibrationDataset()
        if (dataset.size < 3) return RetrainResult.NotEnoughData(readiness)

        val pairs = dataset.map { it.features to it.glucoseMgDl }
        val mard = ModelQuality.leaveOneOutMard(pairs)

        val model = RidgeRegressionCalibrator.train(pairs)
        activeModel = model

        val entity = CalibrationModelEntity(
            trainedAtMs = System.currentTimeMillis(),
            weightsJson = model.serializeWeights(),
            calibrationPointCount = dataset.size,
            estimatedMardPercent = mard,
            isActive = true,
        )
        repository.saveNewActiveModel(entity)

        return RetrainResult.Success(
            calibrationPointCount = dataset.size,
            mardPercent = mard,
            quality = ModelQuality.qualityLabel(mard),
        )
    }

    /** Null means "no trained model yet" — the UI should show a calibration prompt, not a fabricated number. */
    fun estimate(features: FeatureVector): Estimate? {
        val model = activeModel ?: return null
        return Estimate(mgDl = model.predictMgDl(features), features = features)
    }
}

sealed interface RetrainResult {
    data class Success(val calibrationPointCount: Int, val mardPercent: Float?, val quality: QualityLabel) : RetrainResult
    data class NotEnoughData(val readiness: com.glucoring.data.repository.CalibrationReadiness) : RetrainResult
}

data class Estimate(val mgDl: Double, val features: FeatureVector)

package com.glucoring.ml

/**
 * MARD (Mean Absolute Relative Difference) is the standard accuracy metric
 * used to report CGM/glucose-meter accuracy in clinical literature — e.g.
 * "MARD 9%" for a good clinical CGM. Consumer-grade optical estimates should
 * be expected to land far worse than that; showing this number to the user
 * (and refusing to show a live estimate when it's bad) is the honest way to
 * present a model like this.
 */
object ModelQuality {

    fun computeMardPercent(predictions: List<Double>, actuals: List<Int>): Float {
        require(predictions.size == actuals.size && predictions.isNotEmpty())
        val relDiffs = predictions.indices.map { i ->
            kotlin.math.abs(predictions[i] - actuals[i]) / actuals[i]
        }
        return (relDiffs.average() * 100).toFloat()
    }

    /**
     * Leave-one-out cross-validated MARD — trains on all-but-one calibration
     * point and predicts the held-out one, repeated for every point. Far more
     * honest than reporting training-set error, which a ridge regression with
     * few points will always make look artificially good.
     */
    fun leaveOneOutMard(points: List<Pair<com.glucoring.signal.FeatureVector, Int>>): Float? {
        if (points.size < 4) return null // not enough points for a meaningful holdout
        val preds = mutableListOf<Double>()
        val actuals = mutableListOf<Int>()
        for (i in points.indices) {
            val trainSet = points.filterIndexed { idx, _ -> idx != i }
            val model = runCatching { RidgeRegressionCalibrator.train(trainSet) }.getOrNull() ?: continue
            preds.add(model.predictMgDl(points[i].first))
            actuals.add(points[i].second)
        }
        if (preds.isEmpty()) return null
        return computeMardPercent(preds, actuals)
    }

    /** Rough, deliberately conservative bands for what to tell the user about a model's trustworthiness. */
    fun qualityLabel(mardPercent: Float?): QualityLabel = when {
        mardPercent == null -> QualityLabel.INSUFFICIENT_DATA
        mardPercent <= 15f -> QualityLabel.USABLE_WITH_CAUTION
        mardPercent <= 25f -> QualityLabel.LOW_CONFIDENCE
        else -> QualityLabel.UNRELIABLE
    }
}

enum class QualityLabel { INSUFFICIENT_DATA, USABLE_WITH_CAUTION, LOW_CONFIDENCE, UNRELIABLE }

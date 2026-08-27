package com.glucoring.ml

import com.glucoring.signal.FeatureVector

/** A trained per-user model that maps a PPG [FeatureVector] to an estimated glucose value. */
interface CalibrationModel {
    fun predictMgDl(features: FeatureVector): Double
    fun serializeWeights(): String
}

package com.glucoring.signal

import org.json.JSONObject

/**
 * The feature set extracted from one PPG window. These are the inputs the
 * personal calibration model (core-ml) regresses against fingerstick glucose.
 *
 * IMPORTANT — this list is a reasonable *starting point* drawn from the
 * general PPG/optical-glucose research literature (perfusion index, pulse
 * morphology, HRV context), not a validated clinical feature set. Expect to
 * revisit this once you have real calibration data to see which features
 * actually correlate for a given wearer.
 */
data class FeatureVector(
    val meanGreen: Double,          // DC component
    val amplitudeGreen: Double,     // AC component (peak-to-peak of the detrended pulse)
    val perfusionIndex: Double,     // AC/DC — proxy for capillary blood volume change
    val pulseRateBpm: Double,
    val pulseIntervalStdMs: Double, // short-term HRV-ish jitter, can proxy autonomic/vascular state
    val riseFallRatio: Double,      // average pulse rise time / fall time — waveform shape
    val skewness: Double,
    val motionArtifactScore: Float, // 0 = still, higher = more accelerometer variance during the window
    val ambientHeartRateBpm: Int?,  // from the device's own HR estimate, as a sanity cross-check
) {
    fun toJson(): String = JSONObject().apply {
        put("meanGreen", meanGreen)
        put("amplitudeGreen", amplitudeGreen)
        put("perfusionIndex", perfusionIndex)
        put("pulseRateBpm", pulseRateBpm)
        put("pulseIntervalStdMs", pulseIntervalStdMs)
        put("riseFallRatio", riseFallRatio)
        put("skewness", skewness)
        put("motionArtifactScore", motionArtifactScore.toDouble())
        put("ambientHeartRateBpm", ambientHeartRateBpm ?: JSONObject.NULL)
    }.toString()

    /** Order matters here — this is the exact input vector core-ml trains and predicts on. */
    fun toDoubleArray(): DoubleArray = doubleArrayOf(
        meanGreen, amplitudeGreen, perfusionIndex, pulseRateBpm,
        pulseIntervalStdMs, riseFallRatio, skewness, motionArtifactScore.toDouble(),
    )

    companion object {
        val FEATURE_NAMES = listOf(
            "meanGreen", "amplitudeGreen", "perfusionIndex", "pulseRateBpm",
            "pulseIntervalStdMs", "riseFallRatio", "skewness", "motionArtifactScore",
        )

        fun fromJson(json: String): FeatureVector {
            val o = JSONObject(json)
            return FeatureVector(
                meanGreen = o.getDouble("meanGreen"),
                amplitudeGreen = o.getDouble("amplitudeGreen"),
                perfusionIndex = o.getDouble("perfusionIndex"),
                pulseRateBpm = o.getDouble("pulseRateBpm"),
                pulseIntervalStdMs = o.getDouble("pulseIntervalStdMs"),
                riseFallRatio = o.getDouble("riseFallRatio"),
                skewness = o.getDouble("skewness"),
                motionArtifactScore = o.getDouble("motionArtifactScore").toFloat(),
                ambientHeartRateBpm = if (o.isNull("ambientHeartRateBpm")) null else o.getInt("ambientHeartRateBpm"),
            )
        }
    }
}

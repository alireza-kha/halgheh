package com.glucoring.signal

import com.glucoring.ble.model.PpgSample
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Concatenates a rolling window of raw PPG frames (accumulated over
 * [windowDurationMs], default ~30s) and turns them into one [FeatureVector].
 * Feed it frames as they arrive; call [tryExtract] periodically (e.g. once a
 * second) — it returns a feature vector once enough data has accumulated,
 * and null otherwise.
 */
class PpgFeatureExtractor(
    private val windowDurationMs: Long = 30_000,
    private val assumedSampleRateHz: Double = 25.0, // set from the vendor SDK's actual frame rate once known
) {
    private val buffer = mutableListOf<PpgSample>()

    fun addFrame(sample: PpgSample) {
        buffer.add(sample)
        val cutoff = System.currentTimeMillis() - windowDurationMs
        buffer.removeAll { it.timestampMs < cutoff }
    }

    fun tryExtract(): FeatureVector? {
        if (buffer.isEmpty()) return null
        val totalSamples = buffer.sumOf { it.rawGreen.size }
        // Require a reasonably full window before trusting the features.
        if (totalSamples < (assumedSampleRateHz * windowDurationMs / 1000.0 * 0.6)) return null

        val raw = buffer.flatMap { it.rawGreen.toList() }.toIntArray()
        val smoothed = SignalFilters.movingAverage(raw, windowSize = 5)
        val detrended = SignalFilters.detrend(smoothed)

        val mean = raw.average()
        val amplitude = (detrended.maxOrNull() ?: 0.0) - (detrended.minOrNull() ?: 0.0)
        val perfusionIndex = if (mean != 0.0) amplitude / mean else 0.0

        val peaks = SignalFilters.findPeaks(detrended, minDistance = (assumedSampleRateHz * 0.35).toInt().coerceAtLeast(3))
        val sampleIntervalMs = 1000.0 / assumedSampleRateHz
        val peakIntervalsMs = peaks.zipWithNext { a, b -> (b - a) * sampleIntervalMs }
        val pulseRateBpm = if (peakIntervalsMs.isNotEmpty()) 60_000.0 / peakIntervalsMs.average() else 0.0
        val pulseIntervalStdMs = stdDev(peakIntervalsMs)

        val riseFallRatio = averageRiseFallRatio(detrended, peaks)
        val skew = skewness(detrended)

        val motionScore = motionArtifactScore()
        val avgHr = buffer.mapNotNull { it.heartRateBpm }.takeIf { it.isNotEmpty() }?.average()?.toInt()

        return FeatureVector(
            meanGreen = mean,
            amplitudeGreen = amplitude,
            perfusionIndex = perfusionIndex,
            pulseRateBpm = pulseRateBpm,
            pulseIntervalStdMs = pulseIntervalStdMs,
            riseFallRatio = riseFallRatio,
            skewness = skew,
            motionArtifactScore = motionScore,
            ambientHeartRateBpm = avgHr,
        )
    }

    fun clear() = buffer.clear()

    private fun motionArtifactScore(): Float {
        val allAccel = buffer.flatMap { s ->
            (s.accelX?.toList() ?: emptyList()) + (s.accelY?.toList() ?: emptyList()) + (s.accelZ?.toList() ?: emptyList())
        }
        if (allAccel.isEmpty()) return 0f
        return stdDev(allAccel.map { it.toDouble() }).toFloat()
    }

    private fun averageRiseFallRatio(signal: DoubleArray, peaks: List<Int>): Double {
        if (peaks.size < 2) return 1.0
        val ratios = mutableListOf<Double>()
        for (k in 0 until peaks.size - 1) {
            val peak = peaks[k]
            val nextPeak = peaks[k + 1]
            // Approximate the trough between two peaks as the local minimum.
            var trough = peak
            for (idx in peak until nextPeak) if (signal[idx] < signal[trough]) trough = idx
            val riseSamples = (nextPeak - trough).coerceAtLeast(1)
            val fallSamples = (trough - peak).let { if (it <= 0) 1 else it }
            ratios.add(riseSamples.toDouble() / fallSamples.toDouble())
        }
        return if (ratios.isEmpty()) 1.0 else ratios.average()
    }

    private fun stdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean).pow(2) } / (values.size - 1)
        return sqrt(variance)
    }

    private fun skewness(values: DoubleArray): Double {
        if (values.size < 3) return 0.0
        val mean = values.average()
        val sd = sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
        if (sd == 0.0) return 0.0
        val m3 = values.sumOf { (it - mean).pow(3) } / values.size
        return m3 / sd.pow(3)
    }
}

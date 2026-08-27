package com.glucoring.signal

/** Small collection of dependency-free filters for raw PPG samples. */
object SignalFilters {

    /** Simple moving-average low-pass filter to knock down high-frequency sensor noise. */
    fun movingAverage(samples: IntArray, windowSize: Int = 5): DoubleArray {
        if (samples.isEmpty()) return DoubleArray(0)
        val out = DoubleArray(samples.size)
        var sum = 0.0
        val half = windowSize / 2
        for (i in samples.indices) {
            val lo = (i - half).coerceAtLeast(0)
            val hi = (i + half).coerceAtMost(samples.size - 1)
            sum = 0.0
            for (j in lo..hi) sum += samples[j]
            out[i] = sum / (hi - lo + 1)
        }
        return out
    }

    /** Removes the slow DC drift (baseline wander) so pulse features aren't skewed by e.g. sensor pressure changes. */
    fun detrend(samples: DoubleArray, baselineWindow: Int = 51): DoubleArray {
        if (samples.isEmpty()) return samples
        val baseline = DoubleArray(samples.size)
        val half = baselineWindow / 2
        for (i in samples.indices) {
            val lo = (i - half).coerceAtLeast(0)
            val hi = (i + half).coerceAtMost(samples.size - 1)
            var sum = 0.0
            for (j in lo..hi) sum += samples[j]
            baseline[i] = sum / (hi - lo + 1)
        }
        return DoubleArray(samples.size) { samples[it] - baseline[it] }
    }

    /**
     * Very small peak detector for pulse-interval features. Not a substitute for
     * a proper Pan-Tompkins-style detector — good enough for feature extraction
     * on a clean, pre-filtered signal, but revisit if real-world traces are noisy.
     */
    fun findPeaks(samples: DoubleArray, minDistance: Int = 10): List<Int> {
        val peaks = mutableListOf<Int>()
        var i = 1
        while (i < samples.size - 1) {
            if (samples[i] > samples[i - 1] && samples[i] >= samples[i + 1]) {
                if (peaks.isEmpty() || i - peaks.last() >= minDistance) {
                    peaks.add(i)
                }
            }
            i++
        }
        return peaks
    }
}

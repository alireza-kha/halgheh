package com.glucoring.ml

import com.glucoring.signal.FeatureVector
import org.json.JSONArray
import org.json.JSONObject

/**
 * Personal (per-user) ridge-regression calibrator.
 *
 * Why ridge regression and not something fancier: with realistically a
 * handful to a few dozen fingerstick calibration points per user, a small
 * L2-regularized linear model is far less likely to overfit than any
 * nonlinear model, and it stays interpretable (you can inspect the learned
 * weights). This is also standard practice in the academic PPG-glucose
 * literature for the same reason — models are personalized per subject
 * because the PPG→glucose relationship is not stable across people (or even
 * for one person over weeks), not because personalization is a nice-to-have.
 *
 * Features are z-score standardized before fitting so ridge regularization
 * penalizes each feature comparably regardless of its raw scale.
 */
class RidgeRegressionCalibrator private constructor(
    private val weights: DoubleArray,     // one per standardized feature
    private val intercept: Double,
    private val featureMeans: DoubleArray,
    private val featureStdDevs: DoubleArray,
) : CalibrationModel {

    override fun predictMgDl(features: FeatureVector): Double {
        val x = features.toDoubleArray()
        var sum = intercept
        for (i in x.indices) {
            val z = if (featureStdDevs[i] > 1e-9) (x[i] - featureMeans[i]) / featureStdDevs[i] else 0.0
            sum += weights[i] * z
        }
        return sum
    }

    override fun serializeWeights(): String = JSONObject().apply {
        put("weights", JSONArray(weights.toList()))
        put("intercept", intercept)
        put("featureMeans", JSONArray(featureMeans.toList()))
        put("featureStdDevs", JSONArray(featureStdDevs.toList()))
        put("featureNames", JSONArray(FeatureVector.FEATURE_NAMES))
    }.toString()

    companion object {
        /** L2 regularization strength — higher = more conservative fit, safer with very few calibration points. */
        private const val DEFAULT_LAMBDA = 1.0

        fun train(
            points: List<Pair<FeatureVector, Int>>, // (features, glucoseMgDl)
            lambda: Double = DEFAULT_LAMBDA,
        ): RidgeRegressionCalibrator {
            require(points.size >= 3) { "Need at least 3 calibration points to fit a model." }

            val n = points.size
            val d = FeatureVector.FEATURE_NAMES.size
            val rawX = Array(n) { points[it].first.toDoubleArray() }
            val y = DoubleArray(n) { points[it].second.toDouble() }

            val means = DoubleArray(d)
            val stds = DoubleArray(d)
            for (j in 0 until d) {
                val col = DoubleArray(n) { rawX[it][j] }
                means[j] = col.average()
                val variance = col.sumOf { (it - means[j]) * (it - means[j]) } / n
                stds[j] = kotlin.math.sqrt(variance)
            }
            val zX = Array(n) { i -> DoubleArray(d) { j -> if (stds[j] > 1e-9) (rawX[i][j] - means[j]) / stds[j] else 0.0 } }

            // Solve ridge regression via normal equations: w = (Z^T Z + lambda*I)^-1 Z^T y
            // Intercept handled separately as the mean of y (features are standardized to mean 0).
            val yMean = y.average()
            val yCentered = DoubleArray(n) { y[it] - yMean }

            val zt = transpose(zX)                 // d x n
            val ztz = matMul(zt, zX)                // d x d
            for (i in 0 until d) ztz[i][i] += lambda
            val zty = matVecMul(zt, yCentered)      // d

            val w = solveLinearSystem(ztz, zty)

            return RidgeRegressionCalibrator(
                weights = w,
                intercept = yMean,
                featureMeans = means,
                featureStdDevs = stds,
            )
        }

        fun fromSerialized(json: String): RidgeRegressionCalibrator {
            val o = JSONObject(json)
            fun arr(key: String) = o.getJSONArray(key).let { a -> DoubleArray(a.length()) { a.getDouble(it) } }
            return RidgeRegressionCalibrator(
                weights = arr("weights"),
                intercept = o.getDouble("intercept"),
                featureMeans = arr("featureMeans"),
                featureStdDevs = arr("featureStdDevs"),
            )
        }

        // ---- tiny dependency-free linear algebra helpers ----

        private fun transpose(m: Array<DoubleArray>): Array<DoubleArray> {
            val rows = m.size; val cols = m[0].size
            return Array(cols) { j -> DoubleArray(rows) { i -> m[i][j] } }
        }

        private fun matMul(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
            val n = a.size; val k = b.size; val m = b[0].size
            val result = Array(n) { DoubleArray(m) }
            for (i in 0 until n) for (l in 0 until k) {
                val aVal = a[i][l]
                if (aVal == 0.0) continue
                for (j in 0 until m) result[i][j] += aVal * b[l][j]
            }
            return result
        }

        private fun matVecMul(a: Array<DoubleArray>, v: DoubleArray): DoubleArray {
            val n = a.size
            return DoubleArray(n) { i -> a[i].indices.sumOf { j -> a[i][j] * v[j] } }
        }

        /** Gauss-Jordan elimination with partial pivoting. `a` is square (d x d), `b` is length d. */
        private fun solveLinearSystem(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val n = a.size
            // augmented matrix
            val m = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }

            for (col in 0 until n) {
                var pivotRow = col
                for (row in col + 1 until n) {
                    if (kotlin.math.abs(m[row][col]) > kotlin.math.abs(m[pivotRow][col])) pivotRow = row
                }
                val tmp = m[col]; m[col] = m[pivotRow]; m[pivotRow] = tmp

                val pivot = m[col][col]
                if (kotlin.math.abs(pivot) < 1e-12) continue // singular-ish direction; leave weight near 0
                for (j in col..n) m[col][j] /= pivot

                for (row in 0 until n) {
                    if (row == col) continue
                    val factor = m[row][col]
                    if (factor == 0.0) continue
                    for (j in col..n) m[row][j] -= factor * m[col][j]
                }
            }
            return DoubleArray(n) { m[it][n] }
        }
    }
}

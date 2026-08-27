package com.glucoring.data.repository

import com.glucoring.data.db.AppDatabase
import com.glucoring.data.db.entity.CalibrationModelEntity
import com.glucoring.data.db.entity.GlucoseReferenceEntity
import com.glucoring.data.db.entity.PpgWindowEntity
import com.glucoring.data.db.entity.UserProfileEntity
import com.glucoring.signal.FeatureVector
import kotlinx.coroutines.flow.Flow

/** One (features, label) pair ready for the calibrator in core-ml. */
data class CalibrationDataPoint(val features: FeatureVector, val glucoseMgDl: Int, val timestampMs: Long)

class GlucoRepository(private val db: AppDatabase) {

    // ---- PPG windows -------------------------------------------------

    suspend fun logPpgWindow(
        timestampMs: Long,
        features: FeatureVector,
        heartRateBpm: Int?,
        spo2Percent: Int?,
    ): Long = db.ppgWindowDao().insert(
        PpgWindowEntity(
            timestampMs = timestampMs,
            featureVectorJson = features.toJson(),
            heartRateBpm = heartRateBpm,
            spo2Percent = spo2Percent,
            motionArtifactScore = features.motionArtifactScore,
        )
    )

    fun observeRecentWindows(limit: Int = 500): Flow<List<PpgWindowEntity>> =
        db.ppgWindowDao().observeRecent(limit)

    // ---- Reference (fingerstick) readings -----------------------------

    /** Links the reading to the nearest logged PPG window so it becomes a usable calibration point. */
    suspend fun logGlucoseReference(
        timestampMs: Long,
        glucoseMgDl: Int,
        context: String,
        note: String? = null,
    ): Long {
        val closestWindow = db.ppgWindowDao().findClosestTo(timestampMs)
        // A fingerstick reading taken minutes away from any PPG window isn't a
        // trustworthy calibration point — only link within a tight tolerance.
        val maxLinkGapMs = 5 * 60_000L
        val linkedId = closestWindow?.takeIf { kotlin.math.abs(it.timestampMs - timestampMs) <= maxLinkGapMs }?.id

        return db.glucoseReferenceDao().insert(
            GlucoseReferenceEntity(
                timestampMs = timestampMs,
                glucoseMgDl = glucoseMgDl,
                context = context,
                note = note,
                linkedPpgWindowId = linkedId,
            )
        )
    }

    fun observeReferenceReadings(): Flow<List<GlucoseReferenceEntity>> =
        db.glucoseReferenceDao().observeAll()

    /** Everything the calibrator needs: how many points exist and how spread out their values are. */
    suspend fun calibrationReadiness(minPoints: Int = 6, minSpreadMgDl: Int = 40): CalibrationReadiness {
        val count = db.glucoseReferenceDao().count()
        val spread = db.glucoseReferenceDao().valueRangeMgDl() ?: 0
        return CalibrationReadiness(
            pointCount = count,
            valueSpreadMgDl = spread,
            hasEnoughPoints = count >= minPoints,
            hasEnoughSpread = spread >= minSpreadMgDl,
        )
    }

    suspend fun buildCalibrationDataset(): List<CalibrationDataPoint> {
        val refs = db.glucoseReferenceDao().allWithLinkedWindow()
        val windowDao = db.ppgWindowDao()
        return refs.mapNotNull { ref ->
            val windowId = ref.linkedPpgWindowId ?: return@mapNotNull null
            // NOTE: for a real app add a `findById` query; reusing findClosestTo
            // here as a stand-in keeps this skeleton short.
            val window = windowDao.findClosestTo(ref.timestampMs) ?: return@mapNotNull null
            if (window.id != windowId) return@mapNotNull null
            CalibrationDataPoint(
                features = FeatureVector.fromJson(window.featureVectorJson),
                glucoseMgDl = ref.glucoseMgDl,
                timestampMs = ref.timestampMs,
            )
        }
    }

    // ---- Calibration models --------------------------------------------

    suspend fun saveNewActiveModel(model: CalibrationModelEntity) {
        db.calibrationModelDao().deactivateAll()
        db.calibrationModelDao().insert(model.copy(isActive = true))
    }

    suspend fun getActiveModel(): CalibrationModelEntity? = db.calibrationModelDao().getActive()

    fun observeActiveModel(): Flow<CalibrationModelEntity?> = db.calibrationModelDao().observeActive()

    fun observeModelHistory(): Flow<List<CalibrationModelEntity>> = db.calibrationModelDao().observeHistory()

    // ---- User profile ---------------------------------------------------

    suspend fun getProfile(): UserProfileEntity = db.userProfileDao().get() ?: UserProfileEntity.default()

    fun observeProfile(): Flow<UserProfileEntity?> = db.userProfileDao().observe()

    suspend fun saveProfile(profile: UserProfileEntity) = db.userProfileDao().upsert(profile)

    /** Called whenever a successful connection happens, so Profile can show/offer to disconnect the right device. */
    suspend fun rememberPairedDevice(name: String?, mac: String?) {
        // Upsert-safe even if no profile row exists yet.
        val current = db.userProfileDao().get() ?: UserProfileEntity.default()
        db.userProfileDao().upsert(current.copy(pairedDeviceName = name, pairedDeviceMac = mac))
    }
}

data class CalibrationReadiness(
    val pointCount: Int,
    val valueSpreadMgDl: Int,
    val hasEnoughPoints: Boolean,
    val hasEnoughSpread: Boolean,
) {
    val isReady: Boolean get() = hasEnoughPoints && hasEnoughSpread
}

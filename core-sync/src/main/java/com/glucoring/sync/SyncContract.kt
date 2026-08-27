package com.glucoring.sync

import com.glucoring.signal.FeatureVector

/**
 * Contract for the future "central pool for population ML" feature you
 * described. Intentionally NOT implemented yet (you chose local-only storage
 * for now) — this just fixes the shape so wiring in a real backend later is a
 * matter of writing one class, not restructuring the app.
 *
 * A record uploaded here is (features, glucoseMgDl) pairs — never raw PPG
 * waveforms, never anything else from the device — plus a pseudonymous
 * per-install ID, never anything tying it back to a real identity. This
 * matters both ethically and legally: aggregated glucose + biometric feature
 * data is sensitive health data, and centralizing it requires explicit,
 * informed, revocable user consent (and almost certainly a privacy policy,
 * encryption in transit and at rest, and — depending on your jurisdiction and
 * users — regulatory review, e.g. as a medical-adjacent data processor)
 * before a single record leaves the device. Build the consent screen before
 * you build the backend.
 */
interface SyncClient {
    suspend fun uploadCalibrationRecord(record: SyncRecord): Result<Unit>
    suspend fun downloadPopulationModelUpdate(): Result<ByteArray?>
}

data class SyncRecord(
    val pseudonymousInstallId: String,
    val features: FeatureVector,
    val glucoseMgDl: Int,
    val recordedAtMs: Long,
    val consentVersion: String,
)

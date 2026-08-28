package com.glucoring.ble.internal

import android.util.Log
import com.glucoring.ble.model.PpgSample
import com.glucoring.ble.model.VitalsSample
import com.jstyle.blesdk2301.callback.DataListener2301

/**
 * Bridges the vendor SDK's `DataListener2301` (Java, Map-based) callback into
 * typed Kotlin events.
 *
 * Verified by decompiling `BleSDK.DataParsingWithData` (CFR) rather than
 * guessing from doc text or DeviceKey constant names — the earlier version of
 * this file had two real bugs found that way:
 *
 * 1. For the 0x28 (40) health-measurement response (the one
 *    `startVitalsAutoMeasurement` triggers), the vendor SDK does NOT put
 *    `heartRate` / `Blood_oxygen` / `hrv` / `stress` / `highPressure` /
 *    `lowPressure` at the top level of the callback map. They're nested one
 *    level deeper, under a `"dicData"` key, as a `Map<String, String>` (yes,
 *    String values, e.g. `"73"` — not numbers). Reading them from the top
 *    level as `Number` (what this file used to do) silently always returned
 *    null.
 *
 * 2. Raw PPG waveform data (`arrayPpgRawData`) IS implemented in the SDK's
 *    internal `ResolveUtil.getPPG()`, but `DataParsingWithData`'s dispatch
 *    switch never routes any frame to it — there is no reachable code path
 *    in this SDK build that ever produces `arrayPpgRawData`. This isn't an
 *    undocumented-but-working feature; it's dead code in this jar. The
 *    glucose-estimation pipeline that depends on it cannot function until
 *    the vendor ships an SDK build that actually wires this up. The
 *    PpgSample/onPpgFrame path below is kept as-is (harmless — it will just
 *    never fire) so the rest of the app doesn't need to change once a
 *    working SDK build is available.
 */
internal class SdkDataListener(
    private val onPpgFrame: (PpgSample) -> Unit,
    private val onVitals: (VitalsSample) -> Unit,
) : DataListener2301 {

    companion object {
        private const val TAG = "SdkDataListener"
    }

    override fun dataCallback(data: Map<String, Any>) {
        Log.d(TAG, "dataCallback(Map) dataType=${data["dataType"]} keys=${data.keys}")

        val dicData = data["dicData"] as? Map<*, *>
        if (dicData != null) {
            Log.d(TAG, "  dicData=$dicData")
        }

        val now = System.currentTimeMillis()

        // Raw PPG waveform: see kdoc above — this key is never actually
        // produced by this SDK build, so this branch is effectively dead
        // until a working vendor SDK is available. Left in place on purpose.
        val rawGreen = (data["arrayPpgRawData"] as? IntArray)
            ?: (data["arrayPpgRawData"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toIntArray()
        if (rawGreen != null) {
            onPpgFrame(
                PpgSample(
                    timestampMs = now,
                    rawGreen = rawGreen,
                    heartRateBpm = dicData?.stringField("heartRate"),
                    spo2Percent = dicData?.stringField("Blood_oxygen"),
                    accelX = toIntArrayOrNull(data["arrayX"]),
                    accelY = toIntArrayOrNull(data["arrayY"]),
                    accelZ = toIntArrayOrNull(data["arrayZ"]),
                )
            )
        }

        if (dicData != null) {
            onVitals(
                VitalsSample(
                    timestampMs = now,
                    heartRateBpm = dicData.stringField("heartRate"),
                    spo2Percent = dicData.stringField("Blood_oxygen"),
                    systolic = dicData.stringField("highPressure"),
                    diastolic = dicData.stringField("lowPressure"),
                    hrv = dicData.stringField("hrv"),
                    stress = dicData.stringField("stress"),
                )
            )
        } else {
            Log.d(TAG, "dataCallback(Map): no dicData in this frame (dataType=${data["dataType"]}) — not a vitals-measurement response")
        }
    }

    override fun dataCallback(raw: ByteArray) {
        Log.d(TAG, "dataCallback(byte[]): [${raw.joinToString(" ") { "%02x".format(it) }}]")
    }

    /** dicData's values are Strings (e.g. "73"), confirmed from the decompiled ResolveUtil helpers. */
    private fun Map<*, *>.stringField(key: String): Int? = (this[key] as? String)?.toIntOrNull()

    private fun toIntArrayOrNull(value: Any?): IntArray? = when (value) {
        is IntArray -> value
        is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
        else -> null
    }
}

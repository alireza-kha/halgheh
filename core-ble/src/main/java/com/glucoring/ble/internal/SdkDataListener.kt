package com.glucoring.ble.internal

import android.util.Log
import com.glucoring.ble.model.PpgSample
import com.glucoring.ble.model.VitalsSample
import com.jstyle.blesdk2301.callback.DataListener2301

/**
 * Bridges the vendor SDK's `DataListener2301` (Java, Map-based) callback into
 * typed Kotlin events.
 *
 * The vendor SDK decodes a raw notify frame into a `Map<String, Object>` whose
 * keys come from `com.jstyle.blesdk2301.constant.DeviceKey`. The two keys this
 * app cares about most are:
 *  - `arrayPpgRawData`: the raw green-LED PPG waveform for a frame (confirmed
 *    present in this SDK build's DeviceKey constants; NOT covered by the
 *    vendor's shipped documentation, which only documents the periodic
 *    auto-measurement command below — see startVitalsAutoMeasurement()).
 *  - `heartRate` / `Blood_oxygen` / `highPressure` / `lowPressure` / `hrv`:
 *    periodic vitals from the documented 0x28 health-measurement command —
 *    these names come from the vendor doc's description of the `AutoMode`
 *    model class fields, not from a confirmed Map key dump.
 *
 * IMPORTANT: the exact key names above are best-effort, not verified against
 * a real device. Every callback is logged in full below — if vitals/PPG
 * aren't showing up in the app despite the ring notifying (check
 * BleGattManager's "onCharacteristicChanged" logs), check logcat for this
 * class's TAG and compare the real key names against what's read below, then
 * fix the mismatches here.
 */
internal class SdkDataListener(
    private val onPpgFrame: (PpgSample) -> Unit,
    private val onVitals: (VitalsSample) -> Unit,
) : DataListener2301 {

    companion object {
        private const val TAG = "SdkDataListener"
    }

    override fun dataCallback(data: Map<String, Any>) {
        Log.d(TAG, "dataCallback(Map) keys=${data.keys}")
        for ((key, value) in data) {
            val preview = when (value) {
                is IntArray -> "IntArray(size=${value.size}) first10=${value.take(10)}"
                is List<*> -> "List(size=${value.size}) first10=${value.take(10)}"
                else -> value.toString()
            }
            Log.d(TAG, "  $key = $preview")
        }

        val now = System.currentTimeMillis()

        val rawGreen = (data["arrayPpgRawData"] as? IntArray)
            ?: (data["arrayPpgRawData"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toIntArray()

        if (rawGreen != null) {
            onPpgFrame(
                PpgSample(
                    timestampMs = now,
                    rawGreen = rawGreen,
                    heartRateBpm = (data["heartRate"] as? Number)?.toInt(),
                    spo2Percent = (data["Blood_oxygen"] as? Number)?.toInt(),
                    accelX = toIntArrayOrNull(data["arrayX"]),
                    accelY = toIntArrayOrNull(data["arrayY"]),
                    accelZ = toIntArrayOrNull(data["arrayZ"]),
                )
            )
        }

        val hasVitals = data.containsKey("heartRate") || data.containsKey("Blood_oxygen") ||
            data.containsKey("highPressure") || data.containsKey("hrv")
        if (hasVitals) {
            onVitals(
                VitalsSample(
                    timestampMs = now,
                    heartRateBpm = (data["heartRate"] as? Number)?.toInt(),
                    spo2Percent = (data["Blood_oxygen"] as? Number)?.toInt(),
                    systolic = (data["highPressure"] as? Number)?.toInt(),
                    diastolic = (data["lowPressure"] as? Number)?.toInt(),
                    hrv = (data["hrv"] as? Number)?.toInt(),
                    stress = (data["stress"] as? Number)?.toInt(),
                )
            )
        } else {
            Log.d(TAG, "dataCallback(Map): no known vitals keys present in this frame")
        }
    }

    override fun dataCallback(raw: ByteArray) {
        Log.d(TAG, "dataCallback(byte[]): [${raw.joinToString(" ") { "%02x".format(it) }}]")
        // Some frame types are surfaced only as raw bytes by the SDK.
        // Not currently needed for the glucose pipeline — left as an
        // extension point (e.g. for firmware/debug frames).
    }

    private fun toIntArrayOrNull(value: Any?): IntArray? = when (value) {
        is IntArray -> value
        is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
        else -> null
    }
}

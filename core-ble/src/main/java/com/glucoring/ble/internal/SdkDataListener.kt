package com.glucoring.ble.internal

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
 *  - `heartValue` / `Blood_oxygen` / `highPressure` / `lowPressure` / `hrvValue`
 *    / `stress`: periodic vitals from the documented 0x28 health-measurement
 *    command.
 *
 * If a future SDK/firmware revision changes these key names, this is the one
 * place that needs updating.
 */
internal class SdkDataListener(
    private val onPpgFrame: (PpgSample) -> Unit,
    private val onVitals: (VitalsSample) -> Unit,
) : DataListener2301 {

    override fun dataCallback(data: Map<String, Any>) {
        val now = System.currentTimeMillis()

        val rawGreen = (data["arrayPpgRawData"] as? IntArray)
            ?: (data["arrayPpgRawData"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toIntArray()

        if (rawGreen != null) {
            onPpgFrame(
                PpgSample(
                    timestampMs = now,
                    rawGreen = rawGreen,
                    heartRateBpm = (data["heartValue"] as? Number)?.toInt(),
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
        }
    }

    override fun dataCallback(raw: ByteArray) {
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

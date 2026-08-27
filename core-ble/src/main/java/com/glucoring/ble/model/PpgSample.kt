package com.glucoring.ble.model

/**
 * One decoded frame coming back from the ring's SDK callback.
 *
 * [rawGreen] is the raw green-channel PPG waveform for this frame (the
 * `arrayPpgRawData` key exposed by the vendor's DeviceKey constants). Depending
 * on firmware/SDK build this may arrive as a short burst per frame rather than
 * a continuous stream — core-signal windows/concatenates frames before feature
 * extraction, so callers don't need to worry about that here.
 */
data class PpgSample(
    val timestampMs: Long,
    val rawGreen: IntArray,
    val heartRateBpm: Int? = null,
    val spo2Percent: Int? = null,
    val accelX: IntArray? = null,
    val accelY: IntArray? = null,
    val accelZ: IntArray? = null,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/** Periodic auto-measurement result from the documented 0x28 "health measurement control" command. */
data class VitalsSample(
    val timestampMs: Long,
    val heartRateBpm: Int?,
    val spo2Percent: Int?,
    val systolic: Int?,
    val diastolic: Int?,
    val hrv: Int?,
    val stress: Int?,
)

data class ScannedDevice(
    val name: String,
    val macAddress: String,
    val rssi: Int?,
    val alreadyPaired: Boolean,
)

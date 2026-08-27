package com.glucoring.ble

import android.content.Context
import com.glucoring.ble.internal.BleGattManager
import com.glucoring.ble.internal.SdkDataListener
import com.glucoring.ble.model.BleConnectionState
import com.glucoring.ble.model.PpgSample
import com.glucoring.ble.model.ScannedDevice
import com.glucoring.ble.model.VitalsSample
import com.jstyle.blesdk2301.Util.BleSDK
import com.jstyle.blesdk2301.model.AutoTestMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Public entry point for the rest of the app. Wraps:
 *  - [DeviceScanner] / [BleGattManager] — our own GATT transport (see
 *    BleGattManager's kdoc for why this exists instead of using the SDK jar
 *    directly).
 *  - [BleSDK] — the vendor's command builder / frame parser.
 *  - [SdkDataListener] — adapts the vendor's Map-based callback to Kotlin flows.
 */
class GlucoRingBleClient(context: Context) {

    private companion object {
        private const val TAG = "GlucoRingBleClient"
    }

    private val appContext = context.applicationContext
    private val scanner = DeviceScanner(appContext)
    private val gatt = BleGattManager(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _ppgFrames = MutableSharedFlow<PpgSample>(extraBufferCapacity = 64)
    val ppgFrames: SharedFlow<PpgSample> = _ppgFrames

    private val _vitals = MutableSharedFlow<VitalsSample>(extraBufferCapacity = 16)
    val vitals: SharedFlow<VitalsSample> = _vitals

    val connectionState: StateFlow<BleConnectionState> = gatt.connectionState

    private val listener = SdkDataListener(
        onPpgFrame = { _ppgFrames.tryEmit(it) },
        onVitals = { _vitals.tryEmit(it) },
    )

    init {
        // Every raw notify frame gets handed to the vendor SDK for parsing;
        // the result comes back through `listener` (see SdkDataListener).
        scope.launch {
            gatt.observeRawFrames().collect { frame ->
                android.util.Log.d(TAG, "observeRawFrames: got ${frame.size} bytes, handing to BleSDK.DataParsingWithData")
                BleSDK.DataParsingWithData(frame, listener)
            }
        }
    }

    fun scanForDevices(namePrefixes: List<String> = listOf("JC", "jcring", "2301")): Flow<ScannedDevice> =
        scanner.scan(namePrefixes)

    fun connect(macAddress: String) = gatt.connect(macAddress)

    fun disconnect() = gatt.disconnect()

    /**
     * Starts the ring's documented periodic vitals measurement (Bluetooth
     * instruction 0x28 — "health measurement control"). This gives heart
     * rate / SpO2 / HRV / estimated blood pressure at a regular cadence and
     * is the one measurement path that's actually covered by the vendor's
     * shipped documentation.
     *
     * @param intervalSeconds vendor doc specifies a 30s minimum.
     */
    fun startVitalsAutoMeasurement(mode: AutoTestMode = AutoTestMode.AutoHeartRate, intervalSeconds: Long = 30) {
        val command = BleSDK.SetDeviceMeasurementWithType(mode, intervalSeconds, true)
        android.util.Log.d(TAG, "startVitalsAutoMeasurement: mode=$mode intervalSeconds=$intervalSeconds command=${command.joinToString(" ") { "%02x".format(it) }}")
        gatt.send(command)
    }

    fun stopVitalsAutoMeasurement(mode: AutoTestMode = AutoTestMode.AutoHeartRate) {
        gatt.send(BleSDK.SetDeviceMeasurementWithType(mode, 30, false))
    }

    fun requestBatteryLevel() = gatt.send(BleSDK.GetDeviceBatteryLevel())

    /**
     * TODO(vendor SDK): raw continuous PPG-waveform streaming (the
     * `arrayPpgRawData` field decoded in SdkDataListener) is present in this
     * SDK build's constants but its *start/stop command byte* is not covered
     * by the documentation shipped with the SDK ("2301 Android SDK
     * Documentation.doc" only documents 0x28 auto-measurement). Confirm the
     * exact command with the vendor (or by sniffing traffic from their demo
     * app / official phone app while a raw-waveform screen is open) and wire
     * it up here — everything downstream (SdkDataListener → ppgFrames Flow →
     * core-signal windowing) is already in place and will pick it up as soon
     * as frames start arriving with a non-null `arrayPpgRawData`.
     */
    fun startRawPpgCapture() {
        throw NotImplementedError(
            "Raw PPG waveform start command not confirmed with vendor docs yet — see kdoc above."
        )
    }
}

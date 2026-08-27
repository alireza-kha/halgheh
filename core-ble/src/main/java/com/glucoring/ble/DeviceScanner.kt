package com.glucoring.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.glucoring.ble.model.ScannedDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Scans for the ring. [namePrefixes] should match whatever the vendor uses
 * for the JCRing product line (check the device's advertised name once you
 * have a physical unit — the demo app matches names case-insensitively
 * against a caller-supplied list, we do the same here).
 */
internal class DeviceScanner(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun scan(namePrefixes: List<String>): Flow<ScannedDevice> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val scanner = adapter.bluetoothLeScanner

        // Already-bonded devices first, same as the vendor demo app does.
        adapter.bondedDevices
            ?.filter { d -> namePrefixes.any { d.name?.contains(it, ignoreCase = true) == true } }
            ?.forEach { d ->
                trySend(ScannedDevice(d.name ?: "?", d.address, rssi = null, alreadyPaired = true))
            }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: return
                if (namePrefixes.any { name.contains(it, ignoreCase = true) }) {
                    trySend(
                        ScannedDevice(
                            name = name,
                            macAddress = result.device.address,
                            rssi = result.rssi,
                            alreadyPaired = false,
                        )
                    )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed, errorCode=$errorCode"))
            }
        }

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(null, settings, callback)

        awaitClose { scanner.stopScan(callback) }
    }
}

package com.glucoring.ble.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.glucoring.ble.model.BleConnectionState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

/**
 * Raw BLE GATT transport for the JCRing (jstyle 2301) hardware.
 *
 * The vendor's own SDK jar (2301sdk1.0.jar) only ships the command-building /
 * frame-parsing layer (`BleSDK`, `DataListener2301`, model classes) — it does
 * NOT include the actual Bluetooth GATT scan/connect/notify plumbing. That
 * plumbing lives in the vendor's *demo app* source
 * (`com.jstyle.test2025.ble.BleService`), which is where these UUIDs come
 * from. We re-implement the same transport here in Kotlin/coroutines instead
 * of depending on the demo app's Java/RxJava service.
 */
internal class BleGattManager(private val context: Context) {

    companion object {
        private const val TAG = "BleGattManager"
        private val SERVICE_DATA: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val WRITE_CHARACTERISTIC: UUID = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb")
        private val NOTIFY_CHARACTERISTIC: UUID = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var gatt: BluetoothGatt? = null
    private val writeQueue: Queue<ByteArray> = LinkedList()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    /** Raw frames as they arrive from the notify characteristic, before any SDK parsing. */
    private val incomingFrames = Channel<ByteArray>(capacity = Channel.BUFFERED)

    @SuppressLint("MissingPermission")
    fun connect(macAddress: String) {
        _connectionState.value = BleConnectionState.Connecting
        val adapter = bluetoothManager.adapter
        val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeQueue.clear()
        _connectionState.value = BleConnectionState.Disconnected
    }

    /** Frames received from the ring's notify characteristic (still SDK-encoded, undecoded). */
    fun observeRawFrames(): Flow<ByteArray> = callbackFlow {
        // incomingFrames is fed by onCharacteristicChanged; we just forward it.
        for (frame in incomingFrames) {
            trySend(frame)
        }
        awaitClose { }
    }

    /** Queues a command frame; frames are sent one at a time, waiting for each write ack. */
    @SuppressLint("MissingPermission")
    fun send(command: ByteArray) {
        writeQueue.offer(command)
        if (writeQueue.size == 1) writeNext()
    }

    @SuppressLint("MissingPermission")
    private fun writeNext() {
        val current = gatt ?: return
        val data = writeQueue.peek() ?: return
        val service = current.getService(SERVICE_DATA) ?: run {
            Log.w(TAG, "writeNext: data service not found yet")
            return
        }
        val characteristic = service.getCharacteristic(WRITE_CHARACTERISTIC) ?: return
        characteristic.value = data
        current.writeCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(g: BluetoothGatt) {
        val service = g.getService(SERVICE_DATA) ?: return
        val characteristic = service.getCharacteristic(NOTIFY_CHARACTERISTIC) ?: return
        g.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG) ?: return
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        g.writeDescriptor(descriptor)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Disconnected
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(g)
            } else {
                _connectionState.value = BleConnectionState.Failed("discoverServices status=$status")
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Connected
            } else {
                _connectionState.value = BleConnectionState.Failed("enable-notify status=$status")
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            writeQueue.poll()
            if (writeQueue.isNotEmpty()) writeNext()
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            incomingFrames.trySend(value)
        }
    }
}

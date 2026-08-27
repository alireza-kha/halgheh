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

    @SuppressLint("MissingPermission", "DEPRECATION")
    private fun writeNext() {
        val current = gatt ?: return
        val data = writeQueue.peek() ?: return
        val service = current.getService(SERVICE_DATA) ?: run {
            Log.w(TAG, "writeNext: data service ($SERVICE_DATA) not found on this device yet")
            return
        }
        val characteristic = service.getCharacteristic(WRITE_CHARACTERISTIC) ?: run {
            Log.e(TAG, "writeNext: write characteristic ($WRITE_CHARACTERISTIC) not found — check the UUID against your actual ring's GATT table")
            return
        }

        // Some cheap BLE modules (this ring included, apparently) only
        // advertise WRITE_NO_RESPONSE on their command characteristic, not
        // WRITE_TYPE_DEFAULT (write-with-ack). Hard-coding DEFAULT meant
        // every write silently failed and onCharacteristicWrite never fired,
        // so the queue got stuck forever after the very first command —
        // matching "nothing happens when I tap start". Pick whichever the
        // characteristic actually supports instead.
        val supportsWriteWithResponse = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
        val supportsWriteNoResponse = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
        val writeType = when {
            supportsWriteWithResponse -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            supportsWriteNoResponse -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> {
                Log.e(TAG, "writeNext: characteristic advertises neither WRITE nor WRITE_NO_RESPONSE (properties=${characteristic.properties}) — command cannot be sent")
                writeQueue.poll()
                return
            }
        }

        Log.d(TAG, "writeNext: sending ${data.size} bytes [${data.joinToString(" ") { "%02x".format(it) }}] writeType=$writeType")

        val ok: Boolean
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val status = current.writeCharacteristic(characteristic, data, writeType)
            ok = status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            if (!ok) Log.e(TAG, "writeNext: writeCharacteristic (API 33+) failed, status=$status")
        } else {
            characteristic.writeType = writeType
            characteristic.value = data
            ok = current.writeCharacteristic(characteristic)
            if (!ok) Log.e(TAG, "writeNext: writeCharacteristic (legacy) returned false")
        }

        // WRITE_TYPE_NO_RESPONSE never triggers onCharacteristicWrite, so we
        // have to drain the queue here ourselves instead of waiting for a
        // callback that will never come for this write type.
        if (!ok || writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            writeQueue.poll()
            if (writeQueue.isNotEmpty()) writeNext()
        }
    }

    @SuppressLint("MissingPermission", "DEPRECATION")
    private fun enableNotifications(g: BluetoothGatt) {
        val service = g.getService(SERVICE_DATA) ?: return
        val characteristic = service.getCharacteristic(NOTIFY_CHARACTERISTIC) ?: return
        g.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG) ?: return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(descriptor)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status newState=$newState")
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
            Log.d(TAG, "onServicesDiscovered: status=$status services=${g.services.map { it.uuid }}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(g)
            } else {
                _connectionState.value = BleConnectionState.Failed("discoverServices status=$status")
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d(TAG, "onDescriptorWrite: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Connected
            } else {
                _connectionState.value = BleConnectionState.Failed("enable-notify status=$status")
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d(TAG, "onCharacteristicWrite: status=$status")
            writeQueue.poll()
            if (writeQueue.isNotEmpty()) writeNext()
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            // Called instead of the 2-arg overload below on API 33+.
            Log.d(TAG, "onCharacteristicChanged (API33+): [${value.joinToString(" ") { "%02x".format(it) }}]")
            incomingFrames.trySend(value)
        }

        @Deprecated("Deprecated in Java, but still the only callback invoked on API < 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            Log.d(TAG, "onCharacteristicChanged (legacy): [${value.joinToString(" ") { "%02x".format(it) }}]")
            incomingFrames.trySend(value)
        }
    }
}

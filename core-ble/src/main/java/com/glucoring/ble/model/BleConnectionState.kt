package com.glucoring.ble.model

sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data object Connecting : BleConnectionState
    data object Connected : BleConnectionState
    data class Failed(val reason: String) : BleConnectionState
}

package com.glucoring.app.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucoring.app.di.ServiceLocator
import com.glucoring.ble.model.BleConnectionState
import com.glucoring.ble.model.ScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val connectionState: StateFlow<BleConnectionState> = serviceLocator.bleClient.connectionState

    private var scanJob: kotlinx.coroutines.Job? = null

    fun startScan() {
        if (_isScanning.value) return
        _devices.value = emptyList()
        _isScanning.value = true
        scanJob = viewModelScope.launch {
            serviceLocator.bleClient.scanForDevices().collect { device ->
                val current = _devices.value
                if (current.none { it.macAddress == device.macAddress }) {
                    _devices.value = current + device
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    fun connect(device: ScannedDevice) {
        stopScan()
        serviceLocator.bleClient.connect(device.macAddress)
        // So Profile can show "متصل به …" and offer a disconnect button even
        // after the user navigates away from this screen.
        viewModelScope.launch {
            serviceLocator.repository.rememberPairedDevice(device.name, device.macAddress)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}

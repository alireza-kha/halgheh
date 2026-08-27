package com.glucoring.app.ui.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucoring.app.di.ServiceLocator
import com.glucoring.ble.model.BleConnectionState
import com.glucoring.ble.model.VitalsSample
import com.glucoring.ml.Estimate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonitorViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    val connectionState: StateFlow<BleConnectionState> = serviceLocator.bleClient.connectionState

    private val _latestVitals = MutableStateFlow<VitalsSample?>(null)
    val latestVitals: StateFlow<VitalsSample?> = _latestVitals.asStateFlow()

    private val _latestEstimate = MutableStateFlow<Estimate?>(null)
    val latestEstimate: StateFlow<Estimate?> = _latestEstimate.asStateFlow()

    /** True once there's enough calibration data to trust an estimate at all. */
    private val _hasTrainedModel = MutableStateFlow(false)
    val hasTrainedModel: StateFlow<Boolean> = _hasTrainedModel.asStateFlow()

    init {
        viewModelScope.launch {
            serviceLocator.bleClient.vitals.collect { _latestVitals.value = it }
        }
        viewModelScope.launch {
            serviceLocator.repository.getActiveModel()?.let { _hasTrainedModel.value = true }
        }
        // Poll for a fresh estimate periodically rather than on every single
        // PPG frame — a glucose number shouldn't visibly jitter at sensor rate.
        viewModelScope.launch {
            while (true) {
                val features = serviceLocator.featureExtractor.tryExtract()
                if (features != null) {
                    val estimate = serviceLocator.glucoseEstimator.estimate(features)
                    _latestEstimate.value = estimate
                    _hasTrainedModel.value = estimate != null
                }
                delay(10_000)
            }
        }
    }

    fun startVitalsMonitoring() = serviceLocator.bleClient.startVitalsAutoMeasurement()
    fun stopVitalsMonitoring() = serviceLocator.bleClient.stopVitalsAutoMeasurement()
}

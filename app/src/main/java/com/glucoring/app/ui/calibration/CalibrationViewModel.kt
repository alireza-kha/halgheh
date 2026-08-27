package com.glucoring.app.ui.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucoring.app.di.ServiceLocator
import com.glucoring.data.db.entity.GlucoseReferenceEntity
import com.glucoring.data.repository.CalibrationReadiness
import com.glucoring.ml.RetrainResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalibrationViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    val readings: StateFlow<List<GlucoseReferenceEntity>> =
        serviceLocator.repository.observeReferenceReadings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _readiness = MutableStateFlow<CalibrationReadiness?>(null)
    val readiness: StateFlow<CalibrationReadiness?> = _readiness.asStateFlow()

    private val _lastRetrainResult = MutableStateFlow<RetrainResult?>(null)
    val lastRetrainResult: StateFlow<RetrainResult?> = _lastRetrainResult.asStateFlow()

    init {
        refreshReadiness()
    }

    private fun refreshReadiness() {
        viewModelScope.launch {
            _readiness.value = serviceLocator.repository.calibrationReadiness()
        }
    }

    /** [glucoseMgDl] is what the user read off their fingerstick meter, entered manually — never inferred. */
    fun addReading(glucoseMgDl: Int, context: String, note: String?) {
        viewModelScope.launch {
            serviceLocator.repository.logGlucoseReference(
                timestampMs = System.currentTimeMillis(),
                glucoseMgDl = glucoseMgDl,
                context = context,
                note = note,
            )
            refreshReadiness()
        }
    }

    fun retrain() {
        viewModelScope.launch {
            _lastRetrainResult.value = serviceLocator.glucoseEstimator.retrain()
            refreshReadiness()
        }
    }
}

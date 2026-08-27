package com.glucoring.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucoring.app.di.ServiceLocator
import com.glucoring.ble.model.BleConnectionState
import com.glucoring.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    val connectionState: StateFlow<BleConnectionState> = serviceLocator.bleClient.connectionState

    private val _profile = MutableStateFlow(UserProfileEntity.default())
    val profile: StateFlow<UserProfileEntity> = _profile.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        viewModelScope.launch {
            _profile.value = serviceLocator.repository.getProfile()
        }
    }

    fun save(
        displayName: String,
        ageYears: Int?,
        diabetesType: String,
        targetLow: Int,
        targetHigh: Int,
        notes: String?,
    ) {
        viewModelScope.launch {
            val updated = _profile.value.copy(
                displayName = displayName,
                ageYears = ageYears,
                diabetesType = diabetesType,
                targetRangeLowMgDl = targetLow,
                targetRangeHighMgDl = targetHigh,
                notes = notes,
            )
            serviceLocator.repository.saveProfile(updated)
            _profile.value = updated
            _saveState.value = SaveState.Saved
        }
    }

    /** Disconnects from the ring. Does NOT clear the remembered device — the user can reconnect from Scan without re-pairing. */
    fun disconnectFromRing() {
        serviceLocator.bleClient.disconnect()
    }
}

sealed interface SaveState {
    data object Idle : SaveState
    data object Saved : SaveState
}

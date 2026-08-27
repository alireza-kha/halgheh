package com.glucoring.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucoring.app.di.ServiceLocator
import com.glucoring.data.db.entity.PpgWindowEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(serviceLocator: ServiceLocator) : ViewModel() {
    val recentWindows: StateFlow<List<PpgWindowEntity>> =
        serviceLocator.repository.observeRecentWindows(limit = 500)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

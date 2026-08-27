package com.glucoring.app.di

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.Composable
import com.glucoring.app.ui.calibration.CalibrationViewModel
import com.glucoring.app.ui.history.HistoryViewModel
import com.glucoring.app.ui.monitor.MonitorViewModel
import com.glucoring.app.ui.scan.ScanViewModel

/**
 * Thin `viewModel()` wrappers that pass the app-wide [ServiceLocator] into each
 * screen's ViewModel. Kept in one file since there's no DI framework here.
 */
@Composable
fun scanViewModel(serviceLocator: ServiceLocator): ScanViewModel =
    viewModel(factory = viewModelFactory { initializer { ScanViewModel(serviceLocator) } })

@Composable
fun monitorViewModel(serviceLocator: ServiceLocator): MonitorViewModel =
    viewModel(factory = viewModelFactory { initializer { MonitorViewModel(serviceLocator) } })

@Composable
fun calibrationViewModel(serviceLocator: ServiceLocator): CalibrationViewModel =
    viewModel(factory = viewModelFactory { initializer { CalibrationViewModel(serviceLocator) } })

@Composable
fun historyViewModel(serviceLocator: ServiceLocator): HistoryViewModel =
    viewModel(factory = viewModelFactory { initializer { HistoryViewModel(serviceLocator) } })

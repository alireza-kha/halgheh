package com.glucoring.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.ui.calibration.CalibrationScreen
import com.glucoring.app.ui.history.HistoryScreen
import com.glucoring.app.ui.monitor.MonitorScreen
import com.glucoring.app.ui.scan.ScanScreen
import com.glucoring.app.ui.settings.SettingsScreen

object Routes {
    const val SCAN = "scan"
    const val MONITOR = "monitor"
    const val CALIBRATION = "calibration"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun GlucoRingNavGraph(serviceLocator: ServiceLocator) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MONITOR) {
        composable(Routes.SCAN) { ScanScreen(serviceLocator, navController) }
        composable(Routes.MONITOR) { MonitorScreen(serviceLocator, navController) }
        composable(Routes.CALIBRATION) { CalibrationScreen(serviceLocator, navController) }
        composable(Routes.HISTORY) { HistoryScreen(serviceLocator, navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
    }
}

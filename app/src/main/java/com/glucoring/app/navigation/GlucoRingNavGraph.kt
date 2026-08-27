package com.glucoring.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.ui.calibration.CalibrationScreen
import com.glucoring.app.ui.history.HistoryScreen
import com.glucoring.app.ui.monitor.MonitorScreen
import com.glucoring.app.ui.profile.ProfileScreen
import com.glucoring.app.ui.scan.ScanScreen
import com.glucoring.app.ui.settings.SettingsScreen

object Routes {
    const val SCAN = "scan"
    const val MONITOR = "monitor"
    const val CALIBRATION = "calibration"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// Scan isn't a bottom tab — it's reached from Monitor/Profile when there's no
// active connection, same as before.
private val bottomTabs = listOf(
    BottomTab(Routes.MONITOR, "مانیتور", Icons.Filled.Favorite),
    BottomTab(Routes.HISTORY, "تاریخچه", Icons.Filled.History),
    BottomTab(Routes.CALIBRATION, "کالیبراسیون", Icons.Filled.Timeline),
    BottomTab(Routes.PROFILE, "پروفایل", Icons.Filled.AccountCircle),
    BottomTab(Routes.SETTINGS, "تنظیمات", Icons.Filled.Settings),
)

@Composable
fun GlucoRingNavGraph(serviceLocator: ServiceLocator) {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MONITOR,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SCAN) { ScanScreen(serviceLocator, navController) }
            composable(Routes.MONITOR) { MonitorScreen(serviceLocator, navController) }
            composable(Routes.CALIBRATION) { CalibrationScreen(serviceLocator, navController) }
            composable(Routes.HISTORY) { HistoryScreen(serviceLocator, navController) }
            composable(Routes.PROFILE) { ProfileScreen(serviceLocator, navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
        }
    }
}

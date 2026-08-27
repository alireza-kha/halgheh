package com.glucoring.app.ui.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.di.scanViewModel
import com.glucoring.app.navigation.Routes
import com.glucoring.ble.model.BleConnectionState

@Composable
fun ScanScreen(serviceLocator: ServiceLocator, nav: NavHostController) {
    val vm = scanViewModel(serviceLocator)
    val devices by vm.devices.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val connectionState by vm.connectionState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("اتصال به رینگ") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            Text(
                when (connectionState) {
                    is BleConnectionState.Connected -> "متصل ✅"
                    is BleConnectionState.Connecting -> "در حال اتصال…"
                    is BleConnectionState.Failed -> "اتصال ناموفق"
                    BleConnectionState.Disconnected -> "متصل نیست"
                }
            )

            Button(
                onClick = { if (isScanning) vm.stopScan() else vm.startScan() },
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                Text(if (isScanning) "توقف جستجو" else "جستجوی دستگاه‌ها")
            }

            LazyColumn {
                items(devices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.connect(device)
                                nav.navigate(Routes.MONITOR)
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(device.name)
                            Text(device.macAddress)
                        }
                        Text(if (device.alreadyPaired) "جفت‌شده" else "RSSI ${device.rssi ?: "-"}")
                    }
                    Divider()
                }
            }
        }
    }
}

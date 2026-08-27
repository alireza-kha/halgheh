package com.glucoring.app.ui.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.di.monitorViewModel
import com.glucoring.app.navigation.Routes
import com.glucoring.ble.model.BleConnectionState

@Composable
fun MonitorScreen(serviceLocator: ServiceLocator, nav: NavHostController) {
    val vm = monitorViewModel(serviceLocator)
    val connectionState by vm.connectionState.collectAsState()
    val vitals by vm.latestVitals.collectAsState()
    val estimate by vm.latestEstimate.collectAsState()
    val hasTrainedModel by vm.hasTrainedModel.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("مانیتور زنده") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            if (connectionState !is BleConnectionState.Connected) {
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("رینگ متصل نیست")
                        Button(onClick = { nav.navigate(Routes.SCAN) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("اتصال به دستگاه")
                        }
                    }
                }
            }

            GlucoseEstimateCard(hasTrainedModel = hasTrainedModel, estimateMgDl = estimate?.mgDl, nav = nav)

            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("علائم حیاتی لحظه‌ای", fontWeight = FontWeight.Bold)
                    Text("ضربان قلب: ${vitals?.heartRateBpm ?: "-"} bpm")
                    Text("SpO2: ${vitals?.spo2Percent ?: "-"} %")
                    Text("HRV: ${vitals?.hrv ?: "-"}")
                    Text("فشار خون تخمینی: ${vitals?.systolic ?: "-"}/${vitals?.diastolic ?: "-"}")
                }
            }

            Button(onClick = { vm.startVitalsMonitoring() }, modifier = Modifier.padding(top = 12.dp)) {
                Text("شروع اندازه‌گیری خودکار علائم حیاتی")
            }
        }
    }
}

@Composable
private fun GlucoseEstimateCard(hasTrainedModel: Boolean, estimateMgDl: Double?, nav: NavHostController) {
    Card(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (!hasTrainedModel) {
                Text("هنوز مدل کالیبره نشده", fontWeight = FontWeight.Bold)
                Text("برای مشاهده‌ی تخمین قند خون، اول چند نقطه‌ی کالیبراسیون با سوزن ثبت کنید.")
                Button(onClick = { nav.navigate(Routes.CALIBRATION) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("رفتن به کالیبراسیون")
                }
            } else {
                Text("تخمین قند خون (آزمایشی)", fontWeight = FontWeight.Bold)
                Text(
                    text = estimateMgDl?.let { "${it.toInt()} mg/dL" } ?: "در حال محاسبه…",
                    fontSize = 32.sp,
                )
                Text(
                    "⚠️ این عدد یک تخمین آزمایشی است، نه جایگزین اندازه‌گیری با سوزن.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

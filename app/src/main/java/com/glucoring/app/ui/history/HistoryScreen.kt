package com.glucoring.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.di.historyViewModel
import com.glucoring.signal.FeatureVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A plain scrollable log for now — this is exactly the "log a full day's
 * changes" view you described. Swap the LazyColumn for a real chart
 * (e.g. a line chart of perfusionIndex or the predicted glucose curve over
 * the day) once you're ready; recentWindows already has everything needed.
 */
@Composable
fun HistoryScreen(serviceLocator: ServiceLocator, nav: NavHostController) {
    val vm = historyViewModel(serviceLocator)
    val windows by vm.recentWindows.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US) }

    Scaffold(topBar = { TopAppBar(title = { Text("تاریخچه‌ی روزانه") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("${windows.size} پنجره‌ی PPG ثبت‌شده")
            LazyColumn {
                items(windows) { window ->
                    val features = runCatching { FeatureVector.fromJson(window.featureVectorJson) }.getOrNull()
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(dateFormat.format(Date(window.timestampMs)))
                            Text("HR ${window.heartRateBpm ?: "-"} · SpO2 ${window.spo2Percent ?: "-"} · PI ${features?.perfusionIndex?.let { "%.3f".format(it) } ?: "-"}")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

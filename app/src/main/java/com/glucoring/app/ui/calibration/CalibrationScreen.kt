package com.glucoring.app.ui.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.di.calibrationViewModel
import com.glucoring.ml.RetrainResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val contexts = listOf("fasting", "post_meal", "before_bed", "other")
private fun contextLabel(c: String) = when (c) {
    "fasting" -> "ناشتا"
    "post_meal" -> "بعد از غذا"
    "before_bed" -> "قبل خواب"
    else -> "سایر"
}

@Composable
fun CalibrationScreen(serviceLocator: ServiceLocator, nav: NavHostController) {
    val vm = calibrationViewModel(serviceLocator)
    val readings by vm.readings.collectAsState()
    val readiness by vm.readiness.collectAsState()
    val retrainResult by vm.lastRetrainResult.collectAsState()

    var glucoseInput by remember { mutableStateOf("") }
    var selectedContext by remember { mutableStateOf(contexts.first()) }
    var contextMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("کالیبراسیون با سوزن") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ثبت مقدار قند خون اندازه‌گیری‌شده با سوزن")

                    OutlinedTextField(
                        value = glucoseInput,
                        onValueChange = { glucoseInput = it.filter { c -> c.isDigit() } },
                        label = { Text("mg/dL") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )

                    Row(Modifier.padding(top = 8.dp)) {
                        TextButton(onClick = { contextMenuExpanded = true }) {
                            Text(contextLabel(selectedContext))
                        }
                        DropdownMenu(expanded = contextMenuExpanded, onDismissRequest = { contextMenuExpanded = false }) {
                            contexts.forEach { c ->
                                DropdownMenuItem(text = { Text(contextLabel(c)) }, onClick = {
                                    selectedContext = c
                                    contextMenuExpanded = false
                                })
                            }
                        }
                    }

                    Button(
                        onClick = {
                            glucoseInput.toIntOrNull()?.let { value ->
                                vm.addReading(value, selectedContext, note = null)
                                glucoseInput = ""
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = glucoseInput.toIntOrNull() != null,
                    ) { Text("ثبت") }
                }
            }

            readiness?.let { r ->
                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("وضعیت آمادگی کالیبراسیون")
                        Text("تعداد نقاط ثبت‌شده: ${r.pointCount} (حداقل لازم برای شروع: 6)")
                        Text("دامنه‌ی مقادیر: ${r.valueSpreadMgDl} mg/dL (باید حداقل نقاط متنوع، نه همه نزدیک هم، باشند)")
                        Button(onClick = { vm.retrain() }, enabled = r.isReady, modifier = Modifier.padding(top = 8.dp)) {
                            Text("آموزش/به‌روزرسانی مدل شخصی")
                        }
                    }
                }
            }

            retrainResult?.let { result ->
                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        when (result) {
                            is RetrainResult.Success -> {
                                Text("مدل با ${result.calibrationPointCount} نقطه آموزش دید")
                                Text("خطای تخمینی (MARD): ${result.mardPercent?.let { "%.1f".format(it) } ?: "نامشخص"}٪")
                                Text("سطح اعتماد: ${result.quality}")
                            }
                            is RetrainResult.NotEnoughData -> Text("داده‌ی کافی برای آموزش وجود ندارد.")
                        }
                    }
                }
            }

            Text("تاریخچه‌ی ثبت‌ها", modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
            LazyColumn {
                items(readings) { reading ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${reading.glucoseMgDl} mg/dL — ${contextLabel(reading.context)}")
                        Text(dateFormat.format(Date(reading.timestampMs)))
                    }
                    Divider()
                }
            }
        }
    }
}

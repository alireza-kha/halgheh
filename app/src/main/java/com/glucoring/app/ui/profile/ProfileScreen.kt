package com.glucoring.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.glucoring.app.di.ServiceLocator
import com.glucoring.app.di.profileViewModel
import com.glucoring.ble.model.BleConnectionState

private val diabetesTypes = listOf("none", "type1", "type2", "gestational", "other")
private fun diabetesTypeLabel(t: String) = when (t) {
    "type1" -> "دیابت نوع ۱"
    "type2" -> "دیابت نوع ۲"
    "gestational" -> "دیابت بارداری"
    "other" -> "سایر"
    else -> "ندارم / فقط پژوهشی"
}

@Composable
fun ProfileScreen(serviceLocator: ServiceLocator, nav: NavHostController) {
    val vm = profileViewModel(serviceLocator)
    val profile by vm.profile.collectAsState()
    val connectionState by vm.connectionState.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var diabetesType by remember { mutableStateOf("none") }
    var diabetesMenuExpanded by remember { mutableStateOf(false) }
    var targetLowInput by remember { mutableStateOf("70") }
    var targetHighInput by remember { mutableStateOf("140") }
    var notes by remember { mutableStateOf("") }

    // Profile loads asynchronously from Room; sync the form fields once it arrives.
    LaunchedEffect(profile) {
        displayName = profile.displayName
        ageInput = profile.ageYears?.toString() ?: ""
        diabetesType = profile.diabetesType
        targetLowInput = profile.targetRangeLowMgDl.toString()
        targetHighInput = profile.targetRangeHighMgDl.toString()
        notes = profile.notes ?: ""
    }

    Scaffold(topBar = { TopAppBar(title = { Text("پروفایل") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {

            // ---- Bluetooth connection card ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("اتصال رینگ", fontWeight = FontWeight.Bold)

                    val statusText = when (connectionState) {
                        is BleConnectionState.Connected -> "متصل"
                        is BleConnectionState.Connecting -> "در حال اتصال…"
                        is BleConnectionState.Failed -> "اتصال ناموفق"
                        BleConnectionState.Disconnected -> "متصل نیست"
                    }
                    Text("وضعیت: $statusText", modifier = Modifier.padding(top = 4.dp))

                    profile.pairedDeviceName?.let { name ->
                        Text("آخرین دستگاه جفت‌شده: $name")
                        Text(profile.pairedDeviceMac.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }

                    Row(Modifier.padding(top = 12.dp)) {
                        OutlinedButton(
                            onClick = { vm.disconnectFromRing() },
                            enabled = connectionState is BleConnectionState.Connected || connectionState is BleConnectionState.Connecting,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("قطع اتصال از رینگ")
                        }
                    }
                }
            }

            // ---- Profile form ----
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("اطلاعات پروفایل", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("نام") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it.filter { c -> c.isDigit() } },
                        label = { Text("سن") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )

                    Row(Modifier.padding(top = 8.dp)) {
                        Text("نوع دیابت: ")
                        TextButton(onClick = { diabetesMenuExpanded = true }) {
                            Text(diabetesTypeLabel(diabetesType))
                        }
                        DropdownMenu(expanded = diabetesMenuExpanded, onDismissRequest = { diabetesMenuExpanded = false }) {
                            diabetesTypes.forEach { t ->
                                DropdownMenuItem(text = { Text(diabetesTypeLabel(t)) }, onClick = {
                                    diabetesType = t
                                    diabetesMenuExpanded = false
                                })
                            }
                        }
                    }

                    Text("بازه‌ی هدف قند خون (mg/dL)", modifier = Modifier.padding(top = 12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = targetLowInput,
                            onValueChange = { targetLowInput = it.filter { c -> c.isDigit() } },
                            label = { Text("حداقل") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                        )
                        OutlinedTextField(
                            value = targetHighInput,
                            onValueChange = { targetHighInput = it.filter { c -> c.isDigit() } },
                            label = { Text("حداکثر") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("یادداشت (اختیاری)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )

                    Button(
                        onClick = {
                            vm.save(
                                displayName = displayName,
                                ageYears = ageInput.toIntOrNull(),
                                diabetesType = diabetesType,
                                targetLow = targetLowInput.toIntOrNull() ?: 70,
                                targetHigh = targetHighInput.toIntOrNull() ?: 140,
                                notes = notes.ifBlank { null },
                            )
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("ذخیره") }
                }
            }
        }
    }
}

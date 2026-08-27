package com.glucoring.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glucoring.app.navigation.GlucoRingNavGraph
import com.glucoring.app.ui.theme.GlucoRingTheme

class MainActivity : ComponentActivity() {

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled reactively by screens checking bluetoothAdapter state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        requestPermissions.launch(permissions.toTypedArray())

        val app = application as GlucoRingApp

        setContent {
            GlucoRingTheme {
                var showDisclaimer by remember { mutableStateOf(true) }

                Surface {
                    GlucoRingNavGraph(serviceLocator = app.serviceLocator)
                }

                if (showDisclaimer) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text(stringResource(R.string.disclaimer_title)) },
                        text = {
                            Column(Modifier.padding(4.dp)) {
                                Text(stringResource(R.string.disclaimer_body))
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDisclaimer = false }) {
                                Text("متوجه شدم")
                            }
                        },
                    )
                }
            }
        }
    }
}

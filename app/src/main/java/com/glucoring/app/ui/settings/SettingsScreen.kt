package com.glucoring.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.glucoring.app.R

@Composable
fun SettingsScreen(nav: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("تنظیمات") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(Modifier.padding(bottom = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.disclaimer_body), modifier = Modifier.padding(top = 8.dp))
                }
            }

            Text("سینک با سرور مرکزی: غیرفعال (فقط ذخیره‌ی محلی)")
            Text(
                "وقتی سینک فعال شود، فقط بردار ویژگی‌های PPG و مقدار قند مرجع ارسال می‌شود، " +
                    "نه موج خام و نه هیچ اطلاعات هویتی — و فقط با رضایت صریح شما.",
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.glucoring.ble"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Vendor SDK for the JCRing (jstyle blesdk2301) ring — provides command
    // building / raw-frame parsing (BleSDK, DataListener2301, model classes).
    // NOTE: the GATT transport layer (scan/connect/notify) is NOT in this jar —
    // we implement it ourselves in internal/BleGattManager.kt, mirroring the
    // UUIDs used by the vendor's own demo app (service fff0, write fff6, notify fff7).
    implementation(files("libs/2301sdk1.0.jar"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.glucoring.ble"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    //
    // Declared as `api`, not `implementation`: GlucoRingBleClient's public
    // functions (e.g. startVitalsAutoMeasurement) take an AutoTestMode
    // parameter from this jar, so consumers like :app need it on their own
    // compile classpath too — `implementation` would hide it from them.
    api(files("libs/2301sdk1.0.jar"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

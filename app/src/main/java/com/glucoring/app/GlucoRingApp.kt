package com.glucoring.app

import android.app.Application
import com.glucoring.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlucoRingApp : Application() {

    lateinit var serviceLocator: ServiceLocator
        private set

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator.getInstance(this)

        // Feed every parsed PPG frame into the windowing extractor and log a
        // feature window once it's ready. This is the "log a full day's worth
        // of changes" pipeline you described — it runs for as long as the
        // process is alive and the ring stays connected. For real all-day
        // background logging you'll also want a foreground Service + a
        // reconnect/retry policy; that's the next thing to add once this
        // skeleton compiles and the raw-PPG start command is confirmed with
        // the vendor (see GlucoRingBleClient.startRawPpgCapture kdoc).
        appScope.launch {
            serviceLocator.bleClient.ppgFrames.collect { frame ->
                serviceLocator.featureExtractor.addFrame(frame)
                serviceLocator.featureExtractor.tryExtract()?.let { features ->
                    serviceLocator.repository.logPpgWindow(
                        timestampMs = System.currentTimeMillis(),
                        features = features,
                        heartRateBpm = frame.heartRateBpm,
                        spo2Percent = frame.spo2Percent,
                    )
                }
            }
        }

        appScope.launch { serviceLocator.glucoseEstimator.loadActiveModel() }
    }
}

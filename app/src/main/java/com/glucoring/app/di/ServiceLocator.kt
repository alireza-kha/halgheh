package com.glucoring.app.di

import android.content.Context
import com.glucoring.ble.GlucoRingBleClient
import com.glucoring.data.db.AppDatabase
import com.glucoring.data.repository.GlucoRepository
import com.glucoring.ml.GlucoseEstimator
import com.glucoring.signal.PpgFeatureExtractor
import com.glucoring.sync.LocalSyncQueue
import com.glucoring.sync.NoOpSyncClient
import com.glucoring.sync.SyncClient

/**
 * Deliberately simple manual DI — no Hilt/Koin — since the module graph here
 * is small and static. Swap this out for a DI framework once the app grows
 * past a handful of screens.
 */
class ServiceLocator private constructor(context: Context) {

    val bleClient: GlucoRingBleClient = GlucoRingBleClient(context)
    val featureExtractor: PpgFeatureExtractor = PpgFeatureExtractor()

    private val database: AppDatabase = AppDatabase.getInstance(context)
    val repository: GlucoRepository = GlucoRepository(database)
    val glucoseEstimator: GlucoseEstimator = GlucoseEstimator(repository)

    // Sync is intentionally a no-op until a real backend + consent flow exist.
    val syncClient: SyncClient = NoOpSyncClient()
    val syncQueue: LocalSyncQueue = LocalSyncQueue()

    companion object {
        @Volatile private var instance: ServiceLocator? = null

        fun getInstance(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context.applicationContext).also { instance = it }
            }
    }
}

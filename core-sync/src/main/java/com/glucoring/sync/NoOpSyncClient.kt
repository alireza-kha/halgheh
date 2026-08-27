package com.glucoring.sync

/** Default wiring while sync is disabled: never touches the network, always succeeds locally. */
class NoOpSyncClient : SyncClient {
    override suspend fun uploadCalibrationRecord(record: SyncRecord): Result<Unit> = Result.success(Unit)
    override suspend fun downloadPopulationModelUpdate(): Result<ByteArray?> = Result.success(null)
}

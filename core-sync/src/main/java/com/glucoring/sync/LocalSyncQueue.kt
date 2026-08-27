package com.glucoring.sync

/**
 * Records that *would* be synced once you turn a real [SyncClient] on,
 * held here in memory/local-only for now. When you're ready to add the
 * central server, point [SyncClient] at a real implementation and drain
 * this queue — no other module needs to change.
 */
class LocalSyncQueue {
    private val pending = mutableListOf<SyncRecord>()

    fun enqueue(record: SyncRecord) {
        pending.add(record)
    }

    fun pendingCount(): Int = pending.size

    suspend fun drain(client: SyncClient): Int {
        var sent = 0
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val record = iterator.next()
            val result = client.uploadCalibrationRecord(record)
            if (result.isSuccess) {
                iterator.remove()
                sent++
            } else {
                break // preserve order; stop on first failure and retry later
            }
        }
        return sent
    }
}

package com.glucoring.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.glucoring.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = UserProfileEntity.SINGLETON_ID): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observe(id: Int = UserProfileEntity.SINGLETON_ID): Flow<UserProfileEntity?>

    @Query("UPDATE user_profile SET pairedDeviceName = :name, pairedDeviceMac = :mac WHERE id = :id")
    suspend fun updatePairedDevice(name: String?, mac: String?, id: Int = UserProfileEntity.SINGLETON_ID)
}

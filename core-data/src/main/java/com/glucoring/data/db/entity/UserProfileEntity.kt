package com.glucoring.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row local profile. Kept minimal on purpose — this app is a research
 * tool, not a health-records app, so it only stores what actually helps
 * interpret readings (age can matter for vascular/PPG baseline, diabetes
 * type changes what glucose ranges are meaningful) plus which ring is
 * currently paired. Nothing here is synced anywhere (see core-sync).
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val displayName: String,
    val ageYears: Int?,
    val diabetesType: String, // "none", "type1", "type2", "gestational", "other"
    val targetRangeLowMgDl: Int,
    val targetRangeHighMgDl: Int,
    val notes: String?,
    val pairedDeviceName: String?,
    val pairedDeviceMac: String?,
) {
    companion object {
        /** Only ever one profile row locally — this app doesn't support multiple user accounts on one device. */
        const val SINGLETON_ID = 1

        fun default() = UserProfileEntity(
            displayName = "",
            ageYears = null,
            diabetesType = "none",
            targetRangeLowMgDl = 70,
            targetRangeHighMgDl = 140,
            notes = null,
            pairedDeviceName = null,
            pairedDeviceMac = null,
        )
    }
}

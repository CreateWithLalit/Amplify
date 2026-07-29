package com.lalit.amplify.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

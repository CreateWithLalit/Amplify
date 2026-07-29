package com.lalit.amplify.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey val songId: Long,
    val downloadUrl: String,
    val filePath: String?,
    val quality: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

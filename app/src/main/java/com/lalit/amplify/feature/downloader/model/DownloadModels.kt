package com.lalit.amplify.feature.downloader.model

import android.net.Uri

/**
 * Represents a download task in the system.
 */
data class DownloadTask(
    val id: String,
    val trackTitle: String,
    val trackArtist: String,
    val streamUrl: String,
    val thumbnailUrl: String?,
    val fileName: String,
    val fileExtension: String,
    val contentType: String,
    val destinationUri: Uri,        // SAF tree/document URI
    val audioQuality: String
)

/**
 * Current state of a download operation.
 */
sealed class DownloadState {
    object Idle : DownloadState()
    object Preparing : DownloadState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Processing(val message: String = "Processing...") : DownloadState()
    data class Success(val fileUri: Uri, val fileName: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
    object Cancelled : DownloadState()
}

/**
 * Duplicate handling strategy.
 */
enum class DuplicateStrategy {
    REPLACE,    // Overwrite existing file
    KEEP_BOTH,  // Save with incremented suffix
    CANCEL      // Abort download
}

/**
 * Download quality preference.
 */
enum class DownloadQuality(val label: String, val bitrateKbps: Int) {
    HIGH("320 kbps", 320),
    MEDIUM("192 kbps", 192),
    LOW("128 kbps", 128)
}


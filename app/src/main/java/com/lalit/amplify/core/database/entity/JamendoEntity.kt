package com.lalit.amplify.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jamendo favorite tracks - stored separately from local favorites
 */
@Entity(tableName = "jamendo_favorites")
data class JamendoFavoriteEntity(
    @PrimaryKey val jamendoTrackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val imageUrl: String? = null,
    val streamUrl: String? = null,
    val licenseInfo: String? = null,
    val sourceUrl: String? = null,
    val artistUrl: String? = null,
    val genres: String? = null,  // Comma-separated
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Jamendo recently played tracks - separate history from local
 */
@Entity(tableName = "jamendo_recently_played")
data class JamendoRecentlyPlayedEntity(
    @PrimaryKey val jamendoTrackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val imageUrl: String? = null,
    val streamUrl: String? = null,
    val licenseInfo: String? = null,
    val playedAt: Long = System.currentTimeMillis()
)

/**
 * Jamendo audio quality preference
 * Stored separately for per-source configuration
 */
@Entity(tableName = "jamendo_quality_preference")
data class JamendoQualityPreferenceEntity(
    @PrimaryKey val preferenceKey: String = "quality",
    val quality: String = "mp32"  // Preferred: mp32 (VBR), mp31 (128), ogg, flac
)


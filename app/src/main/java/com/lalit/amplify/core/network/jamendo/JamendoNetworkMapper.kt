package com.lalit.amplify.core.network.jamendo

import android.net.Uri
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource

/**
 * Mapper to convert Jamendo API responses to domain Song objects
 */
object JamendoNetworkMapper {

    /**
     * Convert a Jamendo track to a Song object
     * Uses 64-bit hash of Jamendo track ID as unique identifier for consistency
     */
    fun toSong(track: JamendoTrack, preferredQuality: String = "mp32"): Song {
        // Use hash of jamendo track ID as local ID for consistency
        val jamendoId = track.id.toLongOrNull() ?: track.id.hashCode().toLong()

        // Get stream URL based on preferred quality
        val streamUrl = getStreamUrl(track, preferredQuality)

        // Parse license information
        val licenseInfo = parseLicense(track.licenseType, track.licenseUrl)

        // Use tags as genres
        val genres = track.tags ?: emptyList()

        return Song(
            id = jamendoId,
            title = track.name,
            artist = track.artistName,
            album = track.albumName.ifEmpty { "Jamendo" },
            duration = (track.duration * 1000L),  // Convert to milliseconds
            uri = streamUrl?.let { Uri.parse(it) } ?: Uri.EMPTY,
            albumArtUri = track.imageUrl?.let { Uri.parse(it) },
            source = SongSource.JAMENDO,
            streamUrl = streamUrl,
            // Phase 2 is stream-only. Never expose a download URL from this
            // playback model, even where an artist permits downloading.
            downloadUrl = null,
            sourceUrl = track.shareUrl,
            artistUrl = track.artistId.takeIf { it.isNotBlank() }?.let { Uri.parse("https://www.jamendo.com/artist/$it").toString() },
            licenseInfo = licenseInfo,
            isExplicit = false,  // Jamendo doesn't provide explicit flag
            genres = genres
        )
    }

    /**
     * Get the appropriate stream URL based on quality preference
     * Falls back to next available quality if preferred is not available
     */
    private fun getStreamUrl(track: JamendoTrack, preferredQuality: String): String? {
        // Older responses can expose a format map. Current v3 responses put
        // the requested `audioformat` in the single `audio` field.
        track.audioStreams?.let { streams ->
            // Try preferred quality first
            streams[preferredQuality]?.let { return it }

            // Fallback order: mp32 (VBR) > ogg > mp31 > flac
            val fallbackOrder = listOf("mp32", "ogg", "mp31", "flac")
            for (quality in fallbackOrder) {
                streams[quality]?.let { return it }
            }

            // Return first available
            return streams.values.firstOrNull()
        }

        // Legacy fallback to single audio URL
        return track.audioUrl
    }

    /**
     * Parse license information from Jamendo track
     */
    private fun parseLicense(licenseType: String?, licenseUrl: String?): String {
        if (licenseType == null) {
            return "License Unknown"
        }

        val licenseName = when (licenseType) {
            "by" -> "CC Attribution"
            "by-sa" -> "CC Attribution-ShareAlike"
            "by-nd" -> "CC Attribution-NoDerivatives"
            "by-nc" -> "CC Attribution-NonCommercial"
            "by-nc-sa" -> "CC Attribution-NonCommercial-ShareAlike"
            "by-nc-nd" -> "CC Attribution-NonCommercial-NoDerivatives"
            else -> "Creative Commons $licenseType"
        }

        return licenseUrl?.let { "$licenseName\n$it" } ?: licenseName
    }

    /**
     * Convert list of Jamendo tracks to Songs
     */
    fun toSongs(tracks: List<JamendoTrack>, preferredQuality: String = "mp32"): List<Song> {
        return tracks.map { toSong(it, preferredQuality) }
    }
}


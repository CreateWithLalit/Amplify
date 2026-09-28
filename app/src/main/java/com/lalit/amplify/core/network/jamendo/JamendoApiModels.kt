package com.lalit.amplify.core.network.jamendo

import com.google.gson.annotations.SerializedName

/**
 * Jamendo API Response Models
 * https://developer.jamendo.com/v3.0
 */

// Main track response wrapper
data class JamendoTracksResponse(
    @SerializedName("results")
    val results: List<JamendoTrack> = emptyList(),

    @SerializedName("headers")
    val headers: JamendoHeaders? = null
)

// Pagination headers
data class JamendoHeaders(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("code")
    val code: Int = 0,

    @SerializedName("error_message")
    val errorMessage: String? = null,

    @SerializedName("results_count")
    val resultsCount: Int = 0,

    @SerializedName("next")
    val next: String? = null
)

// Individual track object
data class JamendoTrack(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("duration")
    val duration: Int = 0,  // in seconds

    @SerializedName("artist_name")
    val artistName: String = "",

    @SerializedName("artist_id")
    val artistId: String = "",

    @SerializedName("album_name")
    val albumName: String = "",

    @SerializedName("album_id")
    val albumId: String = "",

    @SerializedName("image")
    val imageUrl: String? = null,

    @SerializedName("audio")
    val audioUrl: String? = null,  // Legacy - single stream URL

    @SerializedName("audiodownload")
    val audioDownloadUrl: String? = null,

    @SerializedName("audiodownload_allowed")
    val audioDownloadAllowed: Boolean = false,

    @SerializedName("audiodownloadformat")
    val audioDownloadFormats: Map<String, String>? = null,  // Format: {"mp32": "url", "ogg": "url", etc}

    @SerializedName("audiostream")
    val audioStreams: Map<String, String>? = null,  // Format: {"mp32": "url", "ogg": "url", etc}

    @SerializedName("license_ccurl")
    val licenseUrl: String? = null,

    @SerializedName("license_creativecommons")
    val licenseType: String? = null,  // e.g., "by", "by-sa", "by-nc", "by-nd", "by-nc-sa", "by-nc-nd"

    @SerializedName("tags")
    val tags: List<String>? = emptyList(),

    @SerializedName("shareurl")
    val shareUrl: String? = null
)

// Album response
data class JamendoAlbumsResponse(
    @SerializedName("results")
    val results: List<JamendoAlbum> = emptyList(),

    @SerializedName("headers")
    val headers: JamendoHeaders? = null
)

data class JamendoAlbum(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("artist_name")
    val artistName: String = "",

    @SerializedName("artist_id")
    val artistId: String = "",

    @SerializedName("image")
    val imageUrl: String? = null,

    @SerializedName("releasedate")
    val releaseDate: String? = null
)

// Radios response (for optional radio support)
data class JamendoRadiosResponse(
    @SerializedName("results")
    val results: List<JamendoRadio> = emptyList(),

    @SerializedName("headers")
    val headers: JamendoHeaders? = null
)

data class JamendoRadio(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("image")
    val imageUrl: String? = null
)


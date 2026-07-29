package com.lalit.amplify.feature.search

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents a single internet music search result.
 * Source-agnostic - works with any API backend you plug in later.
 */
data class MusicSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,           // milliseconds, 0 if unknown
    val thumbnailUrl: String?,
    val sourceLabel: String,      // e.g. "YouTube", "SoundCloud"
    val webUrl: String            // URL to the source page or stream
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        title = parcel.readString()!!,
        artist = parcel.readString()!!,
        duration = parcel.readLong(),
        thumbnailUrl = parcel.readString(),
        sourceLabel = parcel.readString()!!,
        webUrl = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeLong(duration)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(sourceLabel)
        parcel.writeString(webUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MusicSearchResult> {
        override fun createFromParcel(parcel: Parcel): MusicSearchResult = MusicSearchResult(parcel)
        override fun newArray(size: Int): Array<MusicSearchResult?> = arrayOfNulls(size)
    }
}

/**
 * Extended search result with downloadable stream info.
 * Populated when user taps "Get Download Link".
 */
data class DownloadableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val thumbnailUrl: String?,
    val sourceLabel: String,
    val webUrl: String,
    val streamUrl: String,        // Direct audio stream URL
    val audioQuality: String,     // e.g. "320kbps", "128kbps"
    val fileExtension: String = "mp3",
    val contentType: String = "audio/mpeg"
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        title = parcel.readString()!!,
        artist = parcel.readString()!!,
        duration = parcel.readLong(),
        thumbnailUrl = parcel.readString(),
        sourceLabel = parcel.readString()!!,
        webUrl = parcel.readString()!!,
        streamUrl = parcel.readString()!!,
        audioQuality = parcel.readString()!!,
        fileExtension = parcel.readString()!!,
        contentType = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeLong(duration)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(sourceLabel)
        parcel.writeString(webUrl)
        parcel.writeString(streamUrl)
        parcel.writeString(audioQuality)
        parcel.writeString(fileExtension)
        parcel.writeString(contentType)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DownloadableTrack> {
        override fun createFromParcel(parcel: Parcel): DownloadableTrack = DownloadableTrack(parcel)
        override fun newArray(size: Int): Array<DownloadableTrack?> = arrayOfNulls(size)
    }
}


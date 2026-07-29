package com.lalit.amplify.core.database.mapper

import android.net.Uri
import com.lalit.amplify.core.database.entity.SongEntity
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource

fun SongEntity.toSong(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uri = Uri.parse(uri),
        albumArtUri = albumArtUri?.let { Uri.parse(it) },
        source = SongSource.valueOf(source)
    )
}

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uri = uri.toString(),
        albumArtUri = albumArtUri?.toString(),
        source = source.name
    )
}

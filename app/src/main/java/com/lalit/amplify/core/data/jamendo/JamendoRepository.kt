package com.lalit.amplify.core.data.jamendo

import android.content.Context
import android.net.Uri
import com.lalit.amplify.core.database.AmplifyDatabase
import com.lalit.amplify.core.database.entity.JamendoFavoriteEntity
import com.lalit.amplify.core.database.entity.JamendoRecentlyPlayedEntity
import com.lalit.amplify.core.database.entity.JamendoQualityPreferenceEntity
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.Room
import kotlinx.coroutines.runBlocking

/**
 * Repository for managing Jamendo-specific data
 * Handles favorites, recently played, search history, and quality preferences
 */
class JamendoRepository(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        AmplifyDatabase::class.java,
        AmplifyDatabase.DATABASE_NAME
    )
        .addMigrations(AmplifyDatabase.MIGRATION_1_2)
        .build()

    private val favoritesDao = database.jamendoFavoritesDao()
    private val recentlyPlayedDao = database.jamendoRecentlyPlayedDao()
    private val qualityDao = database.jamendoQualityPreferenceDao()

    // ============ FAVORITES ============

    fun getFavorites(): Flow<List<JamendoFavoriteEntity>> {
        return favoritesDao.getAllFavorites()
    }

    fun getFavoriteIds(): Flow<Set<Long>> = favoritesDao.getFavoriteIds().map { it.toSet() }

    fun getFavoriteSongs(): Flow<List<Song>> = getFavorites().map { favorites ->
        favorites.map { favorite ->
            Song(
                id = favorite.jamendoTrackId,
                title = favorite.title,
                artist = favorite.artist,
                album = favorite.album,
                duration = favorite.duration,
                uri = favorite.streamUrl?.let(Uri::parse) ?: Uri.EMPTY,
                albumArtUri = favorite.imageUrl?.let(Uri::parse),
                source = SongSource.JAMENDO,
                streamUrl = favorite.streamUrl,
                sourceUrl = favorite.sourceUrl,
                artistUrl = favorite.artistUrl,
                licenseInfo = favorite.licenseInfo,
                genres = favorite.genres?.split(',')?.filter(String::isNotBlank).orEmpty()
            )
        }
    }

    suspend fun addFavorite(song: Song) {
        val favorite = JamendoFavoriteEntity(
            jamendoTrackId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            imageUrl = song.albumArtUri?.toString(),
            streamUrl = song.streamUrl,
            licenseInfo = song.licenseInfo,
            sourceUrl = song.sourceUrl,
            artistUrl = song.artistUrl,
            genres = song.genres.joinToString(",")
        )
        favoritesDao.addFavorite(favorite)
    }

    suspend fun removeFavorite(trackId: Long) {
        favoritesDao.removeFavoriteById(trackId)
    }

    suspend fun isFavorite(trackId: Long): Boolean {
        return favoritesDao.isFavorite(trackId) > 0
    }

    // ============ RECENTLY PLAYED ============

    fun getRecentlyPlayed(): Flow<List<JamendoRecentlyPlayedEntity>> {
        return recentlyPlayedDao.getRecentlyPlayed()
    }

    fun getRecentlyPlayedSongs(): Flow<List<Song>> = getRecentlyPlayed().map { tracks ->
        tracks.map { track ->
            Song(
                id = track.jamendoTrackId,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration = track.duration,
                uri = track.streamUrl?.let(Uri::parse) ?: Uri.EMPTY,
                albumArtUri = track.imageUrl?.let(Uri::parse),
                source = SongSource.JAMENDO,
                streamUrl = track.streamUrl,
                licenseInfo = track.licenseInfo
            )
        }
    }

    suspend fun addRecentlyPlayed(song: Song) {
        val recentTrack = JamendoRecentlyPlayedEntity(
            jamendoTrackId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            imageUrl = song.albumArtUri?.toString(),
            streamUrl = song.streamUrl,
            licenseInfo = song.licenseInfo
        )
        recentlyPlayedDao.addRecentlyPlayed(recentTrack)
        // Prune old entries to keep max 50
        recentlyPlayedDao.pruneOldEntries()
    }

    suspend fun clearRecentlyPlayed() {
        recentlyPlayedDao.clearHistory()
    }

    // ============ AUDIO QUALITY ============

    fun getQualityPreference(): Flow<String> {
        return qualityDao.getQuality().map { it ?: "mp32" }
    }

    fun getQualityPreferenceSync(): String {
        return runBlocking(Dispatchers.IO) {
            qualityDao.getQualitySync() ?: "mp32"
        }
    }

    suspend fun setQualityPreference(quality: String) {
        qualityDao.setQuality(
            JamendoQualityPreferenceEntity(quality = quality)
        )
    }

    companion object {
        private var instance: JamendoRepository? = null

        fun getInstance(context: Context): JamendoRepository {
            return instance ?: JamendoRepository(context).also { instance = it }
        }

        // Supported quality options
        val QUALITY_OPTIONS = listOf(
            "mp32" to "MP3 VBR (Variable)",
            "mp31" to "MP3 96 kbps",
            "ogg" to "OGG Vorbis",
            "flac" to "FLAC (Lossless)"
        )
    }
}


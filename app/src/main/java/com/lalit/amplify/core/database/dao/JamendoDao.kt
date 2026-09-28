package com.lalit.amplify.core.database.dao

import androidx.room.*
import com.lalit.amplify.core.database.entity.JamendoFavoriteEntity
import com.lalit.amplify.core.database.entity.JamendoRecentlyPlayedEntity
import com.lalit.amplify.core.database.entity.JamendoQualityPreferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Jamendo favorites DAO
 */
@Dao
interface JamendoFavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: JamendoFavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: JamendoFavoriteEntity)

    @Query("DELETE FROM jamendo_favorites WHERE jamendoTrackId = :trackId")
    suspend fun removeFavoriteById(trackId: Long)

    @Query("SELECT * FROM jamendo_favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<JamendoFavoriteEntity>>

    @Query("SELECT jamendoTrackId FROM jamendo_favorites")
    fun getFavoriteIds(): Flow<List<Long>>

    @Query("SELECT * FROM jamendo_favorites WHERE jamendoTrackId = :trackId")
    suspend fun getFavoriteById(trackId: Long): JamendoFavoriteEntity?

    @Query("SELECT COUNT(*) FROM jamendo_favorites WHERE jamendoTrackId = :trackId")
    suspend fun isFavorite(trackId: Long): Int
}

/**
 * Jamendo recently played DAO
 */
@Dao
interface JamendoRecentlyPlayedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentlyPlayed(track: JamendoRecentlyPlayedEntity)

    @Delete
    suspend fun removeRecentlyPlayed(track: JamendoRecentlyPlayedEntity)

    @Query("DELETE FROM jamendo_recently_played")
    suspend fun clearHistory()

    @Query("SELECT * FROM jamendo_recently_played ORDER BY playedAt DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<JamendoRecentlyPlayedEntity>>

    @Query("DELETE FROM jamendo_recently_played WHERE playedAt < (SELECT playedAt FROM jamendo_recently_played ORDER BY playedAt DESC LIMIT 1 OFFSET 50)")
    suspend fun pruneOldEntries()
}

/**
 * Jamendo quality preference DAO
 */
@Dao
interface JamendoQualityPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setQuality(preference: JamendoQualityPreferenceEntity)

    @Query("SELECT quality FROM jamendo_quality_preference WHERE preferenceKey = 'quality'")
    fun getQuality(): Flow<String?>

    @Query("SELECT quality FROM jamendo_quality_preference WHERE preferenceKey = 'quality' LIMIT 1")
    suspend fun getQualitySync(): String?
}


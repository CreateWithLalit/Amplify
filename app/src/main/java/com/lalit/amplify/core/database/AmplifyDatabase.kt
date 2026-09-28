package com.lalit.amplify.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.lalit.amplify.core.database.dao.*
import com.lalit.amplify.core.database.entity.*

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentPlayEntity::class,
        SearchHistoryEntity::class,
        DownloadHistoryEntity::class,
        JamendoFavoriteEntity::class,
        JamendoRecentlyPlayedEntity::class,
        JamendoQualityPreferenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AmplifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentDao(): RecentDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun jamendoFavoritesDao(): JamendoFavoritesDao
    abstract fun jamendoRecentlyPlayedDao(): JamendoRecentlyPlayedDao
    abstract fun jamendoQualityPreferenceDao(): JamendoQualityPreferenceDao

    companion object {
        const val DATABASE_NAME = "amplify_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS jamendo_favorites (
                        jamendoTrackId INTEGER NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL,
                        duration INTEGER NOT NULL, imageUrl TEXT, streamUrl TEXT,
                        licenseInfo TEXT, sourceUrl TEXT, artistUrl TEXT, genres TEXT,
                        addedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS jamendo_recently_played (
                        jamendoTrackId INTEGER NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL,
                        duration INTEGER NOT NULL, imageUrl TEXT, streamUrl TEXT,
                        licenseInfo TEXT, playedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS jamendo_quality_preference (
                        preferenceKey TEXT NOT NULL PRIMARY KEY, quality TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

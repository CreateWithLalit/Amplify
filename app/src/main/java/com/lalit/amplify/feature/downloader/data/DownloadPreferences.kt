package com.lalit.amplify.feature.downloader.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lalit.amplify.feature.downloader.model.DownloadQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.downloadDataStore by preferencesDataStore(name = "amplify_download_prefs")

/**
 * DataStore-backed preferences for download settings.
 * Integrated with existing AmplifyDataStore pattern.
 */
class DownloadPreferences(private val context: Context) {

    companion object {
        private val DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
        private val DOWNLOAD_FOLDER_NAME = stringPreferencesKey("download_folder_name")
        private val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        private val AUTO_IMPORT = booleanPreferencesKey("auto_import_downloads")
    }

    val downloadFolderUri: Flow<Uri?> = context.downloadDataStore.data.map { prefs ->
        prefs[DOWNLOAD_FOLDER_URI]?.let { Uri.parse(it) }
    }

    val downloadFolderName: Flow<String> = context.downloadDataStore.data.map { prefs ->
        prefs[DOWNLOAD_FOLDER_NAME] ?: "Music/Amplify"
    }

    val defaultQuality: Flow<DownloadQuality> = context.downloadDataStore.data.map { prefs ->
        when (prefs[DEFAULT_QUALITY]) {
            "high" -> DownloadQuality.HIGH
            "medium" -> DownloadQuality.MEDIUM
            "low" -> DownloadQuality.LOW
            else -> DownloadQuality.HIGH
        }
    }

    val autoImportDownloads: Flow<Boolean> = context.downloadDataStore.data.map { prefs ->
        prefs[AUTO_IMPORT] ?: true
    }

    suspend fun setDownloadFolder(uri: Uri, name: String) {
        context.downloadDataStore.edit { prefs ->
            prefs[DOWNLOAD_FOLDER_URI] = uri.toString()
            prefs[DOWNLOAD_FOLDER_NAME] = name
        }
    }

    suspend fun setDefaultQuality(quality: DownloadQuality) {
        context.downloadDataStore.edit { prefs ->
            prefs[DEFAULT_QUALITY] = when (quality) {
                DownloadQuality.HIGH -> "high"
                DownloadQuality.MEDIUM -> "medium"
                DownloadQuality.LOW -> "low"
            }
        }
    }

    suspend fun setAutoImport(enabled: Boolean) {
        context.downloadDataStore.edit { prefs ->
            prefs[AUTO_IMPORT] = enabled
        }
    }

    suspend fun clearDownloadFolder() {
        context.downloadDataStore.edit { prefs ->
            prefs.remove(DOWNLOAD_FOLDER_URI)
            prefs.remove(DOWNLOAD_FOLDER_NAME)
        }
    }
}


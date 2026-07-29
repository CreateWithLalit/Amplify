package com.lalit.amplify.feature.downloader

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lalit.amplify.feature.downloader.data.DownloadPreferences
import com.lalit.amplify.feature.downloader.engine.AmplifyDownloadManager
import com.lalit.amplify.feature.downloader.model.DownloadQuality
import com.lalit.amplify.feature.downloader.model.DownloadState
import com.lalit.amplify.feature.downloader.model.DownloadTask
import com.lalit.amplify.feature.downloader.model.DuplicateStrategy
import com.lalit.amplify.feature.search.DownloadableTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val downloadManager = AmplifyDownloadManager(context)
    private val preferences = DownloadPreferences(context)
    private val ytResolver = com.lalit.amplify.feature.downloader.data.YouTubeResolverRepository()

    val downloadState: StateFlow<DownloadState> = downloadManager.downloadState

    private val _track = MutableStateFlow<DownloadableTrack?>(null)
    val track: StateFlow<DownloadableTrack?> = _track.asStateFlow()

    private val _manualUrl = MutableStateFlow("")
    val manualUrl: StateFlow<String> = _manualUrl.asStateFlow()

    private val _ytUrl = MutableStateFlow("")
    val ytUrl: StateFlow<String> = _ytUrl.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolveError = MutableStateFlow<String?>(null)
    val resolveError: StateFlow<String?> = _resolveError.asStateFlow()

    private val _destinationUri = MutableStateFlow<Uri?>(null)
    val destinationUri: StateFlow<Uri?> = _destinationUri.asStateFlow()

    private val _destinationName = MutableStateFlow("Music/Amplify")
    val destinationName: StateFlow<String> = _destinationName.asStateFlow()

    private val _defaultQuality = MutableStateFlow(DownloadQuality.HIGH)
    val defaultQuality: StateFlow<DownloadQuality> = _defaultQuality.asStateFlow()

    private val _autoImport = MutableStateFlow(true)
    val autoImport: StateFlow<Boolean> = _autoImport.asStateFlow()

    private val _duplicateCheckResult = MutableStateFlow<Boolean?>(null)
    val duplicateCheckResult: StateFlow<Boolean?> = _duplicateCheckResult.asStateFlow()

    private val _showDuplicateDialog = MutableStateFlow(false)
    val showDuplicateDialog: StateFlow<Boolean> = _showDuplicateDialog.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.downloadFolderUri.collect { uri ->
                _destinationUri.value = uri
            }
        }
        viewModelScope.launch {
            preferences.downloadFolderName.collect { name ->
                _destinationName.value = name
            }
        }
        viewModelScope.launch {
            preferences.defaultQuality.collect { quality ->
                _defaultQuality.value = quality
            }
        }
        viewModelScope.launch {
            preferences.autoImportDownloads.collect { enabled ->
                _autoImport.value = enabled
            }
        }
    }

    fun setTrack(track: DownloadableTrack) {
        _track.value = track
    }

    fun setManualUrl(url: String) {
        _manualUrl.value = url
    }

    fun setYtUrl(url: String) {
        _ytUrl.value = url
        _resolveError.value = null
    }

    fun resolveYouTubeUrl() {
        val url = _ytUrl.value
        if (url.isBlank()) return

        viewModelScope.launch {
            _isResolving.value = true
            _resolveError.value = null
            
            ytResolver.resolveYouTubeUrl(url).fold(
                onSuccess = { resolvedTrack ->
                    _track.value = resolvedTrack
                    _isResolving.value = false
                },
                onFailure = { error ->
                    _resolveError.value = error.message ?: "Failed to resolve link"
                    _isResolving.value = false
                }
            )
        }
    }

    fun updateTrackMetadata(title: String, artist: String) {
        _track.value = _track.value?.copy(title = title, artist = artist)
    }

    fun setDestinationUri(uri: Uri, name: String) {
        viewModelScope.launch {
            preferences.setDownloadFolder(uri, name)
        }
    }

    fun clearDestination() {
        viewModelScope.launch {
            preferences.clearDownloadFolder()
        }
    }

    fun setDefaultQuality(quality: DownloadQuality) {
        viewModelScope.launch {
            preferences.setDefaultQuality(quality)
        }
    }

    fun setAutoImport(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoImport(enabled)
        }
    }

    /**
     * Check if the file already exists before starting download.
     */
    fun checkDuplicate() {
        val currentTrack = _track.value
        val manual = _manualUrl.value
        if (currentTrack == null && manual.isBlank()) return
        
        val destUri = _destinationUri.value

        viewModelScope.launch {
            val fileName = if (currentTrack != null) {
                generateFileName(currentTrack)
            } else {
                "manual_download_${System.currentTimeMillis()}.mp3" // Just a placeholder, manual doesn't really check duplicates well without a fixed name
            }

            val exists = if (destUri != null) {
                val parent = DocumentFile.fromTreeUri(context, destUri)
                parent?.findFile(fileName) != null
            } else {
                val dir = File(context.getExternalFilesDir(null), "Music/Amplify")
                File(dir, fileName).exists()
            }

            _duplicateCheckResult.value = exists
            if (exists) {
                _showDuplicateDialog.value = true
            }
        }
    }

    fun dismissDuplicateDialog() {
        _showDuplicateDialog.value = false
        _duplicateCheckResult.value = null
    }

    /**
     * Start the download with the chosen duplicate strategy.
     */
    fun startDownload(strategy: DuplicateStrategy = DuplicateStrategy.KEEP_BOTH) {
        val currentTrack = _track.value
        val manual = _manualUrl.value
        
        if (currentTrack == null && manual.isBlank()) return
        
        val destUri = resolveDestinationUri()

        val task = if (currentTrack != null) {
            DownloadTask(
                id = currentTrack.id,
                trackTitle = currentTrack.title,
                trackArtist = currentTrack.artist,
                streamUrl = currentTrack.streamUrl,
                requestBody = "{\"url\":\"${currentTrack.webUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\",\"quality\":\"${_defaultQuality.value.name.lowercase()}\"}",
                thumbnailUrl = currentTrack.thumbnailUrl,
                fileName = generateFileName(currentTrack),
                fileExtension = currentTrack.fileExtension,
                contentType = currentTrack.contentType,
                destinationUri = destUri,
                audioQuality = currentTrack.audioQuality
            )
        } else {
            // Manual URL flow
            val fileName = "manual_download_${System.currentTimeMillis()}.mp3"
            DownloadTask(
                id = System.currentTimeMillis().toString(),
                trackTitle = "Manual Download",
                trackArtist = "Unknown Artist",
                streamUrl = manual,
                thumbnailUrl = null,
                fileName = fileName,
                fileExtension = "mp3",
                contentType = "audio/mpeg",
                destinationUri = destUri,
                audioQuality = "Unknown"
            )
        }

        viewModelScope.launch {
            downloadManager.download(task, strategy).fold(
                onSuccess = { fileUri ->
                    // Finalize MediaStore entry if applicable
                    if (destUri.toString().contains(MediaStore.AUTHORITY)) {
                        AmplifyDownloadManager.finalizeMediaStoreEntry(context, fileUri)
                    }

                    // Log and sanity-check the saved file
                    try {
                        val mime = context.contentResolver.getType(fileUri)
                        Log.d("AmplifyDownload", "Downloaded fileUri=$fileUri mime=$mime")
                        // Attempt to open input stream to ensure readability
                        context.contentResolver.openInputStream(fileUri)?.use { stream ->
                            val probe = ByteArray(8)
                            val read = stream.read(probe)
                            Log.d("AmplifyDownload", "Read $read bytes from downloaded file: ${probe.joinToString()}")
                        }
                    } catch (e: Exception) {
                        Log.e("AmplifyDownload", "Failed to probe downloaded file", e)
                    }

                    // Auto-import to library if enabled
                    if (_autoImport.value) {
                        if (currentTrack != null) {
                            importToLibrary(fileUri, currentTrack)
                        }
                    }
                },
                onFailure = { /* Error state handled by downloadManager */ }
            )
        }
    }

    fun cancelDownload() {
        downloadManager.cancel()
    }

    fun reset() {
        downloadManager.reset()
        _track.value = null
        _manualUrl.value = ""
        _duplicateCheckResult.value = null
        _showDuplicateDialog.value = false
    }

    private fun resolveDestinationUri(): Uri {
        val track = _track.value
        val destUri = _destinationUri.value

        return when {
            // User selected SAF folder
            destUri != null -> destUri

            // Fallback: MediaStore public Music folder (no permission needed on Android 10+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val fileName = track?.let { generateFileName(it) } ?: "download.mp3"
                AmplifyDownloadManager.createMediaStoreEntry(
                    context,
                    fileName,
                    track?.title ?: "Unknown",
                    track?.artist ?: "Unknown Artist"
                ) ?: fallbackInternalUri()
            }

            // Fallback: internal app storage
            else -> fallbackInternalUri()
        }
    }

    private fun fallbackInternalUri(): Uri {
        val dir = File(context.getExternalFilesDir(null), "Music/Amplify")
        if (!dir.exists()) dir.mkdirs()
        return Uri.fromFile(dir)
    }

    private fun generateFileName(track: DownloadableTrack): String {
        val safeTitle = track.title.replace(Regex("[^a-zA-Z0-9\\s\\-_.]"), "_").take(50)
        val safeArtist = track.artist.replace(Regex("[^a-zA-Z0-9\\s\\-_.]"), "_").take(30)
        return "${safeArtist} - ${safeTitle}.${track.fileExtension}"
    }

    /**
     * Import downloaded file into Amplify library via MediaStore scan.
     */
    private fun importToLibrary(fileUri: Uri, track: DownloadableTrack) {
        // Trigger MediaStore scan so the file appears in the library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, the file is already in MediaStore if we used that path
            // Otherwise, we need to scan it
            if (!fileUri.toString().contains(MediaStore.AUTHORITY)) {
                scanFile(fileUri)
            }
        } else {
            scanFile(fileUri)
        }
    }

    private fun scanFile(uri: Uri) {
        val path = when (uri.scheme) {
            "file" -> uri.path
            else -> null
        }
        path?.let {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(it),
                arrayOf("audio/mpeg"),
                null
            )
        }
    }
}


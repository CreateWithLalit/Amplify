package com.lalit.amplify.feature.downloader.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.lalit.amplify.feature.downloader.model.DownloadState
import com.lalit.amplify.feature.downloader.model.DownloadTask
import com.lalit.amplify.feature.downloader.model.DuplicateStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Core download engine for AMPLIFY.
 * Downloads audio files using OkHttp and writes to SAF-backed destinations.
 *
 * Supports:
 * - Internal app storage (fallback)
 * - SAF user-selected folders
 * - SD card via DocumentFile (if user granted tree URI)
 * - MediaStore for public Music directory
 */
class AmplifyDownloadManager(private val context: Context) {

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentTask: DownloadTask? = null
    private var isCancelled = false

    /**
     * Start a download.
     * @param task The download task
     * @param duplicateStrategy How to handle existing files
     * @return Result containing the final file URI on success
     */
    suspend fun download(
        task: DownloadTask,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.KEEP_BOTH
    ): Result<Uri> = withContext(Dispatchers.IO) {
        isCancelled = false
        currentTask = task
        _downloadState.value = DownloadState.Preparing

        try {
            // Resolve destination and handle duplicates
            val destinationUri = resolveDestination(task, duplicateStrategy)
                ?: return@withContext Result.failure(IOException("Could not resolve destination"))

            if (isCancelled) {
                _downloadState.value = DownloadState.Cancelled
                return@withContext Result.failure(IOException("Download cancelled"))
            }

            // Execute download
            val requestBuilder = Request.Builder()
                .url(task.streamUrl)
                .header("User-Agent", "Amplify Android")

            if (task.requestBody != null) {
                requestBuilder.post(
                    task.requestBody.toRequestBody("application/json".toMediaType())
                )
            } else {
                requestBuilder.get()
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                val totalBytes = body.contentLength()

                // Write to destination
                when {
                    // SAF DocumentFile destination
                    destinationUri.scheme == "content" &&
                        !destinationUri.toString().contains(MediaStore.AUTHORITY) -> {
                        writeToDocumentFile(destinationUri, body.byteStream(), totalBytes)
                    }
                    // MediaStore destination (public Music folder)
                    destinationUri.scheme == "content" -> {
                        writeToMediaStore(destinationUri, body.byteStream(), totalBytes)
                    }
                    // File destination (internal storage)
                    destinationUri.scheme == "file" -> {
                        writeToFile(File(destinationUri.path!!), body.byteStream(), totalBytes)
                    }
                    else -> throw IOException("Unsupported URI scheme: ${destinationUri.scheme}")
                }

                if (isCancelled) {
                    _downloadState.value = DownloadState.Cancelled
                    // Clean up partial file
                    deletePartial(destinationUri)
                    return@withContext Result.failure(IOException("Download cancelled"))
                }

                _downloadState.value = DownloadState.Success(destinationUri, task.fileName)
                Result.success(destinationUri)
            }
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    fun cancel() {
        isCancelled = true
        _downloadState.value = DownloadState.Cancelled
    }

    fun reset() {
        isCancelled = false
        currentTask = null
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Check if a file already exists at the destination.
     */
    suspend fun checkDuplicate(task: DownloadTask): Boolean = withContext(Dispatchers.IO) {
        when {
            task.destinationUri.scheme == "content" -> {
                val parent = DocumentFile.fromTreeUri(context, task.destinationUri)
                    ?: return@withContext false
                parent.findFile(task.fileName) != null
            }
            else -> false
        }
    }

    // ─── Internal helpers ──────────────────────────────────────────────────

    private fun resolveDestination(task: DownloadTask, strategy: DuplicateStrategy): Uri? {
        return when {
            // SAF tree URI
            task.destinationUri.scheme == "content" &&
                !task.destinationUri.toString().contains(MediaStore.AUTHORITY) -> {
                val parent = DocumentFile.fromTreeUri(context, task.destinationUri)
                    ?: return null

                val existing = parent.findFile(task.fileName)
                when {
                    existing == null -> {
                        // Create new file
                        val mime = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(task.fileExtension) ?: task.contentType
                        val newFile = parent.createFile(mime, task.fileName)
                        newFile?.uri
                    }
                    strategy == DuplicateStrategy.REPLACE -> {
                        // Delete existing and recreate
                        existing.delete()
                        val mime = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(task.fileExtension) ?: task.contentType
                        parent.createFile(mime, task.fileName)?.uri
                    }
                    strategy == DuplicateStrategy.KEEP_BOTH -> {
                        val newName = generateUniqueName(parent, task.fileName)
                        val mime = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(task.fileExtension) ?: task.contentType
                        parent.createFile(mime, newName)?.uri
                    }
                    else -> null // CANCEL
                }
            }
            // MediaStore
            task.destinationUri.scheme == "content" -> {
                task.destinationUri // Already resolved by MediaStore insert
            }
            // Internal file
            else -> {
                val file = File(task.destinationUri.path!!, task.fileName)
                if (file.exists() && strategy == DuplicateStrategy.KEEP_BOTH) {
                    val parent = file.parentFile ?: return null
                    File(parent, generateUniqueNameForFile(parent, task.fileName)).toUri()
                } else {
                    file.toUri()
                }
            }
        }
    }

    private fun writeToDocumentFile(uri: Uri, inputStream: java.io.InputStream, totalBytes: Long) {
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) break
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val progress = if (totalBytes > 0) {
                    ((totalRead * 100) / totalBytes).toInt()
                } else 0
                _downloadState.value = DownloadState.Downloading(
                    progressPercent = progress,
                    bytesDownloaded = totalRead,
                    totalBytes = totalBytes
                )
            }
        } ?: throw IOException("Failed to open output stream")
    }

    private fun writeToMediaStore(uri: Uri, inputStream: java.io.InputStream, totalBytes: Long) {
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) break
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val progress = if (totalBytes > 0) {
                    ((totalRead * 100) / totalBytes).toInt()
                } else 0
                _downloadState.value = DownloadState.Downloading(
                    progressPercent = progress,
                    bytesDownloaded = totalRead,
                    totalBytes = totalBytes
                )
            }
        } ?: throw IOException("Failed to open output stream")
    }

    private fun writeToFile(file: File, inputStream: java.io.InputStream, totalBytes: Long) {
        file.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) break
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val progress = if (totalBytes > 0) {
                    ((totalRead * 100) / totalBytes).toInt()
                } else 0
                _downloadState.value = DownloadState.Downloading(
                    progressPercent = progress,
                    bytesDownloaded = totalRead,
                    totalBytes = totalBytes
                )
            }
        }
    }

    private fun deletePartial(uri: Uri) {
        try {
            if (uri.toString().contains(MediaStore.AUTHORITY)) {
                context.contentResolver.delete(uri, null, null)
            } else {
                DocumentFile.fromSingleUri(context, uri)?.delete()
            }
        } catch (_: Exception) { /* best effort */ }
    }

    private fun generateUniqueName(parent: DocumentFile, originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val name = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val ext = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var counter = 1
        var newName = "${name} ($counter)$ext"
        while (parent.findFile(newName) != null) {
            counter++
            newName = "${name} ($counter)$ext"
        }
        return newName
    }

    private fun generateUniqueNameForFile(parent: File, originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val name = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val ext = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var counter = 1
        var newName = "${name} ($counter)$ext"
        while (File(parent, newName).exists()) {
            counter++
            newName = "${name} ($counter)$ext"
        }
        return newName
    }

    companion object {
        /**
         * Create a MediaStore entry for public Music folder.
         * Returns the content URI to write to.
         */
        fun createMediaStoreEntry(
            context: Context,
            fileName: String,
            title: String,
            artist: String,
            mimeType: String = "audio/mpeg"
        ): Uri? {
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                // A unique album keeps each downloaded track's embedded cover art
                // from being replaced by the artwork of an earlier download.
                put(MediaStore.Audio.Media.ALBUM, title)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Amplify/")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            return resolver.insert(collection, contentValues)
        }

        /**
         * Mark a MediaStore entry as complete (Android 10+).
         */
        fun finalizeMediaStoreEntry(context: Context, uri: Uri) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null)
            }
        }
    }
}


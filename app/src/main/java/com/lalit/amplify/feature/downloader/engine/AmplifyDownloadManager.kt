package com.lalit.amplify.feature.downloader.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
import java.io.File
import java.io.IOException

/**
 * Core download engine for AMPLIFY.
 * Downloads audio files using OkHttp and writes to SAF-backed destinations.
 */
class AmplifyDownloadManager(private val context: Context) {

    private val client: OkHttpClient = NetworkClients.client

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentTask: DownloadTask? = null
    private var isCancelled = false

    suspend fun download(
        task: DownloadTask,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.KEEP_BOTH
    ): Result<Uri> = withContext(Dispatchers.IO) {
        isCancelled = false
        currentTask = task
        _downloadState.value = DownloadState.Preparing
        var createdDestinationUri: Uri? = null

        try {
            Log.d(TAG, "Starting download task stage=DOWNLOAD_PREPARE title='${task.trackTitle}'")
            val destinationUri = resolveDestination(task, duplicateStrategy)
                ?: return@withContext Result.failure(IOException("Could not resolve download destination folder"))
            createdDestinationUri = destinationUri

            if (isCancelled) {
                _downloadState.value = DownloadState.Cancelled
                deletePartial(destinationUri)
                return@withContext Result.failure(IOException("Download cancelled"))
            }

            val requestBuilder = Request.Builder()
                .url(task.streamUrl)
                .header("User-Agent", "Amplify-MusicPlayer/1.0 (Android; Mobile)")
                .header("Accept", "audio/*, */*")

            if (task.requestBody != null) {
                requestBuilder.post(
                    task.requestBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                )
            } else {
                requestBuilder.get()
            }

            val request = requestBuilder.build()
            Log.d(TAG, "Connecting to stream URL stage=DOWNLOAD_CONNECT")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorDetail = when (code) {
                        401 -> "Audio stream request unauthorized (HTTP 401)"
                        403 -> "Audio stream request forbidden (HTTP 403)"
                        404 -> "Audio stream source not found (HTTP 404)"
                        429 -> "Audio stream rate limited (HTTP 429)"
                        in 500..599 -> "Server Error ($code): Audio streaming backend is temporarily unavailable."
                        else -> "HTTP $code"
                    }
                    Log.w(TAG, "Stream response failed stage=DOWNLOAD_HTTP_ERROR code=$code msg=$errorDetail")
                    throw IOException(errorDetail)
                }

                val contentType = response.header("Content-Type").orEmpty()
                if (contentType.contains("text/html", ignoreCase = true)) {
                    Log.e(TAG, "Expected audio stream but server returned HTML ($contentType)")
                    throw IOException("Server returned an HTML page instead of an audio stream ($contentType)")
                }

                val body = response.body ?: throw IOException("Empty response body from audio server")
                val totalBytes = body.contentLength()
                Log.d(TAG, "Stream response received code=${response.code} length=$totalBytes stage=DOWNLOAD_STREAM_START")

                val bytesWritten = when {
                    destinationUri.scheme == "content" &&
                        !destinationUri.toString().contains(MediaStore.AUTHORITY) -> {
                        writeToDocumentFile(destinationUri, body.byteStream(), totalBytes)
                    }
                    destinationUri.scheme == "content" -> {
                        writeToMediaStore(destinationUri, body.byteStream(), totalBytes)
                    }
                    destinationUri.scheme == "file" -> {
                        writeToFile(File(destinationUri.path!!), body.byteStream(), totalBytes)
                    }
                    else -> throw IOException("Unsupported URI scheme: ${destinationUri.scheme}")
                }

                if (isCancelled) {
                    _downloadState.value = DownloadState.Cancelled
                    deletePartial(destinationUri)
                    return@withContext Result.failure(IOException("Download cancelled"))
                }

                if (bytesWritten <= 0L) {
                    deletePartial(destinationUri)
                    throw IOException("Downloaded file is empty (0 bytes received)")
                }

                if (totalBytes > 0 && bytesWritten < totalBytes) {
                    deletePartial(destinationUri)
                    throw IOException("Download incomplete: expected $totalBytes bytes, received $bytesWritten")
                }

                Log.d(TAG, "Download complete bytes=$bytesWritten stage=DOWNLOAD_SUCCESS")
                _downloadState.value = DownloadState.Success(destinationUri, task.fileName)
                Result.success(destinationUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed task='${task.trackTitle}' stage=DOWNLOAD_ERROR", e)
            createdDestinationUri?.let { deletePartial(it) }
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

    private fun resolveDestination(task: DownloadTask, strategy: DuplicateStrategy): Uri? {
        return when {
            task.destinationUri.scheme == "content" &&
                !task.destinationUri.toString().contains(MediaStore.AUTHORITY) -> {
                val parent = DocumentFile.fromTreeUri(context, task.destinationUri)
                    ?: return null

                val existing = parent.findFile(task.fileName)
                when {
                    existing == null -> {
                        val mime = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(task.fileExtension) ?: task.contentType
                        val newFile = parent.createFile(mime, task.fileName)
                        newFile?.uri
                    }
                    strategy == DuplicateStrategy.REPLACE -> {
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
                    else -> null
                }
            }
            task.destinationUri.scheme == "content" -> {
                task.destinationUri
            }
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

    private fun writeToDocumentFile(uri: Uri, inputStream: java.io.InputStream, totalBytes: Long): Long {
        var totalRead = 0L
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

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
        } ?: throw IOException("Failed to open output stream for DocumentFile")
        return totalRead
    }

    private fun writeToMediaStore(uri: Uri, inputStream: java.io.InputStream, totalBytes: Long): Long {
        var totalRead = 0L
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

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
        } ?: throw IOException("Failed to open output stream for MediaStore")
        return totalRead
    }

    private fun writeToFile(file: File, inputStream: java.io.InputStream, totalBytes: Long): Long {
        var totalRead = 0L
        file.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

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
        return totalRead
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
        private const val TAG = "AmplifyDownloadManager"

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

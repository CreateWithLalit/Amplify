package com.lalit.amplify.core.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object AmplifyMediaIndexer {
    private const val TAG = "AmplifyMediaIndexer"
    private const val RELATIVE_MUSIC_DIR = "Music/Amplify/"

    /**
     * Registers a downloaded song into MediaStore and returns the finalized MediaStore Uri,
     * or null on failure.
     *
     * Call from a coroutine. This function performs IO on Dispatchers.IO.
     */
    suspend fun registerDownloadedSong(
        context: Context,
        sourceFileUri: Uri,
        title: String,
        artist: String,
        mimeType: String,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            when (sourceFileUri.scheme) {
                "content" -> handleContentSource(context, sourceFileUri, title, artist, mimeType, fileName)
                "file" -> handleFileSource(context, sourceFileUri, title, artist, mimeType, fileName)
                else -> {
                    Log.w(TAG, "Unsupported URI scheme: ${sourceFileUri.scheme}")
                    null
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "registerDownloadedSong failed", t)
            null
        }
    }

    private fun getCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    private suspend fun handleContentSource(
        context: Context,
        sourceUri: Uri,
        title: String,
        artist: String,
        mimeType: String,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = getCollectionUri()

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_MUSIC_DIR)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val insertedUri = resolver.insert(collection, values)
        if (insertedUri == null) {
            Log.e(TAG, "Failed to insert MediaStore row")
            return@withContext null
        }

        var output: OutputStream? = null
        var input: InputStream? = null
        try {
            output = resolver.openOutputStream(insertedUri)
            if (output == null) throw IllegalStateException("Cannot open output stream for $insertedUri")

            input = resolver.openInputStream(sourceUri)
            if (input == null) throw IllegalStateException("Cannot open input stream for source $sourceUri")

            copyStream(input, output)

            // Finalize: clear IS_PENDING so MediaStore and other apps can see it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(insertedUri, finalValues, null, null)
            }

            return@withContext insertedUri
        } catch (t: Throwable) {
            // Clean up partial record
            try {
                resolver.delete(insertedUri, null, null)
            } catch (ignore: Throwable) {
                Log.w(TAG, "Failed to delete partial MediaStore entry: $insertedUri", ignore)
            }
            Log.e(TAG, "Failed to write bytes to MediaStore for $insertedUri", t)
            return@withContext null
        } finally {
            try { input?.close() } catch (_: Exception) {}
            try { output?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleFileSource(
        context: Context,
        sourceUri: Uri,
        title: String,
        artist: String,
        mimeType: String,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        // For Android Q+ prefer inserting into MediaStore (copy bytes into its row).
        // For legacy versions, use MediaScannerConnection.scanFile and query for the resulting entry.
        val filePath = sourceUri.path ?: return@withContext null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Create a content MediaStore row and write bytes from the file path into it.
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Source file does not exist: $filePath")
                return@withContext null
            }

            val resolver = context.contentResolver
            val collection = getCollectionUri()
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_MUSIC_DIR)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val insertedUri = resolver.insert(collection, values)
            if (insertedUri == null) {
                Log.e(TAG, "Failed to insert MediaStore row for file source")
                return@withContext null
            }

            var output: OutputStream? = null
            var input: InputStream? = null
            try {
                output = resolver.openOutputStream(insertedUri)
                if (output == null) throw IllegalStateException("Cannot open output stream for $insertedUri")
                input = file.inputStream()
                copyStream(input, output)

                val finalValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(insertedUri, finalValues, null, null)
                return@withContext insertedUri
            } catch (t: Throwable) {
                try { resolver.delete(insertedUri, null, null) } catch (_: Throwable) {}
                Log.e(TAG, "Failed copying file bytes to MediaStore", t)
                return@withContext null
            } finally {
                try { input?.close() } catch (_: Exception) {}
                try { output?.close() } catch (_: Exception) {}
            }
        } else {
            // Legacy: the file is on disk. Tell MediaScanner to index it, then query MediaStore for the path.
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Source file does not exist (legacy): $filePath")
                return@withContext null
            }

            val completed = scanFileAndAwait(context, file.absolutePath, mimeType)
            if (!completed) {
                Log.w(TAG, "MediaScanner did not report completion for $filePath")
            }

            // Query MediaStore for the newly scanned item. On < Q the DATA column is available.
            val resolver = context.contentResolver
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.MediaColumns.DATA} = ?"
            val selectionArgs = arrayOf(file.absolutePath)
            resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        return@withContext ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    }
                }

            // If not found, try inserting a row (fallback) by copying file into public Music dir.
            try {
                val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Amplify")
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = uniqueFile(destDir, fileName)
                file.copyTo(destFile, overwrite = false)
                scanFileAndAwait(context, destFile.absolutePath, mimeType)
                // Query destFile
                resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(destFile.absolutePath), null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                            return@withContext ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        }
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Legacy fallback copy failed", t)
            }

            return@withContext null
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(16 * 1024)
        var read: Int
        while (true) {
            read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        output.flush()
    }

    private suspend fun scanFileAndAwait(context: Context, path: String, mimeType: String?): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                var completed = false
                val latch = java.util.concurrent.CountDownLatch(1)
                MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType)) { _, _ ->
                    completed = true
                    latch.countDown()
                }
                // Wait a short time for the scanner to complete
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                completed
            } catch (t: Throwable) {
                Log.w(TAG, "MediaScanner failed", t)
                false
            }
        }

    private fun uniqueFile(dir: File, desiredName: String): File {
        var file = File(dir, desiredName)
        if (!file.exists()) return file
        val base = desiredName.substringBeforeLast('.')
        val ext = desiredName.substringAfterLast('.', "")
        var idx = 1
        while (file.exists()) {
            val name = if (ext.isNotEmpty()) "${base}_$idx.$ext" else "${base}_$idx"
            file = File(dir, name)
            idx++
        }
        return file
    }
}

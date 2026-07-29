package com.lalit.amplify.feature.downloader

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lalit.amplify.feature.downloader.model.DownloadQuality
import com.lalit.amplify.feature.downloader.model.DownloadState
import com.lalit.amplify.feature.downloader.model.DuplicateStrategy
import com.lalit.amplify.feature.search.DownloadableTrack
import com.lalit.amplify.ui.theme.AmplifyTheme

class DownloaderActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val docFile = DocumentFile.fromTreeUri(this, it)
            viewModel.setDestinationUri(it, docFile?.name ?: "Selected Folder")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DOWNLOADABLE_TRACK, DownloadableTrack::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DOWNLOADABLE_TRACK)
        }
        track?.let { viewModel.setTrack(it) }

        setContent {
            AmplifyTheme {
                DownloaderScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onPickFolder = { folderPickerLauncher.launch(null) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.reset()
    }

    companion object {
        const val EXTRA_DOWNLOADABLE_TRACK = "extra_downloadable_track"
    }
}

@Composable
fun DownloadScreen(viewModel: DownloadViewModel = viewModel()) {
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val folder = DocumentFile.fromTreeUri(context, it)
            viewModel.setDestinationUri(it, folder?.name ?: "Selected folder")
        }
    }
    DownloaderScreen(
        viewModel = viewModel,
        onBack = { viewModel.reset() },
        onPickFolder = { folderPicker.launch(null) },
        showBack = false
    )
}

@Composable
fun DownloaderScreen(
    viewModel: DownloadViewModel,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    showBack: Boolean = true
) {
    val track by viewModel.track.collectAsState()
    val manualUrl by viewModel.manualUrl.collectAsState()
    val ytUrl by viewModel.ytUrl.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val resolveError by viewModel.resolveError.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val destinationUri by viewModel.destinationUri.collectAsState()
    val destinationName by viewModel.destinationName.collectAsState()
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val showDuplicateDialog by viewModel.showDuplicateDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack) IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Downloader",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // URL Input Section
            if (track == null) {
                YouTubeInputSection(
                    url = ytUrl,
                    onUrlChange = { viewModel.setYtUrl(it) },
                    onResolve = { viewModel.resolveYouTubeUrl() },
                    isLoading = isResolving,
                    error = resolveError
                )

            } else {
                MetadataPreviewCard(
                    track = track!!,
                    onTitleChange = { viewModel.updateTrackMetadata(it, track!!.artist) },
                    onArtistChange = { viewModel.updateTrackMetadata(track!!.title, it) },
                    onClear = { viewModel.reset() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Destination Folder
            Text(
                text = "Destination",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A))
                    .clickable { onPickFolder() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destinationName,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (destinationUri == null) {
                        Text(
                            text = "Tap to select folder",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                    }
                }
                if (destinationUri != null) {
                    IconButton(
                        onClick = { viewModel.clearDestination() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quality Selector
            Text(
                text = "Quality",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadQuality.values().forEach { quality ->
                    QualityChip(
                        label = quality.label,
                        selected = defaultQuality == quality,
                        onClick = { viewModel.setDefaultQuality(quality) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress / Status Area
            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    // Action Buttons
                    Button(
                        onClick = {
                            viewModel.checkDuplicate()
                            // If no duplicate, start download
                            if (viewModel.duplicateCheckResult.value != true) {
                                viewModel.startDownload(DuplicateStrategy.KEEP_BOTH)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start Download",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                is DownloadState.Preparing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1DB954),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Preparing download...",
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                    }
                }
                is DownloadState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.progressPercent}%",
                                color = Color(0xFF1DB954),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatBytes(state.bytesDownloaded) + " / " + formatBytes(state.totalBytes),
                                color = Color(0xFF666666),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF1DB954),
                            trackColor = Color(0xFF2A2A2A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.cancelDownload() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A2A2A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
                is DownloadState.Processing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1DB954),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            state.message,
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                    }
                }
                is DownloadState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Download Complete",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            state.fileName,
                            color = Color(0xFF888888),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is DownloadState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Download Failed",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            state.message,
                            color = Color(0xFF888888),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1DB954),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is DownloadState.Cancelled -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Download Cancelled",
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Duplicate Dialog
        if (showDuplicateDialog) {
            DuplicateDialog(
                onReplace = {
                    viewModel.dismissDuplicateDialog()
                    viewModel.startDownload(DuplicateStrategy.REPLACE)
                },
                onKeepBoth = {
                    viewModel.dismissDuplicateDialog()
                    viewModel.startDownload(DuplicateStrategy.KEEP_BOTH)
                },
                onCancel = {
                    viewModel.dismissDuplicateDialog()
                }
            )
        }
    }
}

@Composable
private fun YouTubeInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onResolve: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column {
        Text(
            text = "YouTube Link",
            color = Color(0xFF888888),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("https://youtube.com/watch?v=...", color = Color(0xFF444444), fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onResolve,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (url.isNotBlank()) Color(0xFF1DB954) else Color(0xFF2A2A2A),
                    contentColor = if (url.isNotBlank()) Color.Black else Color(0xFF666666)
                ),
                enabled = url.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Resolve", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFFF4444),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun MetadataPreviewCard(
    track: DownloadableTrack,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                if (track.thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF444444),
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Confirm Details",
                    color = Color(0xFF1DB954),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Editable Title
                androidx.compose.foundation.text.BasicTextField(
                    value = track.title,
                    onValueChange = onTitleChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1DB954))
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Editable Artist
                androidx.compose.foundation.text.BasicTextField(
                    value = track.artist,
                    onValueChange = onArtistChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF888888),
                        fontSize = 14.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1DB954))
                )
            }
            
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF666666))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Source: ${track.sourceLabel} \u2022 ${track.audioQuality}",
            color = Color(0xFF444444),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1DB954).copy(alpha = 0.2f) else Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF1DB954) else Color(0xFF888888),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DuplicateDialog(
    onReplace: () -> Unit,
    onKeepBoth: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF181818),
        title = {
            Text(
                "File Already Exists",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "A file with this name already exists in the destination folder.",
                color = Color(0xFFAAAAAA)
            )
        },
        confirmButton = {
            TextButton(onClick = onReplace) {
                Text("Replace", color = Color(0xFFFF4444))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFF888888))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onKeepBoth) {
                    Text("Keep Both", color = Color(0xFF1DB954))
                }
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.0f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}


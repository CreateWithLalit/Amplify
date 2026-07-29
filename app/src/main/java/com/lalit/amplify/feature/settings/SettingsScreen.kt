package com.lalit.amplify.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lalit.amplify.feature.downloader.DownloadViewModel
import com.lalit.amplify.feature.downloader.model.DownloadQuality

@Composable
fun SettingsScreen(
    downloadViewModel: DownloadViewModel = viewModel()
) {
    val context = LocalContext.current

    // Local toggle states
    var amoledMode by remember { mutableStateOf(false) }
    var animations by remember { mutableStateOf(true) }
    var autoMetadata by remember { mutableStateOf(false) }
    var lyricsEnabled by remember { mutableStateOf(false) }

    // Download settings from ViewModel
    val destinationName by downloadViewModel.destinationName.collectAsState()
    val destinationUri by downloadViewModel.destinationUri.collectAsState()
    val defaultQuality by downloadViewModel.defaultQuality.collectAsState()
    val autoImport by downloadViewModel.autoImport.collectAsState()

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val docFile = DocumentFile.fromTreeUri(context, it)
            downloadViewModel.setDestinationUri(it, docFile?.name ?: "Selected Folder")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 24.dp)
        )

        // STORAGE
        SettingsSection(title = "Storage") {
            SettingsNavRow(
                icon = Icons.Default.Folder,
                label = "Download folder",
                value = destinationName,
                onClick = { folderPickerLauncher.launch(null) }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.SdCard,
                label = "SD card support",
                value = if (destinationUri?.toString()?.contains("primary") == false) "Active" else "Not detected",
                onClick = { }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.Download,
                label = "Clear cache",
                value = "0 MB",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PLAYBACK
        SettingsSection(title = "Playback") {
            SettingsNavRow(
                icon = Icons.Default.Equalizer,
                label = "Equalizer",
                onClick = { /* TODO: open system equalizer intent */ }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.Timer,
                label = "Sleep timer",
                value = "Off",
                onClick = { /* TODO: sleep timer dialog */ }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.MusicNote,
                label = "Audio quality",
                value = "High (coming soon)",
                onClick = { }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.MusicNote,
                label = "Crossfade",
                value = "Off (coming soon)",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // METADATA
        SettingsSection(title = "Metadata") {
            SettingsToggleRow(
                icon = Icons.Default.Info,
                label = "Auto metadata enrichment",
                subtitle = "Fetch missing album art & info",
                checked = autoMetadata,
                onCheckedChange = { autoMetadata = it }
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.Lyrics,
                label = "Lyrics",
                subtitle = "Show lyrics when available",
                checked = lyricsEnabled,
                onCheckedChange = { lyricsEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UI
        SettingsSection(title = "Appearance") {
            SettingsToggleRow(
                icon = Icons.Default.ColorLens,
                label = "AMOLED mode",
                subtitle = "Pure black backgrounds",
                checked = amoledMode,
                onCheckedChange = { amoledMode = it }
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.ColorLens,
                label = "Animations",
                subtitle = "Smooth UI transitions",
                checked = animations,
                onCheckedChange = { animations = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DOWNLOADER
        SettingsSection(title = "Downloader") {
            SettingsNavRow(
                icon = Icons.Default.Download,
                label = "Default quality",
                value = defaultQuality.label,
                onClick = {
                    // Cycle through qualities: High -> Medium -> Low -> High
                    val qualities = DownloadQuality.values()
                    val nextIndex = (qualities.indexOf(defaultQuality) + 1) % qualities.size
                    downloadViewModel.setDefaultQuality(qualities[nextIndex])
                }
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.Download,
                label = "Auto import downloads",
                subtitle = "Add to library automatically",
                checked = autoImport,
                onCheckedChange = { downloadViewModel.setAutoImport(it) }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.Folder,
                label = "Download location",
                value = destinationName,
                onClick = { folderPickerLauncher.launch(null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SYSTEM
        SettingsSection(title = "System") {
            SettingsNavRow(
                icon = Icons.Default.Notifications,
                label = "Notification controls",
                onClick = { /* TODO: open notification settings */ }
            )
            SettingsDivider()
            SettingsNavRow(
                icon = Icons.Default.BatteryFull,
                label = "Battery optimization",
                subtitle = "Disable for uninterrupted playback",
                onClick = { /* TODO: open battery optimization settings */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About
        SettingsSection(title = "About") {
            SettingsNavRow(
                icon = Icons.Default.Info,
                label = "Amplify",
                value = "v1.0.0",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Settings Section Container
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title.uppercase(),
            color = Color(0xFF1DB954),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141414))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

// Settings Row with chevron
@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.White, fontSize = 15.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = Color(0xFF555555), fontSize = 12.sp)
            }
        }
        if (value != null) {
            Text(text = value, color = Color(0xFF555555), fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF333333),
            modifier = Modifier.size(18.dp)
        )
    }
}

// Settings Row with toggle
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.White, fontSize = 15.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = Color(0xFF555555), fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1DB954),
                uncheckedThumbColor = Color(0xFF666666),
                uncheckedTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp),
        thickness = 0.5.dp,
        color = Color(0xFF1E1E1E)
    )
}


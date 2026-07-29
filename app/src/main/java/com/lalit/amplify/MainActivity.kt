package com.lalit.amplify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.lalit.amplify.feature.downloader.DownloadViewModel
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.navigation.AmplifyNavHost
import com.lalit.amplify.ui.theme.AmplifyTheme

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmplifyTheme {
                val context = LocalContext.current
                val viewModel: MusicViewModel = viewModel()
                val downloadViewModel: DownloadViewModel = viewModel()

                val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                }

                // Download permissions
                val writePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                } else null

                var hasMediaPermission by remember { mutableStateOf(false) }
                var hasNotificationPermission by remember { mutableStateOf(true) }
                var hasWritePermission by remember { mutableStateOf(true) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    hasMediaPermission = grants[audioPermission] == true
                    hasNotificationPermission =
                        notificationPermission?.let { grants[it] == true } ?: true
                    hasWritePermission = writePermission?.let { grants[it] == true } ?: true
                }

                LaunchedEffect(audioPermission, notificationPermission, writePermission) {
                    hasMediaPermission = ContextCompat.checkSelfPermission(
                        context, audioPermission
                    ) == PackageManager.PERMISSION_GRANTED

                    if (notificationPermission != null) {
                        hasNotificationPermission = ContextCompat.checkSelfPermission(
                            context, notificationPermission
                        ) == PackageManager.PERMISSION_GRANTED
                    }

                    if (writePermission != null) {
                        hasWritePermission = ContextCompat.checkSelfPermission(
                            context, writePermission
                        ) == PackageManager.PERMISSION_GRANTED
                    }

                    val permissionsToRequest = listOfNotNull(
                        audioPermission,
                        notificationPermission,
                        writePermission
                    ).filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }.toTypedArray()

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }

                LaunchedEffect(hasMediaPermission) {
                    if (hasMediaPermission) {
                        viewModel.loadSongs()
                    }
                }

                when {
                    !hasMediaPermission -> {
                        // Permission denied screen
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.amplify_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(bottom = 32.dp)
                            )
                            Text(
                                text = "Music permission required",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    !hasNotificationPermission && notificationPermission != null -> {
                        // Notification permission prompt (preserved from original)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.amplify_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(bottom = 24.dp)
                            )
                            Text(
                                text = "Notification permission is off",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Enable notifications to show playback controls on the lock screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                            )
                            Button(onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }) {
                                Text("Open app settings")
                            }
                        }
                    }

                    else -> {
                        // All permissions granted - launch full app with navigation
                        AmplifyNavHost(
                            musicViewModel = viewModel,
                            downloadViewModel = downloadViewModel
                        )
                    }
                }
            }
        }
    }
}


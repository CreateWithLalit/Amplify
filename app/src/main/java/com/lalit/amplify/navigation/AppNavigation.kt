// app/src/main/java/com/lalit/amplify/navigation/AppNavigation.kt
// MERGE INSTRUCTION: CREATE this file. New file, doesn't exist yet.

package com.lalit.amplify.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lalit.amplify.feature.dashboard.DashboardScreen
import com.lalit.amplify.feature.downloader.DownloadViewModel
import com.lalit.amplify.feature.library.LibraryScreen
import com.lalit.amplify.feature.player.FullPlayerScreen
import com.lalit.amplify.feature.player.MiniPlayer
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.feature.downloader.DownloadScreen
import com.lalit.amplify.feature.settings.SettingsScreen

// ─── Route constants ───────────────────────────────────────────────────────────
object AmplifyRoutes {
    const val HOME = "home"
    const val DOWNLOAD = "download"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val FULL_PLAYER = "full_player"
}

// ─── Bottom nav items ──────────────────────────────────────────────────────────
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(AmplifyRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(AmplifyRoutes.DOWNLOAD, "Download", Icons.Filled.Download, Icons.Filled.Download),
    BottomNavItem(AmplifyRoutes.LIBRARY, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomNavItem(AmplifyRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

// ─── Main navigation host ──────────────────────────────────────────────────────
@UnstableApi
@Composable
fun AmplifyNavHost(
    musicViewModel: MusicViewModel = viewModel(),
    downloadViewModel: DownloadViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerState by musicViewModel.playerState.collectAsState()
    val isFullPlayer = currentRoute == AmplifyRoutes.FULL_PLAYER

    // Bottom nav hidden on full player screen
    val showBottomNav = !isFullPlayer
    val showMiniPlayer = playerState.currentSong != null && !isFullPlayer

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                AmplifyBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AmplifyRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(AmplifyRoutes.HOME) {
                    DashboardScreen(
                        viewModel = musicViewModel,
                        onOpenFullPlayer = {
                            navController.navigate(AmplifyRoutes.FULL_PLAYER)
                        }
                    )
                }
                composable(AmplifyRoutes.DOWNLOAD) {
                    DownloadScreen(downloadViewModel)
                }
                composable(AmplifyRoutes.LIBRARY) {
                    LibraryScreen(viewModel = musicViewModel)
                }
                composable(AmplifyRoutes.SETTINGS) {
                    SettingsScreen(
                        downloadViewModel = downloadViewModel
                    )
                }
                composable(AmplifyRoutes.FULL_PLAYER) {
                    FullPlayerScreen(
                        playerState = playerState,
                        onPlayPause = { musicViewModel.togglePlayPause() },
                        onNext = { musicViewModel.next() },
                        onPrevious = { musicViewModel.previous() },
                        onSeek = { musicViewModel.seekTo(it) },
                        onShuffle = { musicViewModel.toggleShuffle() },
                        onRepeat = { musicViewModel.toggleRepeat() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // MiniPlayer floats above bottom nav, inside scaffold content
            if (showMiniPlayer) {
                MiniPlayer(
                    playerState = playerState,
                    onPlayPause = { musicViewModel.togglePlayPause() },
                    onNext = { musicViewModel.next() },
                    onPrevious = { musicViewModel.previous() },
                    onTap = { navController.navigate(AmplifyRoutes.FULL_PLAYER) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
    }
}

// ─── Bottom Navigation Bar ─────────────────────────────────────────────────────
@Composable
private fun AmplifyBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF111111),
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1DB954),
                    selectedTextColor = Color(0xFF1DB954),
                    unselectedIconColor = Color(0xFF666666),
                    unselectedTextColor = Color(0xFF666666),
                    indicatorColor = Color(0xFF1DB954).copy(alpha = 0.15f)
                )
            )
        }
    }
}

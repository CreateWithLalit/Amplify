package com.lalit.amplify.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.ui.AlbumArtImage
import com.lalit.amplify.core.util.SortOrder
import com.lalit.amplify.feature.downloader.data.DownloadedSongRepository
import com.lalit.amplify.feature.player.MiniPlayer
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.feature.player.formatDuration

@UnstableApi
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onOpenFullPlayer: () -> Unit
) {
    val context = LocalContext.current
    val downloadedSongRepository = remember { DownloadedSongRepository.getInstance(context) }

    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState(initial = emptySet())
    val recentlyPlayedIds by viewModel.recentlyPlayedIds.collectAsState(initial = emptyList())

    // Downloaded songs from repository
    val downloadedSongs by downloadedSongRepository.downloadedSongs.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Music", "Local", "Downloaded")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val nextOrder = SortOrder.entries[
                        (sortOrder.ordinal + 1) % SortOrder.entries.size
                    ]
                    viewModel.updateSortOrder(nextOrder)
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = Color(0xFF888888)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                placeholder = {
                    Text("Search your library...", color = Color(0xFF555555))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF555555))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF1DB954),
                edgePadding = 20.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF1DB954)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Color.White else Color(0xFF666666),
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Sort label
            if (selectedTab != 2) {
                Text(
                    text = "Sorted by: ${sortOrder.displayName}",
                    color = Color(0xFF444444),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 2.dp)
                )
            }

            // Display list
            val displaySongs: List<Song> = when (selectedTab) {
                        0 -> songs + downloadedSongs
                1 -> songs
                2 -> downloadedSongs
                else -> songs
            }

            if (displaySongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTab == 2) "No downloaded songs yet" else "No songs found",
                            color = Color(0xFF444444),
                            fontSize = 15.sp
                        )
                        if (selectedTab == 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use Search to find and download music",
                                color = Color(0xFF333333),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (playerState.currentSong != null) 90.dp else 16.dp
                    )
                ) {
                    item {
                        Text(
                            text = "${displaySongs.size} songs",
                            color = Color(0xFF444444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    itemsIndexed(displaySongs) { _, song ->
                        LibrarySongRow(
                            song = song,
                            isPlaying = playerState.currentSong?.id == song.id,
                            isFavorite = favoriteIds.contains(song.id),
                            onClick = { viewModel.playSong(song, displaySongs) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFF151515)
                        )
                    }
                }
            }
        }

        // MiniPlayer
        if (playerState.currentSong != null) {
            MiniPlayer(
                playerState = playerState,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onTap = onOpenFullPlayer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isPlaying) Color(0xFF0F1F0F) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            uri = song.albumArtUri,
            size = 50.dp,
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isPlaying) Color(0xFF1DB954) else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    text = song.artist,
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDuration(song.duration),
                    color = Color(0xFF555555),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFF1DB954) else Color(0xFF3A3A3A),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


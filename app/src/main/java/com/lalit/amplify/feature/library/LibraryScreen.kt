package com.lalit.amplify.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import com.lalit.amplify.core.ui.AlbumArtImage
import com.lalit.amplify.core.util.SortOrder
import com.lalit.amplify.feature.downloader.data.DownloadedSongRepository
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.feature.player.formatDuration

private enum class LibraryCollection { ALL, FAVORITES, RECENT }

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun LibraryScreen(viewModel: MusicViewModel) {
    val downloadedSongRepository = remember { DownloadedSongRepository.getInstance(viewModel.getApplication()) }
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState(initial = emptySet())
    val recentlyPlayedIds by viewModel.recentlyPlayedIds.collectAsState(initial = emptyList())
    val downloadedSongs by downloadedSongRepository.downloadedSongs.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCollection by remember { mutableStateOf(LibraryCollection.ALL) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var songPendingDelete by remember { mutableStateOf<Song?>(null) }

    val baseSongs = when (selectedTab) {
        0 -> songs
        1 -> songs.filter { it.source == SongSource.LOCAL }
        else -> downloadedSongs
    }
    val visibleSongs = when (selectedCollection) {
        LibraryCollection.ALL -> baseSongs
        LibraryCollection.FAVORITES -> baseSongs.filter { it.id in favoriteIds }
        LibraryCollection.RECENT -> baseSongs.sortedBy { song ->
            recentlyPlayedIds.indexOf(song.id).let { if (it == -1) Int.MAX_VALUE else it }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedSong == null) {
                LibraryHeader(
                    searchQuery = searchQuery,
                    onSearchChange = viewModel::updateSearchQuery,
                    onSort = {
                        val nextOrder = SortOrder.entries[(sortOrder.ordinal + 1) % SortOrder.entries.size]
                        viewModel.updateSortOrder(nextOrder)
                    }
                )

                LibraryShortcuts(
                    selectedCollection = selectedCollection,
                    onCollectionSelected = { selectedCollection = it }
                )
            } else {
                SelectionHeader(
                    canDelete = selectedSong?.source == SongSource.DOWNLOADED,
                    onClose = { selectedSong = null },
                    onDelete = { songPendingDelete = selectedSong }
                )
            }

            LibraryTabs(selectedTab = selectedTab, onTabSelected = {
                selectedTab = it
                selectedCollection = LibraryCollection.ALL
            })

            if (selectedSong == null) {
                ShuffleRow(
                    enabled = visibleSongs.isNotEmpty(),
                    onShuffle = {
                        val shuffled = visibleSongs.shuffled()
                        shuffled.firstOrNull()?.let { viewModel.playSong(it, shuffled) }
                    }
                )
            }

            if (visibleSongs.isEmpty()) {
                EmptyLibraryState(selectedTab, selectedCollection)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (playerState.currentSong != null) 92.dp else 18.dp
                    )
                ) {
                    items(visibleSongs, key = { it.uri.toString() }) { song ->
                        LibrarySongRow(
                            song = song,
                            isPlaying = playerState.currentSong?.uri == song.uri,
                            isFavorite = song.id in favoriteIds,
                            isSelected = selectedSong?.uri == song.uri,
                            onClick = {
                                if (selectedSong == null) viewModel.playSong(song, visibleSongs)
                                else selectedSong = song
                            },
                            onLongClick = {
                                if (song.source == SongSource.DOWNLOADED) selectedSong = song
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(song) }
                        )
                    }
                }
            }
        }

        if (selectedSong != null) {
            SelectionActionBar(
                canDelete = selectedSong?.source == SongSource.DOWNLOADED,
                onDelete = { songPendingDelete = selectedSong },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (playerState.currentSong != null) 82.dp else 12.dp)
            )
        }

        songPendingDelete?.let { song ->
            AlertDialog(
                onDismissRequest = { songPendingDelete = null },
                containerColor = Color(0xFF181818),
                title = { Text("Delete this download?", color = Color.White) },
                text = {
                    Text(
                        "${song.title} will be deleted from this phone.",
                        color = Color(0xFFAAAAAA)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteDownloadedSong(song)
                        songPendingDelete = null
                        selectedSong = null
                    }) {
                        Text("Delete", color = Color(0xFFFF5C5C))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { songPendingDelete = null }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Library filters",
            tint = Color(0xFF9A9A9A),
            modifier = Modifier.padding(horizontal = 4.dp).size(22.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            placeholder = { Text("Search songs, artists, and albums") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onSort) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color(0xFF777777),
                unfocusedPlaceholderColor = Color(0xFF777777),
                focusedContainerColor = Color(0xFF242424),
                unfocusedContainerColor = Color(0xFF242424),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLeadingIconColor = Color(0xFFAAAAAA),
                unfocusedLeadingIconColor = Color(0xFFAAAAAA),
                focusedTrailingIconColor = Color(0xFFAAAAAA),
                unfocusedTrailingIconColor = Color(0xFFAAAAAA)
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true
        )
    }
}

@Composable
private fun LibraryShortcuts(
    selectedCollection: LibraryCollection,
    onCollectionSelected: (LibraryCollection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LibraryShortcut(
            label = "Favourites",
            icon = Icons.Default.Favorite,
            gradient = listOf(Color(0xFF7B1E47), Color(0xFFD34C78)),
            selected = selectedCollection == LibraryCollection.FAVORITES,
            onClick = { onCollectionSelected(LibraryCollection.FAVORITES) },
            modifier = Modifier.weight(1f)
        )
        LibraryShortcut(
            label = "Playlists",
            icon = Icons.Default.PlaylistPlay,
            gradient = listOf(Color(0xFF1B485D), Color(0xFF337D99)),
            selected = false,
            onClick = { onCollectionSelected(LibraryCollection.ALL) },
            modifier = Modifier.weight(1f)
        )
        LibraryShortcut(
            label = "Recent",
            icon = Icons.Default.History,
            gradient = listOf(Color(0xFF35256F), Color(0xFF6650C4)),
            selected = selectedCollection == LibraryCollection.RECENT,
            onClick = { onCollectionSelected(LibraryCollection.RECENT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LibraryShortcut(
    label: String,
    icon: ImageVector,
    gradient: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(gradient))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(21.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectionHeader(canDelete: Boolean, onClose: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel selection", tint = Color.White)
        }
        Text(
            text = "1 item selected",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected song", tint = Color(0xFFFF6B6B))
            }
        }
    }
}

@Composable
private fun LibraryTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val labels = listOf("Songs", "Local", "Downloads")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labels.forEachIndexed { index, label ->
            TextButton(onClick = { onTabSelected(index) }) {
                Text(
                    text = label,
                    color = if (index == selectedTab) Color.Black else Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (index == selectedTab) Color.White else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ShuffleRow(enabled: Boolean, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(enabled = enabled, onClick = onShuffle, onLongClick = {})
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("Shuffle playback", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyLibraryState(tab: Int, collection: LibraryCollection) {
    val message = when {
        collection == LibraryCollection.FAVORITES -> "No favourite songs yet"
        collection == LibraryCollection.RECENT -> "No recent songs yet"
        tab == 2 -> "No downloads yet"
        else -> "No songs found"
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF444444), modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = Color(0xFF888888), fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrarySongRow(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isSelected -> Color(0xFF173C25)
                    isPlaying -> Color(0xFF111D14)
                    else -> Color.Transparent
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(uri = song.albumArtUri, size = 48.dp, cornerRadius = 7.dp)
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isPlaying) Color(0xFF3DDB76) else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = buildString {
                    append(song.artist)
                    if (song.album.isNotBlank() && song.album != "Unknown Album") append(" • ${song.album}")
                },
                color = Color(0xFF999999),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(formatDuration(song.duration), color = Color(0xFF666666), fontSize = 11.sp)
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favourite",
                tint = if (isFavorite) Color(0xFF3DDB76) else Color(0xFF777777),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SelectionActionBar(canDelete: Boolean, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    if (!canDelete) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF242424))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete selected song", tint = Color(0xFFFF6B6B))
        }
        Text("Delete", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(end = 10.dp))
    }
}

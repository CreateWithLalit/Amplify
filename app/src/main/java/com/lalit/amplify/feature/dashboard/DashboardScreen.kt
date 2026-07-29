// app/src/main/java/com/lalit/amplify/feature/dashboard/DashboardScreen.kt
// MERGE INSTRUCTION: CREATE this new file. New package: feature/dashboard/
// This is the new HOME tab. The old HomeScreen.kt becomes the Library screen's song list.

package com.lalit.amplify.feature.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.lalit.amplify.R
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.ui.AlbumArtImage
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.feature.player.MiniPlayer

@UnstableApi
@Composable
fun DashboardScreen(
    viewModel: MusicViewModel,
    onOpenFullPlayer: () -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState(initial = emptySet())
    val recentlyPlayedIds by viewModel.recentlyPlayedIds.collectAsState(initial = emptyList())

    val recentSongs = run {
        val songMap = songs.associateBy { it.id }
        recentlyPlayedIds.mapNotNull { songMap[it] }.take(10)
    }
    val favoriteSongs = songs.filter { favoriteIds.contains(it.id) }.take(10)
    val albumMap = songs.groupBy { it.album }.entries
        .filter { it.key != "Unknown Album" }
        .sortedByDescending { it.value.size }
        .take(12)
    val artistMap = songs.groupBy { it.artist }.entries
        .filter { it.key != "Unknown Artist" }
        .sortedByDescending { it.value.size }
        .take(12)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = if (playerState.currentSong != null) 90.dp else 16.dp
            )
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 28.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.amplify_logo),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Amplify",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Play Grid
            if (songs.isNotEmpty()) {
                item {
                    SectionTitle("Quick Play")
                    QuickPlayGrid(
                        songs = songs.take(6),
                        playingSongId = playerState.currentSong?.id,
                        onPlay = { song -> viewModel.playSong(song, songs) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Continue Listening
            if (recentSongs.isNotEmpty()) {
                item {
                    SectionTitle("Continue Listening")
                    HorizontalSongRow(
                        songs = recentSongs,
                        playingSongId = playerState.currentSong?.id,
                        onSongClick = { song -> viewModel.playSong(song, recentSongs) }
                    )
                }
            }

            // Favorites
            if (favoriteSongs.isNotEmpty()) {
                item {
                    SectionTitle("Favorites")
                    HorizontalSongRow(
                        songs = favoriteSongs,
                        playingSongId = playerState.currentSong?.id,
                        onSongClick = { song -> viewModel.playSong(song, favoriteSongs) }
                    )
                }
            }

            // Albums
            if (albumMap.isNotEmpty()) {
                item {
                    SectionTitle("Albums")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(albumMap, key = { it.key }) { entry ->
                            val representative = entry.value.first()
                            AlbumCard(
                                albumName = entry.key,
                                artistName = representative.artist,
                                albumArtUri = representative.albumArtUri,
                                songCount = entry.value.size,
                                onClick = {
                                    val albumSongs = entry.value
                                    viewModel.playSong(albumSongs.first(), albumSongs)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Artists
            if (artistMap.isNotEmpty()) {
                item {
                    SectionTitle("Artists")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(artistMap, key = { it.key }) { entry ->
                            val representative = entry.value.first()
                            ArtistCard(
                                artistName = entry.key,
                                songCount = entry.value.size,
                                albumArtUri = representative.albumArtUri,
                                onClick = {
                                    val artistSongs = entry.value
                                    viewModel.playSong(artistSongs.first(), artistSongs)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // All Songs suggestion
            if (songs.isNotEmpty()) {
                item {
                    SectionTitle("Suggested For You")
                    HorizontalSongRow(
                        songs = songs.shuffled().take(10),
                        playingSongId = playerState.currentSong?.id,
                        onSongClick = { song -> viewModel.playSong(song, songs) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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

// Quick Play 2x3 Grid
@Composable
private fun QuickPlayGrid(
    songs: List<Song>,
    playingSongId: Long?,
    onPlay: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        songs.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { song ->
                    QuickPlayItem(
                        song = song,
                        isPlaying = song.id == playingSongId,
                        onClick = { onPlay(song) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty slot if odd count
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPlayItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlaying) Color(0xFF1A2A1A) else Color(0xFF181818))
            .clickable { onClick() }
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            uri = song.albumArtUri,
            size = 52.dp,
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = song.title,
            color = if (isPlaying) Color(0xFF1DB954) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// Horizontal Song Scroll Row
@Composable
private fun HorizontalSongRow(
    songs: List<Song>,
    playingSongId: Long?,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongCard(
                song = song,
                isPlaying = song.id == playingSongId,
                onClick = { onSongClick(song) }
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.size(130.dp)) {
            AlbumArtImage(
                uri = song.albumArtUri,
                size = 130.dp,
                cornerRadius = 12.dp
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DB954)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.title,
            color = if (isPlaying) Color(0xFF1DB954) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = Color(0xFF888888),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Album Card
@Composable
private fun AlbumCard(
    albumName: String,
    artistName: String,
    albumArtUri: android.net.Uri?,
    songCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        AlbumArtImage(
            uri = albumArtUri,
            size = 130.dp,
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = albumName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$artistName • $songCount songs",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Artist Card
@Composable
private fun ArtistCard(
    artistName: String,
    songCount: Int,
    albumArtUri: android.net.Uri?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
        ) {
            AlbumArtImage(
                uri = albumArtUri,
                size = 110.dp,
                cornerRadius = 55.dp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artistName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$songCount songs",
            color = Color(0xFF888888),
            fontSize = 11.sp
        )
    }
}

// Section Title
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp)
    )
}


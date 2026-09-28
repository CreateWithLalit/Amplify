package com.lalit.amplify.feature.search

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.ui.AlbumArtImage
import com.lalit.amplify.feature.downloader.DownloaderActivity
import com.lalit.amplify.feature.player.MiniPlayer
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.feature.player.formatDuration
import com.lalit.amplify.feature.search.SearchTab

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun SearchScreen(
    musicViewModel: MusicViewModel,
    onOpenFullPlayer: () -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val recentSearches by searchViewModel.recentSearches.collectAsState()
    val playerState by musicViewModel.playerState.collectAsState()
    val localSongs by musicViewModel.songs.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val query = uiState.query
    val selectedTab = uiState.selectedTab
    val bollywoodResults = uiState.bollywoodResults
    val jamendoResults = uiState.jamendoResults
    val localResults = localSongs
    var selectedDetailsSong by remember { mutableStateOf<Song?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Search & Discover",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    searchViewModel.onQueryChange(it)
                    musicViewModel.updateSearchQuery(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text(
                        "Search Bollywood, artists, global hits...",
                        color = Color(0xFF555555)
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF666666))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            searchViewModel.clearSearch()
                            musicViewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF666666))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFF1DB954)
                ),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    searchViewModel.searchNow()
                    focusManager.clearFocus()
                })
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SearchTabChip(label = "All", selected = selectedTab == SearchTab.ALL) {
                        searchViewModel.onTabSelected(SearchTab.ALL)
                    }
                }
                item {
                    SearchTabChip(label = "Bollywood & Global", selected = selectedTab == SearchTab.BOLLYWOOD_GLOBAL) {
                        searchViewModel.onTabSelected(SearchTab.BOLLYWOOD_GLOBAL)
                    }
                }
                item {
                    SearchTabChip(label = "Jamendo", selected = selectedTab == SearchTab.JAMENDO) {
                        searchViewModel.onTabSelected(SearchTab.JAMENDO)
                    }
                }
                item {
                    SearchTabChip(label = "Local", selected = selectedTab == SearchTab.LOCAL) {
                        searchViewModel.onTabSelected(SearchTab.LOCAL)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (query.isBlank()) {
                if (recentSearches.isNotEmpty()) {
                    RecentSearchesSection(
                        recentSearches = recentSearches,
                        onClick = {
                            searchViewModel.onQueryChange(it)
                            musicViewModel.updateSearchQuery(it)
                            searchViewModel.searchNow()
                        },
                        onRemove = searchViewModel::removeRecentSearch,
                        onClearAll = searchViewModel::clearRecentSearches
                    )
                }

                if (selectedTab != SearchTab.LOCAL) {
                    GenreSection(
                        genres = searchViewModel.genres,
                        onGenreClick = {
                            searchViewModel.searchGenre(it)
                            musicViewModel.updateSearchQuery("")
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    SearchTab.BOLLYWOOD_GLOBAL -> SongResultsList(
                        title = "Bollywood & Global Discovery",
                        songs = bollywoodResults,
                        listState = listState,
                        playerState = playerState,
                        onSongClick = { song ->
                            if (song.playbackCapability == com.lalit.amplify.core.model.PlaybackCapability.EXTERNAL_LINK) {
                                selectedDetailsSong = song
                            } else {
                                musicViewModel.playSong(song, bollywoodResults)
                            }
                        }
                    )

                    SearchTab.LOCAL -> SongResultsList(
                        title = "Local Library",
                        songs = localResults,
                        listState = listState,
                        playerState = playerState,
                        onSongClick = { song -> musicViewModel.playSong(song, localResults) }
                    )

                    SearchTab.JAMENDO -> {
                        if (uiState.jamendoLoading && jamendoResults.isEmpty()) {
                            SearchLoadingIndicator()
                        } else if (uiState.jamendoError != null && jamendoResults.isEmpty()) {
                            SearchError(message = uiState.jamendoError ?: "Search failed") {
                                searchViewModel.searchNow()
                            }
                        } else if (query.isBlank() && uiState.selectedGenre == null) {
                            EmptySearchResult(query = query)
                        } else {
                            SongResultsList(
                                title = if (uiState.selectedGenre != null) {
                                    "${uiState.selectedGenre} on Jamendo"
                                } else {
                                    "Jamendo Results"
                                },
                                songs = jamendoResults,
                                listState = listState,
                                playerState = playerState,
                                onSongClick = { song -> musicViewModel.playSong(song, jamendoResults) },
                                loading = uiState.jamendoLoading,
                                showLoadMore = uiState.jamendoHasMore,
                                onLoadMore = searchViewModel::loadMoreJamendo
                            )
                        }
                    }

                    SearchTab.ALL -> {
                        if (query.isBlank() && uiState.selectedGenre == null) {
                            IdleSearchHint()
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    bottom = if (playerState.currentSong != null) 90.dp else 16.dp
                                )
                            ) {
                                if (bollywoodResults.isNotEmpty()) {
                                    item { SectionHeader("Bollywood & Global Discovery") }
                                    items(bollywoodResults, key = { "bollywood-${it.id}" }) { song ->
                                        SongRow(song = song, isPlaying = song.id == playerState.currentSong?.id) {
                                            if (song.playbackCapability == com.lalit.amplify.core.model.PlaybackCapability.EXTERNAL_LINK) {
                                                selectedDetailsSong = song
                                            } else {
                                                musicViewModel.playSong(song, bollywoodResults)
                                            }
                                        }
                                    }
                                }

                                if (localResults.isNotEmpty()) {
                                    item {
                                        SectionHeader("Local Files")
                                    }
                                    items(localResults, key = { "local-${it.id}" }) { song ->
                                        SongRow(song = song, isPlaying = song.id == playerState.currentSong?.id) {
                                            musicViewModel.playSong(song, localResults)
                                        }
                                    }
                                }

                                if (jamendoResults.isNotEmpty()) {
                                    item { SectionHeader("Jamendo Stream") }
                                    items(jamendoResults, key = { "jamendo-${it.id}" }) { song ->
                                        SongRow(song = song, isPlaying = song.id == playerState.currentSong?.id) {
                                            musicViewModel.playSong(song, jamendoResults)
                                        }
                                    }
                                    if (uiState.jamendoHasMore) {
                                        item {
                                            Button(
                                                onClick = { searchViewModel.loadMoreJamendo() },
                                                modifier = Modifier
                                                    .padding(20.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                Text("Load more Jamendo")
                                            }
                                        }
                                    }
                                }

                                if (bollywoodResults.isEmpty() && localResults.isEmpty() && jamendoResults.isEmpty() && !uiState.jamendoLoading) {
                                    item { EmptySearchResult(query = query) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Track Details Dialog
        selectedDetailsSong?.let { song ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { selectedDetailsSong = null },
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        AlbumArtImage(uri = song.albumArtUri, size = 160.dp, cornerRadius = 16.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(song.artist, color = Color(0xFFB3B3B3), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        if (song.album.isNotBlank() && song.album != "Unknown Album") {
                            Text(song.album, color = Color(0xFF777777), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val badge = when (song.source) {
                            com.lalit.amplify.core.model.SongSource.BOLLYWOOD_CATALOG -> "Bollywood Discovery Catalog"
                            com.lalit.amplify.core.model.SongSource.INTERNATIONAL_CATALOG -> "Global Discovery Catalog"
                            else -> song.source.name
                        }
                        Text("Source: $badge", color = Color(0xFF1DB954), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        song.licenseInfo?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(it, color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                },
                confirmButton = {
                    val targetUrl = song.sourceUrl ?: song.artistUrl ?: (if (song.uri.scheme?.startsWith("http") == true) song.uri.toString() else null)
                    if (targetUrl != null) {
                        Button(
                            onClick = {
                                uriHandler.openUri(targetUrl)
                                selectedDetailsSong = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            Text("Open Official Stream")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedDetailsSong = null }) {
                        Text("Close", color = Color(0xFF888888))
                    }
                }
            )
        }

        // Mini Player
        if (playerState.currentSong != null) {
            MiniPlayer(
                playerState = playerState,
                onPlayPause = { musicViewModel.togglePlayPause() },
                onNext = { musicViewModel.next() },
                onPrevious = { musicViewModel.previous() },
                onTap = onOpenFullPlayer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SearchTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF1DB954) else Color(0xFF1A1A1A),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(label) }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent searches", color = Color.White, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onClearAll) { Text("Clear all") }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(recentSearches, key = { it }) { item ->
                Button(onClick = { onClick(item) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))) {
                    Text(item)
                }
                IconButton(onClick = { onRemove(item) }) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF888888)) }
            }
        }
    }
}

@Composable
private fun GenreSection(genres: List<String>, onGenreClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Browse genres",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(genres, key = { it }) { genre ->
                Button(onClick = { onGenreClick(genre) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))) {
                    Text(genre.replaceFirstChar { it.uppercaseChar() })
                }
            }
        }
    }
}

@Composable
private fun SongResultsList(
    title: String,
    songs: List<Song>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    playerState: com.lalit.amplify.core.model.PlayerState,
    onSongClick: (Song) -> Unit,
    loading: Boolean = false,
    showLoadMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = if (playerState.currentSong != null) 90.dp else 16.dp)
    ) {
        item { SectionHeader(title) }
        if (songs.isEmpty()) {
            item { EmptySearchResult(query = title) }
        } else {
            items(songs, key = { "${it.source}-${it.id}" }) { song ->
                SongRow(song = song, isPlaying = song.id == playerState.currentSong?.id, onClick = { onSongClick(song) })
            }
            if (loading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Color(0xFF1DB954))
                    }
                }
            }
            if (showLoadMore) {
                item {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier.padding(20.dp).fillMaxWidth()
                    ) { Text("Load more") }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(uri = song.albumArtUri, size = 54.dp, cornerRadius = 10.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = if (isPlaying) Color(0xFF1DB954) else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color(0xFF9A9A9A), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (sourceLabel, sourceColor) = when (song.source) {
                    com.lalit.amplify.core.model.SongSource.BOLLYWOOD_CATALOG -> "Bollywood" to Color(0xFFFFB300)
                    com.lalit.amplify.core.model.SongSource.INTERNATIONAL_CATALOG -> "Global" to Color(0xFF29B6F6)
                    com.lalit.amplify.core.model.SongSource.JAMENDO -> "Jamendo" to Color(0xFF1DB954)
                    com.lalit.amplify.core.model.SongSource.LOCAL -> "Local" to Color(0xFFAAAAAA)
                    com.lalit.amplify.core.model.SongSource.DOWNLOADED -> "Downloaded" to Color(0xFF4DD0E1)
                    else -> song.source.name to Color(0xFF1DB954)
                }
                Text(sourceLabel, color = sourceColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (song.duration > 0) formatDuration(song.duration) else "--:--", color = Color(0xFF666666), fontSize = 11.sp)
            }
        }
    }
}

// Search Result Row
@Composable
private fun SearchResultRow(
    result: MusicSearchResult,
    onTap: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with Coil
        SearchThumbnail(
            thumbnailUrl = result.thumbnailUrl,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.artist,
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.duration > 0) formatDuration(result.duration) else "--:--",
                    color = Color(0xFF555555),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = result.sourceLabel,
                color = Color(0xFF1DB954),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onDownload,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF444444),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Result Detail Sheet Content
@Composable
private fun SearchResultDetailSheetContent(
    result: MusicSearchResult,
    downloadLinkState: DownloadLinkState,
    onGetDownloadLink: () -> Unit,
    onNavigateToDownloader: (DownloadableTrack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .align(Alignment.CenterHorizontally)
        ) {
            SearchThumbnail(
                thumbnailUrl = result.thumbnailUrl,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = result.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = result.artist,
            color = Color(0xFF999999),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "${result.sourceLabel}  \u2022  ${if (result.duration > 0) formatDuration(result.duration) else "Unknown duration"}",
            color = Color(0xFF666666),
            fontSize = 12.sp,
            modifier = Modifier
                .padding(top = 4.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons based on state
        when (downloadLinkState) {
            DownloadLinkState.Idle, DownloadLinkState.Resolving -> {
                Button(
                    onClick = onGetDownloadLink,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = downloadLinkState != DownloadLinkState.Resolving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (downloadLinkState == DownloadLinkState.Resolving) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (downloadLinkState == DownloadLinkState.Resolving) "Getting link..." else "Get Download Link",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is DownloadLinkState.Resolved -> {
                Button(
                    onClick = { onNavigateToDownloader(downloadLinkState.track) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Download", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            is DownloadLinkState.Error -> {
                Text(
                    text = downloadLinkState.message,
                    color = Color(0xFFFF4444),
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGetDownloadLink,
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

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// State UI helpers
@Composable
private fun IdleSearchHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF333333),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search for music",
            color = Color(0xFF555555),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try \"Arijit Singh\", \"Shape of You\",\nor any song or artist",
            color = Color(0xFF333333),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SearchLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFF1DB954),
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Searching...", color = Color(0xFF666666), fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No results for", color = Color(0xFF555555), fontSize = 14.sp)
        Text(
            text = "\"$query\"",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SearchError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color(0xFF666666), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
        ) {
            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SearchChip(label: String, active: Boolean, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Color(0xFF1DB954).copy(alpha = 0.15f) else Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (active) Color(0xFF1DB954) else Color(0xFF666666),
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lalit.amplify.feature.downloader.DownloaderActivity
import com.lalit.amplify.feature.player.MiniPlayer
import com.lalit.amplify.feature.player.MusicViewModel
import com.lalit.amplify.feature.player.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun SearchScreen(
    musicViewModel: MusicViewModel,
    onOpenFullPlayer: () -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val query by searchViewModel.query.collectAsState()
    val uiState by searchViewModel.uiState.collectAsState()
    val selectedResult by searchViewModel.selectedResult.collectAsState()
    val downloadLinkState by searchViewModel.downloadLinkState.collectAsState()
    val playerState by musicViewModel.playerState.collectAsState()
    val localSongs by musicViewModel.songs.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Search",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    context.startActivity(Intent(context, DownloaderActivity::class.java))
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Downloader", tint = Color.White)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { searchViewModel.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text(
                        "Songs, artists, albums...",
                        color = Color(0xFF555555)
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF666666))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.clearSearch() }) {
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
                    searchViewModel.onSearchSubmit()
                    focusManager.clearFocus()
                })
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tab indicator: Internet / Local
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchChip(label = "Internet", active = true)
                SearchChip(
                    label = "Local (${localSongs.size})",
                    active = false,
                    onClick = { /* Library tab handles local */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = uiState) {
                    SearchUiState.Idle -> IdleSearchHint()
                    SearchUiState.Loading -> SearchLoadingIndicator()
                    SearchUiState.Empty -> EmptySearchResult(query = query)
                    is SearchUiState.Error -> SearchError(message = state.message) {
                        searchViewModel.onSearchSubmit()
                    }
                    is SearchUiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                bottom = if (playerState.currentSong != null) 90.dp else 16.dp
                            )
                        ) {
                            item {
                                Text(
                                    text = "${state.results.size} results for \"$query\"",
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(
                                        horizontal = 20.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                            items(state.results, key = { it.id }) { result ->
                                SearchResultRow(
                                    result = result,
                                    onTap = {
                                        searchViewModel.selectResult(result)
                                    },
                                    onDownload = {
                                        searchViewModel.selectResult(result)
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal bottom sheet for search result details
        if (selectedResult != null) {
            ModalBottomSheet(
                onDismissRequest = { searchViewModel.clearSelection() },
                sheetState = sheetState,
                containerColor = Color(0xFF181818),
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF444444))
                    )
                }
            ) {
                SearchResultDetailSheetContent(
                    result = selectedResult!!,
                    downloadLinkState = downloadLinkState,
                    onGetDownloadLink = { searchViewModel.getDownloadLink() },
                    onNavigateToDownloader = { track ->
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            val intent = Intent(context, DownloaderActivity::class.java).apply {
                                putExtra(DownloaderActivity.EXTRA_DOWNLOADABLE_TRACK, track)
                            }
                            context.startActivity(intent)
                            searchViewModel.clearSelection()
                        }
                    }
                )
            }
        }

        // Mini Player
        if (playerState.currentSong != null && selectedResult == null) {
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

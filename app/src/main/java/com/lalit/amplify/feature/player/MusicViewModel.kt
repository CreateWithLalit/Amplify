package com.lalit.amplify.feature.player

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import android.content.ComponentName
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lalit.amplify.core.model.PlayerState
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.feature.downloader.data.DownloadedSongRepository
import com.lalit.amplify.service.MusicPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.lalit.amplify.core.data.AmplifyDataStore
import com.lalit.amplify.core.util.SortOrder
import kotlinx.coroutines.launch

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val dataStore = AmplifyDataStore(context)

    private val downloadedSongRepo = DownloadedSongRepository.getInstance(context)
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val rawSongs: StateFlow<List<Song>> = combine(
        _localSongs,
        downloadedSongRepo.downloadedSongs
    ) { local, downloaded ->
        val downloadedUris = downloaded.mapTo(mutableSetOf()) { it.uri.toString() }
        // Prefer the downloaded-song record, which carries its source and album art,
        // while preventing MediaStore from showing the same file twice.
        (downloaded + local.filterNot { it.uri.toString() in downloadedUris })
            .sortedBy { it.title.lowercase() }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val favoriteIds = dataStore.favoriteIds
    val recentlyPlayedIds = dataStore.recentlyPlayedIds

    // Processed songs list based on search and sort
    val filteredSongs = combine(rawSongs, _searchQuery, _sortOrder) { songs, query, sort ->
        var filtered = if (query.isBlank()) {
            songs
        } else {
            songs.filter { 
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SortOrder.DURATION -> filtered.sortedByDescending { it.duration }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    // Convenience flow for UI
    val songs: StateFlow<List<Song>> = filteredSongs

    val downloadedSongRepository: DownloadedSongRepository
        get() = downloadedSongRepo

    private val _playerState = MutableStateFlow(PlayerState(repeatMode = 1)) // 1 = Repeat All
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                val song = rawSongs.value.firstOrNull { it.id == mediaId }
                _playerState.value = _playerState.value.copy(
                    currentSong = song,
                    currentPosition = 0L,
                    duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _playerState.value = _playerState.value.copy(
                        duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playerState.value = _playerState.value.copy(shuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val stateMode = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> 1
                    Player.REPEAT_MODE_ONE -> 2
                    else -> 0
                }
                _playerState.value = _playerState.value.copy(repeatMode = stateMode)
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _playerState.value = _playerState.value.copy(
                    currentPosition = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                    duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
                )
                delay(500L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _localSongs.value = fetchSongsFromMediaStore()
            downloadedSongRepo.scanDownloadedSongs()
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun fetchSongsFromMediaStore(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val titleRaw = cursor.getString(titleCol) ?: "Unknown Title"
                    val artistRaw = cursor.getString(artistCol)
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)

                    val artist = if (artistRaw.isNullOrBlank() ||
                        artistRaw.equals("<unknown>", ignoreCase = true) ||
                        artistRaw.equals("unknown", ignoreCase = true)) {
                        "Unknown Artist"
                    } else {
                        artistRaw
                    }

                    var title = titleRaw
                    if (title.contains("VID_", ignoreCase = true) ||
                        title.contains("y2mate", ignoreCase = true) ||
                        title.contains("videoplayback", ignoreCase = true) ||
                        title.contains("audio", ignoreCase = true) ||
                        title.contains("-", ignoreCase = true)) {

                        title = title.replace(Regex("(?i)VID_\\d+_\\d+_\\d+"), "")
                            .replace(Regex("(?i)y2mate\\.com\\s*-\\s*"), "")
                            .replace(Regex("(?i)videoplayback\\s*\\(\\d+\\)"), "")
                            .replace(Regex("(?i)\\.mp3$"), "")
                            .replace("_", " ")
                            .trim()

                        if (title.equals("audio", ignoreCase = true) || title.isBlank()) {
                            title = "Unknown Title"
                        }
                    }

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val albumArtUri = "content://media/external/audio/albumart/$albumId".toUri()

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            uri = uri,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        return songs
    }

    fun playSong(song: Song, songList: List<Song>) {
        val index = songList.indexOf(song)
        if (index == -1) return

        ensurePlaybackService()
        
        viewModelScope.launch {
            dataStore.addRecentlyPlayed(song.id)
        }

        val mediaItems = songList.map { buildMediaItem(it) }
        controller?.let {
            it.setMediaItems(mediaItems, index, 0L)
            it.prepare()
            it.play()
        }

        _playerState.value = _playerState.value.copy(
            currentSong = song,
            isPlaying = true,
            currentPosition = 0L
        )
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                ensurePlaybackService()
                it.play()
            }
        }
    }

    fun next() {
        controller?.let {
            if (it.hasNextMediaItem()) it.seekToNextMediaItem()
        }
    }

    fun previous() {
        controller?.let {
            if (it.currentPosition > 3000L) {
                it.seekTo(0L)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _playerState.value = _playerState.value.copy(currentPosition = position)
    }

    fun toggleShuffle() {
        controller?.let {
            val newShuffle = !it.shuffleModeEnabled
            it.shuffleModeEnabled = newShuffle
            _playerState.value = _playerState.value.copy(shuffleEnabled = newShuffle)
        }
    }

    fun toggleRepeat() {
        controller?.let {
            val next = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> 1
                Player.REPEAT_MODE_ALL -> 2
                else -> 0
            }
            it.repeatMode = when (next) {
                1 -> Player.REPEAT_MODE_ALL
                2 -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            _playerState.value = _playerState.value.copy(repeatMode = next)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            dataStore.toggleFavorite(song.id)
        }
    }

    fun deleteDownloadedSong(song: Song) {
        if (song.source != com.lalit.amplify.core.model.SongSource.DOWNLOADED) return

        viewModelScope.launch {
            if (_playerState.value.currentSong?.uri == song.uri) {
                controller?.stop()
                _playerState.value = _playerState.value.copy(
                    currentSong = null,
                    isPlaying = false,
                    currentPosition = 0L,
                    duration = 0L
                )
            }
            downloadedSongRepo.deleteSong(song)
            _localSongs.value = fetchSongsFromMediaStore()
        }
    }

    private fun ensurePlaybackService() {
        MusicPlaybackService.start(context)
    }

    private fun buildMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(song.toMediaMetadata())
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}

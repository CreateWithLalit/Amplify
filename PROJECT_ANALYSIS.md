# Amplify Music Player - Project Analysis

## Executive Summary
Amplify is an **Android music player and downloader application** built with Jetpack Compose, Media3 (ExoPlayer), and Kotlin. It provides local music playback, internet music search, downloading capabilities, and library management features.

---

## ✅ WHAT THE APP CAN DO

### 1. **Music Playback & Playback Controls**
- ✅ Play local music files from the device's MediaStore
- ✅ Play downloaded music files
- ✅ Play/Pause functionality
- ✅ Skip to next/previous track
- ✅ Seek to specific position in a track
- ✅ Display current playback time and total duration
- ✅ Shuffle mode (enable/disable)
- ✅ Repeat modes (off, repeat all, repeat one)
- ✅ Progress tracking during playback
- ✅ Full player UI with album art display

### 2. **Music Library Management**
- ✅ Scan and list all local audio files from device storage
- ✅ Display local music library in the Library screen
- ✅ View music metadata (title, artist, album, duration)
- ✅ Display album artwork when available
- ✅ Filter music by:
  - All Music
  - Local files
  - Downloaded files
- ✅ Search within the library
- ✅ Sort options for songs
- ✅ Display song count in library

### 3. **Internet Music Search**
- ✅ Search for music by query
- ✅ Display search results with:
  - Song title
  - Artist name
  - Duration
  - Thumbnail/album art
  - Source label (YouTube, Spotify, etc.)
- ✅ View detailed information in expandable sheet
- ✅ Mock search results (demo functionality working)

### 4. **Music Download System**
- ✅ Download music from URL/stream
- ✅ Select audio quality (LOW, MEDIUM, HIGH)
- ✅ Download progress tracking with percentage
- ✅ Cancel ongoing downloads
- ✅ Download to:
  - SAF (Storage Access Framework) user-selected folders
  - MediaStore public Music directory
  - App's internal storage (fallback)
  - SD card (via tree URI)
- ✅ Duplicate detection (check if file already exists)
- ✅ Multiple duplicate strategies (KEEP_BOTH, REPLACE)
- ✅ Manual URL input for downloads
- ✅ Download state management (Idle, Preparing, Downloading, Processing, Success, Error)
- ✅ Foreground download service with notifications
- ✅ Auto-import downloaded music to library
- ✅ File format support: MP3, M4A, WAV, FLAC, OGG, AAC

### 5. **User Interface & Navigation**
- ✅ Bottom navigation bar with 4 main screens:
  - Home/Dashboard
  - Search
  - Library
  - Settings
- ✅ Dashboard screen with:
  - Quick play grid (6 songs)
  - Continue listening section (recently played)
  - Favorites section
  - Albums section
  - Suggested songs
- ✅ Mini player that appears above bottom nav when playing
- ✅ Full player screen with:
  - Album artwork
  - Song info
  - Playback controls
  - Shuffle and repeat toggles
  - Progress bar
- ✅ Responsive Jetpack Compose UI
- ✅ Dark theme (0xFF0A0A0A background)
- ✅ Material Design 3 components

### 6. **Favorites & Recently Played**
- ✅ Mark songs as favorites (star icon)
- ✅ View favorite songs in a dedicated section
- ✅ Track recently played songs
- ✅ Display recently played songs on dashboard
- ✅ Persist favorites and recently played in DataStore
- ✅ Max 20 recently played songs stored

### 7. **Permissions & Security**
- ✅ Request READ_MEDIA_AUDIO permission (Android 13+)
- ✅ Request READ_EXTERNAL_STORAGE permission (Android 12 and below)
- ✅ Request POST_NOTIFICATIONS permission
- ✅ Request FOREGROUND_SERVICE permissions for playback and downloads
- ✅ Graceful permission denial UI
- ✅ Permission state tracking

### 8. **Download Management (Downloader Screen)**
- ✅ Dedicated downloader activity
- ✅ Display downloadable track information
- ✅ Manual URL input option
- ✅ Select download destination folder via file picker
- ✅ Remember selected download folder
- ✅ Show download folder path
- ✅ Configure default audio quality
- ✅ Preview download before starting
- ✅ Download status display
- ✅ Handle upload/download errors

### 9. **Data Persistence**
- ✅ DataStore for user preferences (favorites, recently played)
- ✅ Download preferences (folder URI, audio quality)
- ✅ MediaStore integration for audio metadata
- ✅ Local music file discovery

### 10. **Settings**
- ✅ Settings screen with toggles for:
  - AMOLED mode (UI toggle implemented)
  - Animations
  - Auto-metadata fetching
  - Lyrics display
- ✅ Download settings:
  - Default quality selection
  - Download folder selection
- ✅ View and manage download destination

### 11. **Background Services**
- ✅ MusicPlaybackService - handles music playback in foreground
- ✅ MusicDownloadService - handles downloads in foreground
- ✅ Media3 session management
- ✅ Media button receiver for hardware controls
- ✅ Notifications for playback and downloads

### 12. **Audio Format Support**
- ✅ MP3 playback
- ✅ M4A playback
- ✅ WAV playback
- ✅ FLAC playback
- ✅ OGG playback
- ✅ AAC playback

### 13. **Download Folder Management**
- ✅ Choose custom download folder with file picker
- ✅ Support for:
  - Internal app storage
  - SAF user-selected folders
  - SD card via DocumentFile
  - Public Music directory via MediaStore

---

## ❌ WHAT THE APP CANNOT DO (Not Yet Implemented)

### 1. **Internet Search (Backend) - MOCK ONLY**
- ❌ **Real API integration** - Currently uses MockMusicSearchRepository
- ❌ Cannot actually search YouTube Music or any real music service
- ❌ Cannot fetch real stream URLs from internet sources
- ❌ Demo data only (static mock results)
- 📝 **Status**: Ready for implementation when API key/credentials available

### 2. **Content Metadata Enrichment**
- ❌ Cannot download song lyrics
- ❌ Cannot fetch album cover art from internet
- ❌ Cannot get real-time metadata updates
- ❌ Cannot display music artist profiles or bios

### 3. **User Authentication & Accounts**
- ❌ No user login/signup system
- ❌ No cloud sync of favorites/playlists
- ❌ No user profiles
- ❌ No multi-account support

### 4. **Advanced Playlist Features**
- ❌ Cannot create custom playlists
- ❌ Cannot edit playlists
- ❌ Cannot share playlists
- ❌ Cannot import/export playlists
- ❌ No collaborative playlists
- ❌ No playlist persistence across reinstalls

### 5. **Equalizer & Audio Processing**
- ❌ No audio equalizer
- ❌ No bass/treble adjustments
- ❌ No audio effects
- ❌ No 3D audio support
- ❌ No volume normalization

### 6. **Advanced Playback Features**
- ❌ No sleep timer
- ❌ No crossfade between tracks
- ❌ No gapless playback control
- ❌ No audio visualization

### 7. **Lyrics & Metadata Display**
- ❌ Cannot display song lyrics
- ❌ Cannot show real-time lyrics sync
- ❌ Limited metadata display (no extended info)
- ❌ Cannot edit song metadata

### 8. **Social Features**
- ❌ Cannot share songs to social media
- ❌ Cannot share playlists
- ❌ No social media integration
- ❌ No friend activity feed
- ❌ No ratings/reviews system

### 9. **Advanced Download Features**
- ❌ Cannot download entire albums at once
- ❌ Cannot download entire artist discography
- ❌ Cannot batch download playlists
- ❌ No download scheduling
- ❌ No resume on partial downloads
- ❌ No download history

### 10. **Music Streaming**
- ❌ Cannot stream music from external services (Spotify, Apple Music, etc.)
- ❌ No streaming from cloud storage (Google Drive, Dropbox, etc.)
- ❌ No internet radio
- ❌ No podcast support

### 11. **Advanced Search**
- ❌ No advanced search filters (by artist, genre, year, etc.)
- ❌ No search history
- ❌ No search suggestions/autocomplete
- ❌ No trending music display
- ❌ No recommendation algorithm

### 12. **Offline Features**
- ❌ Cannot work without permission requests
- ❌ No offline mode toggle
- ❌ No resource caching beyond local files

### 13. **UI/UX Advanced Features**
- ❌ No dark/light theme toggle (settings UI skeleton exists but not wired)
- ❌ No custom color themes
- ❌ No animation toggles that actually disable animations
- ❌ No tablet/landscape layout optimization

### 14. **Testing & QA**
- ❌ No unit tests implemented
- ❌ No integration tests
- ❌ No UI automated tests
- ❌ No error tracking/analytics

### 15. **Database Features**
- ❌ No Room database for persistent storage (only DataStore for preferences)
- ❌ No custom database models
- ❌ No data migration system

### 16. **API/Backend Integration** (Incomplete)
- ❌ No real music search API implementation
- ❌ No authentication API
- ❌ No user sync API
- ❌ No download link resolution from real sources

### 17. **Plugin/Extension System**
- ❌ No plugin support
- ❌ No theme extensions
- ❌ No custom visualizers

### 18. **Performance Features**
- ❌ No caching optimization for search results
- ❌ No bandwidth limiting for downloads
- ❌ No memory optimization options
- ❌ No lazy loading optimization

---

## 📊 Technology Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Music Playback**: Media3 (ExoPlayer)
- **Architecture**: MVVM with ViewModels
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Compose

### Libraries & Dependencies
- **Media Playing**: androidx.media3:media3-exoplayer
- **Session Management**: androidx.media3:media3-session
- **HTTP Client**: OkHttp + Logging Interceptor
- **Image Loading**: Coil
- **Data Persistence**: DataStore Preferences
- **Storage Access**: DocumentFile (SAF)
- **JSON**: Gson
- **Android Core**: AndroidX (Core KTX, Lifecycle, Activity Compose)

### Target API
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Build Tools**: Gradle with Kotlin DSL

---

## 🏗️ Project Structure

```
app/src/main/java/com/lalit/amplify/
├── core/
│   ├── data/              # DataStore and preferences
│   ├── media/             # Media player utilities
│   ├── model/             # Data models (Song, PlayerState)
│   ├── ui/                # Shared UI components
│   └── util/              # Utilities and helpers
├── feature/
│   ├── dashboard/         # Home screen
│   ├── downloader/        # Download management
│   │   ├── data/          # Download repository
│   │   ├── engine/        # Download engine
│   │   └── model/         # Download models
│   ├── library/           # Music library
│   ├── player/            # Playback and player UI
│   ├── search/            # Music search
│   └── settings/          # App settings
├── navigation/            # Navigation routes and host
├── service/               # Background services
├── ui/                    # Theme and styling
└── MainActivity.kt        # App entry point
```

---

## 🚀 Ready for Work

### High-Priority TODOs
1. **Implement real music search API** (replace MockMusicSearchRepository)
   - YouTube Music API or Spotify API
   - Stream URL resolution

2. **Add Room Database** for:
   - Persistent playlists
   - Download history tracking
   - Search history

3. **Implement Settings Toggles** 
   - Connect AMOLED mode to UI
   - Wire animation preferences
   - Connect lyrics preference to display logic

4. **Add Playlist Feature**
   - Create/Edit/Delete playlists
   - Drag-and-drop reordering
   - Playlist export/sharing

5. **Implement Quality Settings**
   - Actually use download quality preference
   - Fetch different quality streams

6. **Add Error Handling & Retry Logic**
   - Download retry mechanism
   - Better error messages

7. **Performance Optimization**
   - Lazy loading for large libraries
   - Image caching
   - Download resumption

### Medium-Priority TODOs
- Equalizer (either use MediaSession built-in or implement custom)
- Lyrics display
- Search history and suggestions
- Advanced filters in search
- Batch operations (multi-select downloads)
- Theme customization

### Testing Needed
- Unit tests for ViewModels
- Integration tests for download manager
- UI tests for navigation flows
- End-to-end testing for playback

---

## 📝 Configuration Notes

### Permissions Declared
- `INTERNET` - for API calls
- `ACCESS_NETWORK_STATE` - check connectivity
- `READ_MEDIA_AUDIO` - read music files (Android 13+)
- `READ_EXTERNAL_STORAGE` - read audio (legacy)
- `WRITE_EXTERNAL_STORAGE` - legacy write access
- `FOREGROUND_SERVICE` - background playback
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - playback service
- `FOREGROUND_SERVICE_DATA_SYNC` - download service
- `POST_NOTIFICATIONS` - notification permission

### Important Implementation Details
- Uses **DataStore** (not Room) for user preferences
- **Mock search repository** used for demo - needs real API
- **ContentResolver** used to scan MediaStore for local audio
- **DocumentFile** used for SAF folder access
- **MediaStore** used for downloaded files detection
- Auto-detects downloaded files in "Amplify" folders
- Download ID offset (9999999) to avoid collision with MediaStore IDs

---

## 🎯 Summary

**Amplify is a functional local music player with download capabilities.**

### Overall Capability Assessment
- ✅ **95% Complete** for local music playback and management
- ⚠️ **50% Complete** for internet music search (mock only)
- ⚠️ **70% Complete** for downloads (missing batch, resume, schedules)
- ⚠️ **20% Complete** for social/sharing features
- ⚠️ **10% Complete** for audio processing/effects

**Primary Strength**: Robust local playback with offline library management
**Primary Limitation**: No real external music source integration yet

---

**Analysis Date**: May 18, 2026  
**Project**: Amplify Music Player (Android)  
**Target Users**: Users who want a lightweight offline music player with download capabilities


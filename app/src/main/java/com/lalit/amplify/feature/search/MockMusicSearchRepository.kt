package com.lalit.amplify.feature.search

import kotlinx.coroutines.delay

/**
 * Mock implementation of MusicSearchRepository.
 * Returns realistic fake data so the UI is fully functional.
 *
 * REPLACE with YouTubeMusicSearchRepository when you have an API key.
 */
class MockMusicSearchRepository : MusicSearchRepository {

    override suspend fun search(query: String): Result<List<MusicSearchResult>> {
        delay(1200) // Simulate network latency

        if (query.isBlank()) return Result.success(emptyList())

        // Generate query-aware mock results
        val results = listOf(
            MusicSearchResult(
                id = "mock_1",
                title = "$query - Official Audio",
                artist = "Artist One",
                duration = 213_000L,
                thumbnailUrl = null,
                sourceLabel = "YouTube Music",
                webUrl = "https://youtube.com/watch?v=mock1"
            ),
            MusicSearchResult(
                id = "mock_2",
                title = "$query (Lyric Video)",
                artist = "Artist Two",
                duration = 198_000L,
                thumbnailUrl = null,
                sourceLabel = "YouTube",
                webUrl = "https://youtube.com/watch?v=mock2"
            ),
            MusicSearchResult(
                id = "mock_3",
                title = "Best of $query Mix",
                artist = "Various Artists",
                duration = 3600_000L,
                thumbnailUrl = null,
                sourceLabel = "YouTube",
                webUrl = "https://youtube.com/watch?v=mock3"
            ),
            MusicSearchResult(
                id = "mock_4",
                title = "$query - Live Performance",
                artist = "Artist One",
                duration = 245_000L,
                thumbnailUrl = null,
                sourceLabel = "YouTube",
                webUrl = "https://youtube.com/watch?v=mock4"
            ),
            MusicSearchResult(
                id = "mock_5",
                title = "$query Acoustic Cover",
                artist = "Cover Artist",
                duration = 187_000L,
                thumbnailUrl = null,
                sourceLabel = "YouTube",
                webUrl = "https://youtube.com/watch?v=mock5"
            )
        )
        return Result.success(results)
    }

    override suspend fun resolveDownloadLink(result: MusicSearchResult): Result<DownloadableTrack> {
        delay(800) // Simulate API call

        val track = DownloadableTrack(
            id = result.id,
            title = result.title,
            artist = result.artist,
            duration = result.duration,
            thumbnailUrl = result.thumbnailUrl,
            sourceLabel = result.sourceLabel,
            webUrl = result.webUrl,
            streamUrl = result.webUrl, // Placeholder - real impl would resolve this
            audioQuality = "320kbps",
            fileExtension = "mp3",
            contentType = "audio/mpeg"
        )
        return Result.success(track)
    }
}


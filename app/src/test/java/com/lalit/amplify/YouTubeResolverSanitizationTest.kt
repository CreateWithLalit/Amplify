package com.lalit.amplify

import com.lalit.amplify.feature.downloader.data.YouTubeResolverRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class YouTubeResolverSanitizationTest {

    private lateinit var repository: YouTubeResolverRepository

    @Before
    fun setUp() {
        repository = YouTubeResolverRepository()
    }

    @Test
    fun testExtractVideoIdStandardWatch() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", repository.extractVideoId(url))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", repository.sanitizeYouTubeUrl(url))
    }

    @Test
    fun testExtractVideoIdShorts() {
        val url = "https://youtube.com/shorts/dQw4w9WgXcQ?feature=share"
        assertEquals("dQw4w9WgXcQ", repository.extractVideoId(url))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", repository.sanitizeYouTubeUrl(url))
    }

    @Test
    fun testExtractVideoIdYouTuDotBeWithTracking() {
        val url = "https://youtu.be/dQw4w9WgXcQ?si=abcdef123456"
        assertEquals("dQw4w9WgXcQ", repository.extractVideoId(url))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", repository.sanitizeYouTubeUrl(url))
    }

    @Test
    fun testExtractVideoIdMobileAndPlaylist() {
        val url = "https://m.youtube.com/watch?v=dQw4w9WgXcQ&list=RDdQw4w9WgXcQ&index=1"
        assertEquals("dQw4w9WgXcQ", repository.extractVideoId(url))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", repository.sanitizeYouTubeUrl(url))
    }

    @Test
    fun testExtractVideoIdMusicYouTube() {
        val url = "https://music.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", repository.extractVideoId(url))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", repository.sanitizeYouTubeUrl(url))
    }

    @Test
    fun testInvalidUrl() {
        val url = "https://example.com/not-youtube"
        assertNull(repository.extractVideoId(url))
        assertEquals("https://example.com/not-youtube", repository.sanitizeYouTubeUrl(url))
    }

    @Test
    fun testBackendUrlConfiguration() {
        val defaultUrl = repository.getBackendUrl()
        assertNotNull(defaultUrl)

        repository.setBackendUrl("https://my-custom-backend.com/")
        assertEquals("https://my-custom-backend.com", repository.getBackendUrl())
    }
}

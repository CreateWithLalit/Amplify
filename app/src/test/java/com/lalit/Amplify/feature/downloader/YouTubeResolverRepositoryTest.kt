package com.lalit.amplify.feature.downloader

import com.lalit.amplify.feature.downloader.data.YouTubeResolverRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeResolverRepositoryTest {

    @Test
    fun parseErrorMessage_returnsSpecific401Message() {
        val message = YouTubeResolverRepository.parseErrorMessage(401, null)
        assertTrue(message.contains("401 Unauthorized"))
        assertTrue(message.contains("access denied"))
    }

    @Test
    fun parseErrorMessage_returnsSpecific403Message() {
        val message = YouTubeResolverRepository.parseErrorMessage(403, null)
        assertTrue(message.contains("403 Forbidden"))
        assertTrue(message.contains("forbidden"))
    }

    @Test
    fun parseErrorMessage_returnsSpecific404Message() {
        val message = YouTubeResolverRepository.parseErrorMessage(404, null)
        assertTrue(message.contains("404 Not Found"))
    }

    @Test
    fun parseErrorMessage_returnsSpecific429Message() {
        val message = YouTubeResolverRepository.parseErrorMessage(429, null)
        assertTrue(message.contains("429"))
        assertTrue(message.contains("Rate limit"))
    }

    @Test
    fun parseErrorMessage_returnsSpecific502Message() {
        val message = YouTubeResolverRepository.parseErrorMessage(502, null)
        assertTrue(message.contains("502 Bad Gateway"))
    }

    @Test
    fun parseErrorMessage_usesServerJsonMessageIfPresent() {
        val json = "{\"error\":\"Custom server bot block notice\"}"
        val message = YouTubeResolverRepository.parseErrorMessage(502, json)
        assertEquals("Custom server bot block notice", message)
    }
}

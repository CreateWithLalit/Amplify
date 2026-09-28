package com.lalit.amplify

import com.lalit.amplify.feature.player.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun testFormatZero() {
        assertEquals("0:00", formatDuration(0L))
    }

    @Test
    fun testFormatNegative() {
        assertEquals("0:00", formatDuration(-500L))
    }

    @Test
    fun testFormatSeconds() {
        assertEquals("0:45", formatDuration(45000L))
    }

    @Test
    fun testFormatMinutesAndSeconds() {
        assertEquals("3:25", formatDuration(205000L))
    }

    @Test
    fun testFormatLongTrack() {
        assertEquals("12:08", formatDuration(728000L))
    }
}

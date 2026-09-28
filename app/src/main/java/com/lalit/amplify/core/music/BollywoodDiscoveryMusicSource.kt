package com.lalit.amplify.core.music

import android.net.Uri
import com.lalit.amplify.core.model.PlaybackCapability
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Discovery Music Source for Bollywood and International Music.
 * Follows strict legal & licensing compliance: metadata is displayed with full
 * attribution and official external streaming links.
 */
class BollywoodDiscoveryMusicSource : MusicSource {

    override val sourceId: String = "bollywood-discovery"
    override val displayName: String = "Bollywood & Global"
    override val requiresInternet: Boolean = false

    private fun parseUri(url: String): Uri = Uri.parse(url)

    private val catalog by lazy {
        listOf(
            Song(
                id = 9001L,
                title = "Tum Hi Ho",
                artist = "Arijit Singh",
                album = "Aashiqui 2",
                duration = 262000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Tum+Hi+Ho+Arijit+Singh"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Tum+Hi+Ho+Arijit+Singh",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Bollywood Romance", "Soulful"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9002L,
                title = "Kun Faya Kun",
                artist = "A.R. Rahman, Javed Ali, Mohit Chauhan",
                album = "Rockstar",
                duration = 473000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Kun+Faya+Kun+Rockstar"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Kun+Faya+Kun+Rockstar",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Sufi & Semi-Classical", "Bollywood Romance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9003L,
                title = "Kesariya",
                artist = "Arijit Singh, Pritam",
                album = "Brahmastra",
                duration = 268000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Kesariya+Brahmastra"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Kesariya+Brahmastra",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Bollywood Romance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9004L,
                title = "Channa Mereya",
                artist = "Arijit Singh, Pritam",
                album = "Ae Dil Hai Mushkil",
                duration = 289000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Channa+Mereya"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Channa+Mereya",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Bollywood Romance", "Sufi & Semi-Classical"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9005L,
                title = "Ghungroo",
                artist = "Arijit Singh, Shilpa Rao, Vishal-Shekhar",
                album = "War",
                duration = 302000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Ghungroo+Song+War"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Ghungroo+Song+War",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Bollywood Party & Dance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9006L,
                title = "Ilahi",
                artist = "Arijit Singh, Pritam",
                album = "Yeh Jawaani Hai Deewani",
                duration = 229000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Ilahi+YJHD"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Ilahi+YJHD",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Indian Indie & Acoustic", "Bollywood Party & Dance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9007L,
                title = "Tere Hawale",
                artist = "Arijit Singh, Shilpa Rao, Pritam",
                album = "Laal Singh Chaddha",
                duration = 346000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Tere+Hawale+Laal+Singh+Chaddha"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Tere+Hawale+Laal+Singh+Chaddha",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Bollywood Romance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9008L,
                title = "Lover",
                artist = "Diljit Dosanjh, Intense",
                album = "MoonChild Era",
                duration = 195000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Lover+Diljit+Dosanjh"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Lover+Diljit+Dosanjh",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Punjabi Hits", "Bollywood Party & Dance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9009L,
                title = "Khairiyat",
                artist = "Arijit Singh, Pritam",
                album = "Chhichhore",
                duration = 280000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Khairiyat+Chhichhore"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Khairiyat+Chhichhore",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Bollywood Romance"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9010L,
                title = "Kabira",
                artist = "Tochi Raina, Rekha Bhardwaj, Pritam",
                album = "Yeh Jawaani Hai Deewani",
                duration = 223000L,
                uri = parseUri("https://www.youtube.com/results?search_query=Kabira+YJHD"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"),
                source = SongSource.BOLLYWOOD_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=Kabira+YJHD",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Sufi & Semi-Classical", "Indian Indie & Acoustic"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9011L,
                title = "Golden Hour (Acoustic)",
                artist = "JVKE",
                album = "This Is What Falling In Love Feels Like",
                duration = 209000L,
                uri = parseUri("https://www.youtube.com/results?search_query=JVKE+Golden+Hour"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500"),
                source = SongSource.INTERNATIONAL_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=JVKE+Golden+Hour",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Global Pop & Acoustic"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            ),
            Song(
                id = 9012L,
                title = "Daylight",
                artist = "David Kushner",
                album = "Daylight Single",
                duration = 212000L,
                uri = parseUri("https://www.youtube.com/results?search_query=David+Kushner+Daylight"),
                albumArtUri = parseUri("https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500"),
                source = SongSource.INTERNATIONAL_CATALOG,
                sourceUrl = "https://www.youtube.com/results?search_query=David+Kushner+Daylight",
                licenseInfo = "Official catalog metadata; listen via authorized platform",
                genres = listOf("Global Pop & Acoustic"),
                playbackCapability = PlaybackCapability.EXTERNAL_LINK
            )
        )
    }

    override suspend fun search(query: String, limit: Int, offset: Int): List<Song> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return catalog.drop(offset).take(limit)
        return catalog.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.genres.any { g -> g.lowercase().contains(q) }
        }.drop(offset).take(limit)
    }

    override suspend fun getTrending(limit: Int, offset: Int): List<Song> {
        return catalog.drop(offset).take(limit)
    }

    override suspend fun getByGenre(genre: String, limit: Int, offset: Int): List<Song> {
        val g = genre.lowercase()
        return catalog.filter { it.genres.any { item -> item.lowercase().contains(g) } }
            .drop(offset).take(limit)
    }

    override suspend fun getRecommendations(limit: Int): List<Song> {
        return catalog.shuffled().take(limit)
    }

    override fun getSongsFlow(): Flow<List<Song>> = flowOf(catalog)

    override suspend fun isAvailable(): Boolean = true

    companion object {
        val GENRES = listOf(
            "Bollywood Romance",
            "Bollywood Party & Dance",
            "Sufi & Semi-Classical",
            "Indian Indie & Acoustic",
            "Punjabi Hits",
            "Global Pop & Acoustic"
        )
    }
}

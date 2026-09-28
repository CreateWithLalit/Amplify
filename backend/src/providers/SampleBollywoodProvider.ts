import { MetadataProvider } from './MetadataProvider';
import { Track, GenreInfo } from '../types';

export class SampleBollywoodProvider implements MetadataProvider {
  name = 'amplify-bollywood-catalog';

  private genres: GenreInfo[] = [
    {
      id: 'bollywood-romance',
      name: 'Bollywood Romance',
      description: 'Soulful and timeless love songs from Hindi cinema',
      artworkUrl: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400'
    },
    {
      id: 'bollywood-dance',
      name: 'Bollywood Party & Dance',
      description: 'High-energy chartbusters and festive wedding beats',
      artworkUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400'
    },
    {
      id: 'sufi-classical',
      name: 'Sufi & Semi-Classical',
      description: 'Deep spiritual melodies and soulful acoustic arrangements',
      artworkUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400'
    },
    {
      id: 'indie-hindi',
      name: 'Indian Indie & Acoustic',
      description: 'Fresh independent singer-songwriters and acoustic tracks',
      artworkUrl: 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400'
    },
    {
      id: 'punjabi-pop',
      name: 'Punjabi Hits',
      description: 'Contemporary folk fusion and urban beats',
      artworkUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400'
    },
    {
      id: 'international-pop',
      name: 'Global Pop & Acoustic',
      description: 'Top international discovery and acoustic classics',
      artworkUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400'
    }
  ];

  private tracks: Track[] = [
    {
      id: 'bolly-001',
      source: 'bollywood-catalog',
      title: 'Tum Hi Ho',
      artist: 'Arijit Singh',
      album: 'Aashiqui 2',
      artworkUrl: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500',
      durationSeconds: 262,
      externalUrl: 'https://www.youtube.com/results?search_query=Tum+Hi+Ho+Arijit+Singh',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-romance'],
      year: 2013
    },
    {
      id: 'bolly-002',
      source: 'bollywood-catalog',
      title: 'Kun Faya Kun',
      artist: 'A.R. Rahman, Javed Ali, Mohit Chauhan',
      album: 'Rockstar',
      artworkUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500',
      durationSeconds: 473,
      externalUrl: 'https://www.youtube.com/results?search_query=Kun+Faya+Kun+Rockstar',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['sufi-classical', 'bollywood-romance'],
      year: 2011
    },
    {
      id: 'bolly-003',
      source: 'bollywood-catalog',
      title: 'Kesariya',
      artist: 'Arijit Singh, Pritam, Amitabh Bhattacharya',
      album: 'Brahmastra',
      artworkUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500',
      durationSeconds: 268,
      externalUrl: 'https://www.youtube.com/results?search_query=Kesariya+Brahmastra',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-romance'],
      year: 2022
    },
    {
      id: 'bolly-004',
      source: 'bollywood-catalog',
      title: 'Channa Mereya',
      artist: 'Arijit Singh, Pritam',
      album: 'Ae Dil Hai Mushkil',
      artworkUrl: 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500',
      durationSeconds: 289,
      externalUrl: 'https://www.youtube.com/results?search_query=Channa+Mereya',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-romance', 'sufi-classical'],
      year: 2016
    },
    {
      id: 'bolly-005',
      source: 'bollywood-catalog',
      title: 'Ghungroo',
      artist: 'Arijit Singh, Shilpa Rao, Vishal-Shekhar',
      album: 'War',
      artworkUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500',
      durationSeconds: 302,
      externalUrl: 'https://www.youtube.com/results?search_query=Ghungroo+Song+War',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-dance'],
      year: 2019
    },
    {
      id: 'bolly-006',
      source: 'bollywood-catalog',
      title: 'Ilahi',
      artist: 'Arijit Singh, Pritam',
      album: 'Yeh Jawaani Hai Deewani',
      artworkUrl: 'https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500',
      durationSeconds: 229,
      externalUrl: 'https://www.youtube.com/results?search_query=Ilahi+YJHD',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['indie-hindi', 'bollywood-dance'],
      year: 2013
    },
    {
      id: 'bolly-007',
      source: 'bollywood-catalog',
      title: 'Tere Hawale',
      artist: 'Arijit Singh, Shilpa Rao, Pritam',
      album: 'Laal Singh Chaddha',
      artworkUrl: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500',
      durationSeconds: 346,
      externalUrl: 'https://www.youtube.com/results?search_query=Tere+Hawale+Laal+Singh+Chaddha',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-romance'],
      year: 2022
    },
    {
      id: 'bolly-008',
      source: 'bollywood-catalog',
      title: 'Lover',
      artist: 'Diljit Dosanjh, Intense',
      album: 'MoonChild Era',
      artworkUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500',
      durationSeconds: 195,
      externalUrl: 'https://www.youtube.com/results?search_query=Lover+Diljit+Dosanjh',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['punjabi-pop', 'bollywood-dance'],
      year: 2021
    },
    {
      id: 'bolly-009',
      source: 'bollywood-catalog',
      title: 'Khairiyat',
      artist: 'Arijit Singh, Pritam',
      album: 'Chhichhore',
      artworkUrl: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500',
      durationSeconds: 280,
      externalUrl: 'https://www.youtube.com/results?search_query=Khairiyat+Chhichhore',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-romance'],
      year: 2019
    },
    {
      id: 'bolly-010',
      source: 'bollywood-catalog',
      title: 'Kabira',
      artist: 'Tochi Raina, Rekha Bhardwaj, Pritam',
      album: 'Yeh Jawaani Hai Deewani',
      artworkUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500',
      durationSeconds: 223,
      externalUrl: 'https://www.youtube.com/results?search_query=Kabira+YJHD',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['sufi-classical', 'indie-hindi'],
      year: 2013
    },
    {
      id: 'bolly-011',
      source: 'bollywood-catalog',
      title: 'Kasoor (Acoustic)',
      artist: 'Prateek Kuhad',
      album: 'Kasoor Single',
      artworkUrl: 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500',
      durationSeconds: 197,
      externalUrl: 'https://www.youtube.com/results?search_query=Kasoor+Prateek+Kuhad',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['indie-hindi'],
      year: 2020
    },
    {
      id: 'bolly-012',
      source: 'bollywood-catalog',
      title: 'Pee Loon',
      artist: 'Mohit Chauhan, Pritam',
      album: 'Once Upon A Time In Mumbaai',
      artworkUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500',
      durationSeconds: 288,
      externalUrl: 'https://www.youtube.com/results?search_query=Pee+Loon',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['bollywood-romance', 'sufi-classical'],
      year: 2010
    },
    {
      id: 'intl-001',
      source: 'international-catalog',
      title: 'Golden Hour (Acoustic)',
      artist: 'JVKE',
      album: 'This Is What Falling In Love Feels Like',
      artworkUrl: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500',
      durationSeconds: 209,
      externalUrl: 'https://www.youtube.com/results?search_query=JVKE+Golden+Hour',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['international-pop'],
      year: 2022
    },
    {
      id: 'intl-002',
      source: 'international-catalog',
      title: 'Daylight',
      artist: 'David Kushner',
      album: 'Daylight Single',
      artworkUrl: 'https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500',
      durationSeconds: 212,
      externalUrl: 'https://www.youtube.com/results?search_query=David+Kushner+Daylight',
      playbackCapability: 'EXTERNAL_LINK',
      licenseOrAttribution: 'Metadata authorized under fair catalog reference; streaming via official platforms',
      genres: ['international-pop'],
      year: 2023
    }
  ];

  async searchTracks(query: string, page = 0, limit = 20): Promise<Track[]> {
    const q = query.toLowerCase().trim();
    if (!q) return this.tracks.slice(page * limit, (page + 1) * limit);

    return this.tracks
      .filter(t =>
        t.title.toLowerCase().includes(q) ||
        t.artist.toLowerCase().includes(q) ||
        t.album.toLowerCase().includes(q) ||
        (t.genres && t.genres.some(g => g.toLowerCase().includes(q)))
      )
      .slice(page * limit, (page + 1) * limit);
  }

  async getTrackDetails(id: string): Promise<Track | null> {
    return this.tracks.find(t => t.id === id) || null;
  }

  async getTrendingTracks(limit = 20): Promise<Track[]> {
    return this.tracks.slice(0, limit);
  }

  async getGenres(): Promise<GenreInfo[]> {
    return this.genres;
  }

  async getTracksByGenre(genreId: string, page = 0, limit = 20): Promise<Track[]> {
    return this.tracks
      .filter(t => t.genres && t.genres.includes(genreId))
      .slice(page * limit, (page + 1) * limit);
  }
}

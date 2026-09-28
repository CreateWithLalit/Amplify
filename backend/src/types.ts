export type PlaybackCapability = 'DIRECT_STREAM' | 'EXTERNAL_LINK' | 'SDK_REQUIRED';

export interface Track {
  id: string;
  source: string;
  title: string;
  artist: string;
  album: string;
  artworkUrl: string;
  durationSeconds: number;
  externalUrl: string;
  playbackCapability: PlaybackCapability;
  licenseOrAttribution?: string | null;
  directStreamUrl?: string | null;
  genres?: string[];
  year?: number;
}

export interface GenreInfo {
  id: string;
  name: string;
  description: string;
  artworkUrl?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  total?: number;
  source?: string;
}

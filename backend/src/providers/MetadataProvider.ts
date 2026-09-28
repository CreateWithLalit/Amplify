import { Track, GenreInfo } from '../types';

export interface MetadataProvider {
  name: string;
  searchTracks(query: string, page?: number, limit?: number): Promise<Track[]>;
  getTrackDetails(id: string): Promise<Track | null>;
  getTrendingTracks(limit?: number): Promise<Track[]>;
  getGenres(): Promise<GenreInfo[]>;
  getTracksByGenre(genreId: string, page?: number, limit?: number): Promise<Track[]>;
}

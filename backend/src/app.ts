import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { SampleBollywoodProvider } from './providers/SampleBollywoodProvider';
import { rateLimiter } from './middleware/rateLimiter';
import { ApiResponse } from './types';

const app = express();
const bollywoodProvider = new SampleBollywoodProvider();

app.use(cors());
app.use(express.json());
app.use(rateLimiter);

// Request logger
app.use((req: Request, _res: Response, next: NextFunction) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  next();
});

// Root endpoint with API directory
app.get('/', (_req: Request, res: Response) => {
  res.json({
    name: 'Amplify Authorized Music Metadata Service',
    status: 'online',
    version: '1.0.0',
    documentation: {
      health: 'GET /api/health',
      search: 'GET /api/search?q=:query',
      trending: 'GET /api/trending',
      track: 'GET /api/track/:id',
      genres: 'GET /api/genres',
      genreTracks: 'GET /api/genres/:genreId/tracks'
    }
  });
});

// GET /api/health
app.get('/api/health', (_req: Request, res: Response) => {
  const response: ApiResponse<{ status: string; uptime: number; timestamp: string }> = {
    success: true,
    data: {
      status: 'healthy',
      uptime: process.uptime(),
      timestamp: new Date().toISOString()
    }
  };
  res.json(response);
});

// GET /api/search?q=&page=&limit=
app.get('/api/search', async (req: Request, res: Response) => {
  try {
    const query = String(req.query.q || '').trim();
    const page = Math.max(0, parseInt(String(req.query.page || '0'), 10) || 0);
    const limit = Math.min(50, Math.max(1, parseInt(String(req.query.limit || '20'), 10) || 20));

    if (!query) {
      return res.status(400).json({
        success: false,
        error: 'Query parameter "q" is required'
      });
    }

    const tracks = await bollywoodProvider.searchTracks(query, page, limit);

    return res.json({
      success: true,
      data: tracks,
      total: tracks.length,
      source: bollywoodProvider.name
    });
  } catch (error: any) {
    console.error('Search error:', error);
    return res.status(500).json({
      success: false,
      error: error.message || 'Internal server error while searching'
    });
  }
});

// GET /api/trending?limit=
app.get('/api/trending', async (req: Request, res: Response) => {
  try {
    const limit = Math.min(50, Math.max(1, parseInt(String(req.query.limit || '20'), 10) || 20));
    const tracks = await bollywoodProvider.getTrendingTracks(limit);

    return res.json({
      success: true,
      data: tracks,
      total: tracks.length,
      source: bollywoodProvider.name
    });
  } catch (error: any) {
    console.error('Trending error:', error);
    return res.status(500).json({
      success: false,
      error: error.message || 'Internal server error while fetching trending tracks'
    });
  }
});

// GET /api/track/:id
app.get('/api/track/:id', async (req: Request, res: Response) => {
  try {
    const id = req.params.id;
    if (!id) {
      return res.status(400).json({
        success: false,
        error: 'Track ID is required'
      });
    }

    const track = await bollywoodProvider.getTrackDetails(id);
    if (!track) {
      return res.status(404).json({
        success: false,
        error: `Track with ID "${id}" was not found`
      });
    }

    return res.json({
      success: true,
      data: track,
      source: bollywoodProvider.name
    });
  } catch (error: any) {
    console.error('Track details error:', error);
    return res.status(500).json({
      success: false,
      error: error.message || 'Internal server error while fetching track'
    });
  }
});

// GET /api/genres
app.get('/api/genres', async (_req: Request, res: Response) => {
  try {
    const genres = await bollywoodProvider.getGenres();
    return res.json({
      success: true,
      data: genres,
      total: genres.length
    });
  } catch (error: any) {
    console.error('Genres error:', error);
    return res.status(500).json({
      success: false,
      error: error.message || 'Internal server error while fetching genres'
    });
  }
});

// GET /api/genres/:genreId/tracks
app.get('/api/genres/:genreId/tracks', async (req: Request, res: Response) => {
  try {
    const genreId = req.params.genreId;
    const page = Math.max(0, parseInt(String(req.query.page || '0'), 10) || 0);
    const limit = Math.min(50, Math.max(1, parseInt(String(req.query.limit || '20'), 10) || 20));

    const tracks = await bollywoodProvider.getTracksByGenre(genreId, page, limit);

    return res.json({
      success: true,
      data: tracks,
      total: tracks.length,
      source: bollywoodProvider.name
    });
  } catch (error: any) {
    console.error('Genre tracks error:', error);
    return res.status(500).json({
      success: false,
      error: error.message || 'Internal server error while fetching genre tracks'
    });
  }
});

// 404 handler
app.use((_req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: 'Endpoint not found'
  });
});

// 500 error handler
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error('Unhandled express error:', err);
  res.status(500).json({
    success: false,
    error: 'Internal server error'
  });
});

export default app;

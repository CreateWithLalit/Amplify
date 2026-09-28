import { Request, Response, NextFunction } from 'express';

interface RateLimitStore {
  [ip: string]: { count: number; resetTime: number };
}

const store: RateLimitStore = {};
const WINDOW_MS = 60 * 1000; // 1 minute
const MAX_REQUESTS = 120; // 120 requests per minute per IP

export function rateLimiter(req: Request, res: Response, next: NextFunction): void {
  const ip = req.ip || req.socket.remoteAddress || 'anonymous';
  const now = Date.now();

  if (!store[ip] || now > store[ip].resetTime) {
    store[ip] = { count: 1, resetTime: now + WINDOW_MS };
    return next();
  }

  store[ip].count++;

  if (store[ip].count > MAX_REQUESTS) {
    res.status(429).json({
      success: false,
      error: 'Rate limit exceeded. Please wait a moment before sending more requests.'
    });
    return;
  }

  next();
}

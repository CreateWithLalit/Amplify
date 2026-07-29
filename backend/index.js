const express = require('express');
const { spawn } = require('child_process');
const fs = require('fs');
const NodeCache = require('node-cache');

const app = express();
const port = process.env.PORT || 8080;
const cache = new NodeCache({ stdTTL: 21600 });

app.use(express.json({ limit: '16kb' }));
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);
  next();
});

function ytDlpCommand() {
  return fs.existsSync('./yt-dlp') ? './yt-dlp' : 'yt-dlp';
}

function isYouTubeUrl(value) {
  try {
    const host = new URL(value).hostname.toLowerCase();
    return host === 'youtu.be' || host.endsWith('.youtube.com') || host === 'youtube.com';
  } catch {
    return false;
  }
}

function metadataFor(url) {
  return new Promise((resolve, reject) => {
    const process = spawn(ytDlpCommand(), ['--dump-single-json', '--no-playlist', '--no-warnings', url]);
    let stdout = '';
    let stderr = '';
    process.stdout.on('data', data => { stdout += data; });
    process.stderr.on('data', data => { stderr += data; });
    process.on('error', reject);
    process.on('close', code => {
      if (code !== 0) return reject(new Error(stderr || `yt-dlp exited with ${code}`));
      try { resolve(JSON.parse(stdout)); } catch (error) { reject(error); }
    });
  });
}

function cleanFilename(title) {
  return (title || 'audio').replace(/[\\/:*?"<>|]/g, '').trim().slice(0, 120) || 'audio';
}

function readRequest(req) {
  // POST is the supported mobile API. GET keeps already released app builds working.
  return req.method === 'GET' ? req.query : req.body;
}

app.get('/', (_req, res) => res.json({
  status: 'online',
  endpoints: { resolve: 'POST /resolve', download: 'POST /download' }
}));

app.all('/resolve', async (req, res) => {
  const { url } = readRequest(req);
  if (!isYouTubeUrl(url)) return res.status(400).json({ error: 'A valid YouTube URL is required.' });

  try {
    let info = cache.get(`metadata:${url}`);
    if (!info) {
      info = await metadataFor(url);
      cache.set(`metadata:${url}`, info);
    }
    res.json({
      title: info.title || 'Unknown title',
      artist: info.uploader || info.channel || 'Unknown artist',
      duration: info.duration || 0,
      thumbnail: info.thumbnail || null,
      formats: ['low', 'medium', 'high']
    });
  } catch (error) {
    console.error('Resolve error:', error.message);
    res.status(502).json({ error: 'Could not resolve this YouTube URL.' });
  }
});

app.all('/download', async (req, res) => {
  const { url, quality = 'medium' } = readRequest(req);
  if (!isYouTubeUrl(url)) return res.status(400).json({ error: 'A valid YouTube URL is required.' });

  const bitrate = { low: '128k', medium: '192k', high: '320k' }[String(quality).toLowerCase()] || '192k';
  let title = 'audio';
  try {
    const info = cache.get(`metadata:${url}`) || await metadataFor(url);
    cache.set(`metadata:${url}`, info);
    title = cleanFilename(info.title);
  } catch (error) {
    console.warn('Metadata unavailable for download:', error.message);
  }

  res.setHeader('Content-Type', 'audio/mpeg');
  res.setHeader('Content-Disposition', `attachment; filename="${title}.mp3"`);

  const ytProcess = spawn(ytDlpCommand(), [
    '--no-playlist', '--no-warnings', '-f', 'bestaudio',
    '--extract-audio', '--audio-format', 'mp3',
    '--postprocessor-args', `ffmpeg:-b:a ${bitrate}`,
    '-o', '-', url
  ]);
  ytProcess.stdout.pipe(res);
  ytProcess.stderr.on('data', data => console.error(`yt-dlp: ${data}`));
  ytProcess.on('error', error => {
    console.error('Download process error:', error.message);
    if (!res.headersSent) res.status(500).json({ error: 'Could not start yt-dlp.' });
  });
  ytProcess.on('close', code => { if (code !== 0) console.error(`yt-dlp exited with ${code}`); });
  req.on('close', () => { if (!res.writableEnded) ytProcess.kill('SIGTERM'); });
});

app.listen(port, '0.0.0.0', () => console.log(`Amplify resolver listening on ${port}`));
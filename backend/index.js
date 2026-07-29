const express = require('express');
const { spawn } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
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
      // Music uploads often expose the singer in `artist` or `creators`; fall
      // back to the channel/uploader only when YouTube supplies no artist data.
      artist: info.artist || info.creators?.join(', ') || info.uploader || info.channel || 'Unknown artist',
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

  // Download to an isolated temporary directory first. Post-processing cannot
  // reliably embed album art into an MP3 written directly to stdout.
  const jobDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'amplify-'));
  const outputTemplate = path.join(jobDirectory, 'audio.%(ext)s');
  let audioStream;
  let clientDisconnected = false;
  const cleanup = () => fs.rm(jobDirectory, { recursive: true, force: true }, () => {});
  const ytProcess = spawn(ytDlpCommand(), [
    '--no-playlist', '--no-warnings', '-f', 'bestaudio',
    '--extract-audio', '--audio-format', 'mp3',
    '--embed-thumbnail', '--convert-thumbnails', 'jpg', '--add-metadata',
    '--postprocessor-args', `ffmpeg:-b:a ${bitrate}`,
    '-o', outputTemplate, url
  ]);
  ytProcess.stderr.on('data', data => console.error(`yt-dlp: ${data}`));
  ytProcess.on('error', error => {
    console.error('Download process error:', error.message);
    cleanup();
    if (!res.headersSent) res.status(500).json({ error: 'Could not start yt-dlp.' });
  });
  ytProcess.on('close', code => {
    if (clientDisconnected) return cleanup();
    if (code !== 0) {
      console.error(`yt-dlp exited with ${code}`);
      cleanup();
      return res.status(502).json({ error: 'Could not create the audio file.' });
    }

    const audioPath = path.join(jobDirectory, 'audio.mp3');
    if (!fs.existsSync(audioPath)) {
      cleanup();
      return res.status(502).json({ error: 'Audio conversion did not produce an MP3 file.' });
    }

    res.setHeader('Content-Type', 'audio/mpeg');
    res.setHeader('Content-Disposition', `attachment; filename="${title}.mp3"`);
    res.setHeader('Content-Length', fs.statSync(audioPath).size);
    audioStream = fs.createReadStream(audioPath);
    audioStream.on('error', error => {
      console.error('Audio stream error:', error.message);
      cleanup();
      if (!res.headersSent) res.status(500).json({ error: 'Could not stream the audio file.' });
      else res.destroy(error);
    });
    audioStream.pipe(res);
  });

  res.on('close', () => {
    clientDisconnected = true;
    if (ytProcess.exitCode == null) ytProcess.kill('SIGTERM');
    audioStream?.destroy();
    cleanup();
  });
  res.on('finish', cleanup);
});

app.listen(port, '0.0.0.0', () => console.log(`Amplify resolver listening on ${port}`));

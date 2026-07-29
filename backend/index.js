const express = require('express');
const { spawn } = require('child_process');
const axios = require('axios');
const NodeCache = require('node-cache');

const app = express();
const port = process.env.PORT || 8080;
const cache = new NodeCache({ stdTTL: 21600 }); // 6 hours cache

// Middleware to log requests
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
    next();
});

// Root route for status check
app.get('/', (req, res) => {
    res.json({
        status: "online",
        message: "Amplify YouTube Resolver is running",
        endpoints: {
            resolve: "/resolve?url=<youtube_url>",
            stream: "/stream?url=<youtube_url>"
        }
    });
});

// Helper to run yt-dlp
function getYtDlpMetadata(url) {
    return new Promise((resolve, reject) => {
        // Use local ./yt-dlp if it exists, otherwise fallback to global
        const command = require('fs').existsSync('./yt-dlp') ? './yt-dlp' : 'yt-dlp';

        const ytDlp = spawn(command, [
            '--dump-json',
            '--no-playlist',
            '--no-warnings',
            '--no-check-certificate',
            '--extract-audio',
            '--audio-format', 'mp3',
            url
        ]);

        let output = '';
        let error = '';

        ytDlp.stdout.on('data', (data) => {
            output += data;
        });

        ytDlp.stderr.on('data', (data) => {
            error += data;
        });

        ytDlp.on('close', (code) => {
            if (code !== 0) {
                reject(new Error(error || `yt-dlp exited with code ${code}`));
                return;
            }
            try {
                resolve(JSON.parse(output));
            } catch (e) {
                reject(e);
            }
        });
    });
}

// GET /resolve?url=<youtube_url>
app.get('/resolve', async (req, res) => {
    const videoUrl = req.query.url;
    if (!videoUrl) {
        return res.status(400).json({ error: 'Missing url parameter' });
    }

    const cachedData = cache.get(videoUrl);
    if (cachedData) {
        return res.json(cachedData);
    }

    try {
        const metadata = await getYtDlpMetadata(videoUrl);

        const result = {
            title: metadata.title,
            artist: metadata.uploader,
            thumbnailUrl: metadata.thumbnail,
            durationSeconds: metadata.duration,
            audioStreamUrl: metadata.url,
            expiresInSeconds: 21600
        };

        cache.set(videoUrl, result);
        res.json(result);
    } catch (error) {
        console.error('Resolve Error:', error.message);
        res.status(500).json({ error: 'Failed to resolve YouTube URL', message: error.message });
    }
});

// GET /stream?url=<youtube_url>
// Mode A: Proxying the stream
app.get('/stream', async (req, res) => {
    const videoUrl = req.query.url;
    if (!videoUrl) {
        return res.status(400).send('Missing url parameter');
    }

    try {
        // Spawn yt-dlp to extract audio and output MP3 to stdout, piping to response
        const command = require('fs').existsSync('./yt-dlp') ? './yt-dlp' : 'yt-dlp';
        const ytDlp = spawn(command, [
            '--no-playlist',
            '--no-warnings',
            '--no-check-certificate',
            '-o', '-',
            '--extract-audio',
            '--audio-format', 'mp3',
            videoUrl
        ]);

        res.setHeader('Content-Type', 'audio/mpeg');

        ytDlp.stdout.pipe(res);

        ytDlp.stderr.on('data', (data) => {
            console.error('yt-dlp:', data.toString());
        });

        ytDlp.on('close', (code) => {
            if (code !== 0) {
                console.error(`yt-dlp exited with code ${code}`);
            }
        });
    } catch (error) {
        console.error('Stream Error:', error.message);
        res.status(500).send('Failed to stream audio');
    }
});

app.listen(port, '0.0.0.0', () => {
    console.log(`Amplify Resolver Backend listening at http://0.0.0.0:${port}`);
});

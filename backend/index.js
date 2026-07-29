const express = require('express');
const { spawn } = require('child_process');
const axios = require('axios');
const NodeCache = require('node-cache');

const app = express();
const port = process.env.PORT || 3000;
const cache = new NodeCache({ stdTTL: 21600 }); // 6 hours cache

// Middleware to log requests
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
    next();
});

// Helper to run yt-dlp
function getYtDlpMetadata(url) {
    return new Promise((resolve, reject) => {
        const ytDlp = spawn('yt-dlp', [
            '--dump-json',
            '--no-playlist',
            '-f', 'ba[ext=m4a]/ba',
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
        let streamUrl;
        const cachedData = cache.get(videoUrl);

        if (cachedData) {
            streamUrl = cachedData.audioStreamUrl;
        } else {
            const metadata = await getYtDlpMetadata(videoUrl);
            streamUrl = metadata.url;
        }

        const response = await axios({
            method: 'get',
            url: streamUrl,
            responseType: 'stream',
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
            }
        });

        // Forward headers
        res.setHeader('Content-Type', response.headers['content-type'] || 'audio/mp4');
        if (response.headers['content-length']) {
            res.setHeader('Content-Length', response.headers['content-length']);
        }

        response.data.pipe(res);
    } catch (error) {
        console.error('Stream Error:', error.message);
        res.status(500).send('Failed to stream audio');
    }
});

app.listen(port, () => {
    console.log(`Amplify Resolver Backend listening at http://localhost:${port}`);
});

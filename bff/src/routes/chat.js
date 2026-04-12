const express = require('express');
const axios = require('axios');

const router = express.Router();
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

router.post('/', async (req, res) => {
  const { message, sessionId } = req.body;

  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.flushHeaders();

  try {
    const upstream = await axios.post(
      `${BACKEND_URL}/api/chat/stream`,
      { message, sessionId },
      {
        responseType: 'stream',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
        },
      }
    );

    upstream.data.on('data', (chunk) => res.write(chunk));
    upstream.data.on('end', () => {
      res.write('data: [DONE]\n\n');
      res.end();
    });
    upstream.data.on('error', (err) => {
      console.error('Upstream stream error:', err.message);
      res.end();
    });
  } catch (err) {
    console.error('Backend request failed:', err.message);
    res.write(`data: [ERROR] ${err.message}\n\n`);
    res.end();
  }
});

module.exports = router;

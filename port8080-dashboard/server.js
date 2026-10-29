const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

// Increase body limit to handle high-resolution camera frames (e.g., 10MB)
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));
app.use(express.raw({ type: 'image/jpeg', limit: '10mb' })); // For raw binary JPEG uploads

// Serve the laptop webpage UI from 'public' folder
app.use(express.static(path.join(__dirname, 'public')));

// --- RECEIVE STREAM FROM YOUR JAVA APP ---
// Option A: If your Java app sends frames via HTTP POST (e.g., sending JPEG bytes or base64)
app.post('/upload', (req, res) => {
    let frameBuffer = req.body;

    // If your Java app sends raw binary JPEG bytes
    if (Buffer.isBuffer(frameBuffer)) {
        broadcastFrame(frameBuffer);
    } 
    // If your Java app sends JSON with base64 image data
    else if (req.body && req.body.image) {
        broadcastFrame(Buffer.from(req.body.image, 'base64'));
    }

    res.status(200).send({ status: 'received' });
});

// Helper function to broadcast frame to all connected browser tabs
function broadcastFrame(buffer) {
    wss.clients.forEach(client => {
        if (client.readyState === 1) { // WebSocket.OPEN
            client.send(buffer);
        }
    });
}

// WebSocket connection for the laptop webpage viewer
wss.on('connection', (ws) => {
    console.log('Laptop browser connected to stream viewer');
    ws.on('close', () => console.log('Browser disconnected'));
});

const PORT = 8080;
server.listen(PORT, () => {
    console.log(`==================================================`);
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`--------------------------------------------------`);
    console.log(`💻 Open Webpage:      http://localhost:${PORT}`);
    console.log(`📥 Java App Endpoint: http://<your-laptop-ip>:8080/upload`);
    console.log(`==================================================\n`);
});
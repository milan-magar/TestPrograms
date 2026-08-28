// server.js
const WebSocket = require('ws');
const http = require('http');
const fs = require('fs');
const path = require('path');

// Serve the HTML file
const server = http.createServer((req, res) => {
    if (req.url === '/') {
        fs.readFile(path.join(__dirname, 'index.html'), (err, data) => {
            if (err) {
                res.writeHead(500);
                res.end('Error loading index.html');
                return;
            }
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end(data);
        });
    } else {
        res.writeHead(404);
        res.end('Not found');
    }
});

// Attach WebSocket server
const wss = new WebSocket.Server({ server });

// Store rooms: roomId -> { display: ws, controllers: [ws, ...] }
const rooms = new Map();

wss.on('connection', (ws, req) => {
    let currentRoom = null;
    let currentMode = null;

    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            const { type, mode, room, slide } = data;

            if (type === 'join') {
                currentRoom = room;
                currentMode = mode;

                if (!rooms.has(room)) {
                    rooms.set(room, { display: null, controllers: [] });
                }
                const roomData = rooms.get(room);

                if (mode === 'display') {
                    // If a display already exists, disconnect the old one
                    if (roomData.display) {
                        try { roomData.display.close(); } catch (e) {}
                    }
                    roomData.display = ws;
                    ws.send(JSON.stringify({ type: 'joined', mode: 'display', slide: 0 }));
                } else if (mode === 'controller') {
                    roomData.controllers.push(ws);
                    // Send current slide to the new controller
                    ws.send(JSON.stringify({ type: 'joined', mode: 'controller', slide: 0 }));
                    // If a display exists, ask it to sync its current slide
                    if (roomData.display) {
                        try {
                            roomData.display.send(JSON.stringify({ type: 'sync_request' }));
                        } catch (e) {}
                    }
                }
                return;
            }

            // If we have a room, forward messages to the display
            if (currentRoom && rooms.has(currentRoom)) {
                const roomData = rooms.get(currentRoom);
                const target = roomData.display;

                if (target && target.readyState === WebSocket.OPEN) {
                    // Forward controller commands to the display
                    if (currentMode === 'controller') {
                        target.send(JSON.stringify(data));
                    }
                    // If display sends sync, broadcast to all controllers
                    if (currentMode === 'display' && (type === 'sync' || type === 'goto')) {
                        roomData.controllers.forEach(ctrl => {
                            if (ctrl.readyState === WebSocket.OPEN) {
                                ctrl.send(JSON.stringify(data));
                            }
                        });
                    }
                }
            }
        } catch (err) {
            console.warn('Message error:', err);
        }
    });

    ws.on('close', () => {
        // Cleanup disconnected clients
        if (currentRoom && rooms.has(currentRoom)) {
            const roomData = rooms.get(currentRoom);
            if (roomData.display === ws) {
                roomData.display = null;
            }
            const index = roomData.controllers.indexOf(ws);
            if (index > -1) {
                roomData.controllers.splice(index, 1);
            }
            // Delete empty rooms
            if (!roomData.display && roomData.controllers.length === 0) {
                rooms.delete(currentRoom);
            }
        }
    });
});

const PORT = 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
    console.log(`📡 WebSocket ready on ws://localhost:${PORT}`);
    console.log(`🌐 Open on your laptop. Find your local IP using "ipconfig" (Windows) or "ifconfig" (Mac/Linux).`);
});
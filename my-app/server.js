const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

let users = [
  { id: 1, name: 'Alice' },
  { id: 2, name: 'Bob' }
];

// ---------- SSE client list ----------
let clients = [];

function sendJSON(res, statusCode, data) {
  res.statusCode = statusCode;
  res.setHeader('Content-Type', 'application/json');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.end(JSON.stringify(data));
}

function serveStatic(res, filePath, contentType) {
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.statusCode = 404;
      res.end('File not found');
    } else {
      res.statusCode = 200;
      res.setHeader('Content-Type', contentType);
      res.end(data);
    }
  });
}

const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = parsedUrl.pathname;
  const method = req.method;

  console.log(`${method} ${pathname}`);

  // ---------- SSE endpoint ----------
  if (pathname === '/events') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*'
    });

    // Send an initial message to confirm connection
    res.write('data: connected\n\n');

    // Store the client connection
    clients.push(res);

    // Remove client when connection closes
    req.on('close', () => {
      clients = clients.filter(client => client !== res);
    });

    return; // Do not continue to other routes
  }

  // ---------- API routes ----------
  if (pathname === '/users') {
    if (method === 'GET') {
      const id = parsedUrl.searchParams.get('id');
      if (id) {
        const user = users.find(u => u.id === parseInt(id));
        if (user) sendJSON(res, 200, user);
        else sendJSON(res, 404, { error: 'User not found' });
      } else {
        sendJSON(res, 200, users);
      }
    }
    else if (method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const newUser = JSON.parse(body);
          console.log('📥 Received POST data:', newUser);

          if (!newUser.name) {
            sendJSON(res, 400, { error: 'Name is required' });
            return;
          }
          const user = { id: users.length + 1, name: newUser.name };
          users.push(user);
          console.log('✅ Added user:', user);

          // ---------- Broadcast the new user to all SSE clients ----------
          const message = `data: ${JSON.stringify({ type: 'newUser', user })}\n\n`;
          clients.forEach(client => client.write(message));

          sendJSON(res, 201, user);
        } catch (err) {
          console.error('❌ Invalid JSON:', err.message);
          sendJSON(res, 400, { error: 'Invalid JSON' });
        }
      });
    }
    else {
      sendJSON(res, 405, { error: 'Method not allowed' });
    }
  }
  // ---------- Static files ----------
  else if (pathname === '/') {
    serveStatic(res, path.join(__dirname, 'index.html'), 'text/html');
  }
  else if (pathname === '/style.css') {
    serveStatic(res, path.join(__dirname, 'style.css'), 'text/css');
  }
  else if (pathname === '/script.js') {
    serveStatic(res, path.join(__dirname, 'script.js'), 'application/javascript');
  }
  else {
    sendJSON(res, 404, { error: 'Not found' });
  }
});

const PORT = 3000;
server.listen(PORT, () => {
  console.log(`🚀 Server running at http://localhost:${PORT}`);
});
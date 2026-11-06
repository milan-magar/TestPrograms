'use strict';
const path = require('path');
const fs   = require('fs');
const http = require('http');
const https = require('https');
const os   = require('os');
const express = require('express');
const { Server } = require('socket.io');

const PORT      = Number(process.env.PORT) || 3000;
const HOST      = '0.0.0.0';
const USE_HTTPS = process.argv.includes('--https');
const CERT_DIR  = path.join(__dirname, 'certs');

const app = express();
app.use(express.static(path.join(__dirname, 'public')));

function detectLocalIPv4() {
  const nets = os.networkInterfaces();
  const cands = [];
  for (const n of Object.keys(nets))
    for (const i of nets[n] || [])
      if ((i.family === 'IPv4' || i.family === 4) && !i.internal) cands.push(i.address);
  return cands.find(a => /^192\.168\./.test(a))
      || cands.find(a => /^10\./.test(a))
      || cands.find(a => /^172\.(1[6-9]|2\d|3[01])\./.test(a))
      || cands[0] || '127.0.0.1';
}
const LAN_IP = detectLocalIPv4();

app.get('/api/info', (req, res) => {
  const proto = USE_HTTPS ? 'https' : 'http';
  res.json({
    protocol: proto, ip: LAN_IP, port: PORT,
    desktopUrl: `${proto}://localhost:${PORT}/?mode=desktop`,
    mobileUrl:  `${proto}://${LAN_IP}:${PORT}/?mode=mobile`
  });
});

async function start() {
  let server;
  if (USE_HTTPS) {
    try {
      const selfsigned = require('selfsigned');
      fs.mkdirSync(CERT_DIR, { recursive: true });
      const kp = path.join(CERT_DIR, 'key.pem');
      const cp = path.join(CERT_DIR, 'cert.pem');
      let key, cert;
      if (fs.existsSync(kp) && fs.existsSync(cp)) {
        key = fs.readFileSync(kp, 'utf8'); cert = fs.readFileSync(cp, 'utf8');
      } else {
        console.log('Generating self-signed certificate…');
        const p = selfsigned.generate(
          [{ name: 'commonName', value: 'localhost' },
           { name: 'subjectAltName', value: `DNS:localhost,IP:127.0.0.1,IP:${LAN_IP}` }],
          { days: 365, keySize: 2048 });
        key = p.private; cert = p.cert;
        fs.writeFileSync(kp, key); fs.writeFileSync(cp, cert);
      }
      server = https.createServer({ key, cert }, app);
    } catch (e) {
      console.error('HTTPS setup failed, using HTTP:', e.message);
      server = http.createServer(app);
    }
  } else {
    server = http.createServer(app);
  }

  const io = new Server(server, { cors: { origin: '*', methods: ['GET', 'POST'] } });

  io.on('connection', (socket) => {
    const role = socket.handshake.query.role === 'mobile' ? 'mobile' : 'desktop';
    console.log(`[+] ${role.padEnd(7)} ${socket.id}`);
    socket.on('motion-data', (p) => {
      if (!p || typeof p !== 'object') return;
      socket.broadcast.emit('update-motion', p);
    });
    socket.on('disconnect', () => console.log(`[-] ${role.padEnd(7)} ${socket.id}`));
  });

  server.listen(PORT, HOST, () => {
    const proto = USE_HTTPS ? 'https' : 'http';
    const line = '═'.repeat(68);
    console.log(`\n${line}`);
    console.log('   MOTIONSYNC 3D  —  real-time motion telemetry relay');
    console.log(line);
    console.log(`   Protocol        : ${proto.toUpperCase()}`);
    console.log(`   Desktop         : ${proto}://localhost:${PORT}`);
    console.log(`   Mobile (LAN)    : ${proto}://${LAN_IP}:${PORT}`);
    console.log(line);
    if (!USE_HTTPS) {
      console.log('   ⚠  Real phone sensors need HTTPS.');
      console.log('      Android: node server.js --https');
      console.log('      iOS    : npx cloudflared tunnel --url http://localhost:3000');
    } else {
      console.log('   ⚠  On the phone: tap "Advanced → Proceed" once.');
    }
    console.log(line + '\n');
  });
}
start();
// Off-Grid SOS Mesh — Dual HTTP + HTTPS Server
// Serves full app directly on both HTTP:8080 and HTTPS:8443
// Allows all mobile phones, tablets, and laptops on the same Wi-Fi to access with zero errors!

const https = require('https');
const http  = require('http');
const fs    = require('fs');
const path  = require('path');

const HTTP_PORT  = 8080;
const HTTPS_PORT = 8443;
const PUBLIC_DIR = __dirname;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js'  : 'text/javascript; charset=utf-8',
  '.css' : 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.png' : 'image/png',
  '.jpg' : 'image/jpeg',
  '.svg' : 'image/svg+xml',
  '.ico' : 'image/x-icon'
};

function serveRequest(req, res) {
  let urlPath = req.url.split('?')[0];
  if (urlPath === '/' || urlPath === '') urlPath = '/index.html';

  const ext = path.extname(urlPath).toLowerCase();
  if (ext === '.key' || ext === '.crt' || urlPath.includes('..')) {
    res.writeHead(403);
    res.end('Access Denied');
    return;
  }

  let filePath = path.join(PUBLIC_DIR, urlPath);
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    res.end('Access Denied');
    return;
  }

  fs.stat(filePath, (statErr, stats) => {
    if (statErr || !stats.isFile()) {
      filePath = path.join(PUBLIC_DIR, 'index.html');
    }
    const fileExt = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[fileExt] || 'application/octet-stream';

    fs.readFile(filePath, (readErr, data) => {
      if (readErr) {
        res.writeHead(500);
        res.end('Server Error: ' + readErr.message);
        return;
      }
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': 'no-cache',
        'Access-Control-Allow-Origin': '*',
        'Permissions-Policy': 'geolocation=*',
        'Feature-Policy': 'geolocation *'
      });
      res.end(data);
    });
  });
}

// 1. HTTP Server on 8080 (Primary, zero warnings, instant connection on LAN)
const httpServer = http.createServer(serveRequest);
httpServer.listen(HTTP_PORT, '0.0.0.0', () => {
  console.log(`[HTTP Server] Running on http://0.0.0.0:${HTTP_PORT}/`);
  console.log(`[LAN URL] http://192.168.1.69:${HTTP_PORT}/`);
  console.log(`[Local URL] http://localhost:${HTTP_PORT}/`);
});

// 2. HTTPS Server on 8443 (Optional TLS for secure context requirements)
const keyPath  = path.join(__dirname, 'server.key');
const certPath = path.join(__dirname, 'server.crt');

try {
  if (fs.existsSync(keyPath) && fs.existsSync(certPath)) {
    const keyData  = fs.readFileSync(keyPath, 'utf8');
    const certData = fs.readFileSync(certPath, 'utf8');
    if (keyData.includes('BEGIN') && certData.includes('BEGIN')) {
      const httpsServer = https.createServer({ key: keyData, cert: certData }, serveRequest);
      httpsServer.listen(HTTPS_PORT, '0.0.0.0', () => {
        console.log(`[HTTPS Server] Running on https://0.0.0.0:${HTTPS_PORT}/`);
        console.log(`[LAN HTTPS URL] https://192.168.1.69:${HTTPS_PORT}/`);
      });
    }
  }
} catch (e) {
  console.log('[HTTPS Note] HTTPS skipped:', e.message);
}


const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
//curl -i http://localhost:3000/
// curl http://localhost:3000/about
// curl http://localhost:3000/api
//create certificate: openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem -days 365 -subj "/CN=localhost"
const PORT = 3000;
const HTTPS_PORT = 3443;

const handler = (req, res) => {
    console.log(`Received request: ${req.url}`);

    res.setHeader('Content-Type', 'text/plain; charset=utf-8');

    if (req.url === '/') {
        res.statusCode = 200;
        res.end('Hello this is the home page');
    } else if (req.url === '/about') {
        res.statusCode = 200;
        res.end('This is the about page');
    } else if (req.url === '/api') {
        res.setHeader('Content-Type', 'application/json');
        res.statusCode = 200;
        res.end(JSON.stringify({ message: 'API endpoint works!', time: new Date() }));
    } else {
        res.statusCode = 404;
        res.end('Page not found');
    }
};

http.createServer(handler).listen(PORT, () => {
    console.log(`Server listens on http://localhost:${PORT}`);
});

// Self-signed cert: openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem -days 365 -subj "/CN=localhost"
const options = {
    key: fs.readFileSync(path.join(__dirname, 'key.pem')),
    cert: fs.readFileSync(path.join(__dirname, 'cert.pem')),
};
https.createServer(options, handler).listen(HTTPS_PORT, () => {
    console.log(`Server listens on https://localhost:${HTTPS_PORT}`);
});

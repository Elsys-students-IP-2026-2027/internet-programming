
const http = require('http');

const PORT = 3000;

const server = http.createServer((req, res) => {
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
});

// Стартираме сървъра
server.listen(PORT, () => {
    console.log(`Сървърът слуша на http://localhost:${PORT}`);
});

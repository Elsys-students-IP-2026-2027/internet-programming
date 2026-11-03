const http = require('http');

/*
Curl examples for testing the REST API:
(you can also use a browser or Postman)

# GET /hello
curl http://localhost:8000/hello

# POST /users
curl -X POST http://localhost:8000/users -H "Content-Type: application/json" -d '{"name": "Charlie"}'

# PUT /users/1
curl -X PUT http://localhost:8000/users/1 -H "Content-Type: application/json" -d '{"name": "Alice Updated"}'

# PATCH /users/2
curl -X PATCH http://localhost:8000/users/2 -H "Content-Type: application/json" -d '{"status": "active"}'

# DELETE /users/1
curl -X DELETE http://localhost:8000/users/1
*/

let users = {
    1: { id: 1, name: "Alice", status: "inactive" },
    2: { id: 2, name: "Bob", status: "inactive" }
};

const server = http.createServer((req, res) => {
    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    const method = req.method;
    const path = parsedUrl.pathname;
    let body = '';

    console.log(`${method} ${path}`);

    req.on('data', chunk => {
        body += chunk.toString();
    });

    req.on('end', () => {
        // Parse JSON body safely
        try {
            if (body && req.headers['content-type'] === 'application/json') {
                body = JSON.parse(body);
            }
        } catch (err) {
            res.writeHead(400, { 'Content-Type': 'text/plain' });
            return res.end('400 Bad Request – Invalid JSON');
        }

        if (method === 'GET' && path === '/hello') {
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            return res.end('Hello, World!');
        }

        if (method === 'POST' && path === '/users') {
            if (!body || !body.name) {
                res.writeHead(400, { 'Content-Type': 'text/plain' });
                return res.end('400 Bad Request – Missing name field');
            }
            const id = Date.now();
            users[id] = { id, ...body };
            res.writeHead(201, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify(users[id]));
        }

        if (method === 'PUT' && path.startsWith('/users/')) {
            const id = path.split('/')[2];
            if (!users[id]) {
                res.writeHead(404, { 'Content-Type': 'text/plain' });
                return res.end('404 Not Found – User not found');
            }
            users[id] = { ...users[id], ...body };
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify(users[id]));
        }

        if (method === 'PATCH' && path.startsWith('/users/')) {
            const id = path.split('/')[2];
            if (!users[id]) {
                res.writeHead(404, { 'Content-Type': 'text/plain' });
                return res.end('404 Not Found – User not found');
            }
            users[id] = { ...users[id], ...body };
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify(users[id]));
        }

        if (method === 'DELETE' && path.startsWith('/users/')) {
            const id = path.split('/')[2];
            if (!users[id]) {
                res.writeHead(404, { 'Content-Type': 'text/plain' });
                return res.end('404 Not Found – User not found');
            }
            delete users[id];
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            return res.end(`User ${id} deleted successfully`);
        }

        const supportedMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
        if (!supportedMethods.includes(method)) {
            res.writeHead(405, { 'Content-Type': 'text/plain' });
            return res.end('405 Method Not Allowed');
        }

        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
    });
});

const PORT = 8000;
server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}`);
});

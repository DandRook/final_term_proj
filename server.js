const http = require('http');

// Render requires dynamic port binding
const PORT = process.env.PORT || 3000;

const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('Hello from Render-deployed server!');
});

server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}/`);
});

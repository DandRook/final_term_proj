// server.js
const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;
let quizQuestions = [];
let currentQuestion = null;
let currentQuestionStartTime = null;
const rotationInterval = 60000; // 60s
let playerQueue = [];

// Load CSV on startup
fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', (row) => quizQuestions.push(row))
  .on('end', () => {
    console.log('CSV loaded!');
    rotateQuestion(); // start rotation
    setInterval(rotateQuestion, rotationInterval);
  });

// Pick a new random question and start timer
function rotateQuestion() {
  currentQuestion = quizQuestions[Math.floor(Math.random() * quizQuestions.length)];
  currentQuestionStartTime = Date.now();
  console.log(`New question: ${currentQuestion.question}`);
}

// Pair players if possible
function matchPlayer(userId) {
  if (playerQueue.length > 0) {
    const opponentId = playerQueue.shift();
    return { status: 'matched', opponentId };
  } else {
    playerQueue.push(userId);
    return { status: 'waiting' };
  }
}

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);

  if (parsedUrl.pathname === '/next-question' && req.method === 'GET') {
    const userId = parsedUrl.query.userId;
    if (!userId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Missing userId' }));
    }

    const matchStatus = matchPlayer(userId);

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      ...matchStatus,
      question: currentQuestion ? currentQuestion.question : null,
      questionId: currentQuestion ? currentQuestion.id : null,
      startTime: currentQuestionStartTime,
      deadline: currentQuestionStartTime + rotationInterval
    }));
  } else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('PvP Quiz Server is running!');
  }
});

server.listen(PORT, () => {
  console.log(`Server live at http://localhost:${PORT}`);
});

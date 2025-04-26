// server.js
const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;
let quizQuestions = [];
let currentQuestion = null;
let currentQuestionStartTime = null;
const rotationInterval = 60000; // 60 seconds
let playerQueue = [];

// Load CSV questions at startup
fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', (row) => quizQuestions.push(row))
  .on('end', () => {
    console.log(`Loaded ${quizQuestions.length} questions`);
    rotateQuestion();
    setInterval(rotateQuestion, rotationInterval);
  });

// Pick random question
function rotateQuestion() {
  const index = Math.floor(Math.random() * quizQuestions.length);
  currentQuestion = quizQuestions[index];
  currentQuestionStartTime = Date.now();
  console.log(`Rotated question: ${currentQuestion.question}`);
}

// Matchmaking placeholder
function matchPlayer(userId) {
  if (playerQueue.length > 0) {
    const opponentId = playerQueue.shift();
    return { status: 'matched', opponentId };
  } else {
    playerQueue.push(userId);
    return { status: 'waiting' };
  }
}

// Create HTTP server
const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);

  if (parsedUrl.pathname === '/next-question' && req.method === 'GET') {
    const userId = parsedUrl.query.userId;
    if (!userId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Missing userId' }));
    }

    // Always rotate a new question when client requests next
    rotateQuestion();

    const matchStatus = matchPlayer(userId);

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      ...matchStatus,
      question: currentQuestion.question,
      questionId: currentQuestion.id,
      options: [
        currentQuestion.option1,
        currentQuestion.option2,
        currentQuestion.option3,
        currentQuestion.option4
      ].filter(Boolean),
      startTime: currentQuestionStartTime,
      deadline: currentQuestionStartTime + rotationInterval
    }));
  }

  else if (parsedUrl.pathname === '/submit-answer' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const { userId, questionId, userAnswer } = JSON.parse(body);
        const question = quizQuestions.find(q => q.id === questionId);
        if (!question) throw new Error("Invalid question");

        const correct = question.correctAnswer.trim().toLowerCase() === userAnswer.trim().toLowerCase();

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ correct }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
    });
  }

  else if (parsedUrl.pathname === '/ping') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'online' }));
  }

  else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('PvP Quiz Server active.');
  }
});

// Start server
server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

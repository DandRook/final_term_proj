//server.js
const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;
let quizQuestions = [];
let currentQuestion = null;
let currentQuestionStartTime = null;
const rotationInterval = 60000;
let playerQueue = [];
let recentQuestions = [];
const recentLimit = 5;

// Load CSV
fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', (row) => quizQuestions.push(row))
  .on('end', () => {
    console.log(`Loaded ${quizQuestions.length} questions`);
    if (quizQuestions.length > 0) {
      rotateQuestion();
      setInterval(rotateQuestion, rotationInterval);
    }
  });

function rotateQuestion() {
  const pool = quizQuestions.filter(q => !recentQuestions.includes(q.id));
  const candidates = pool.length > 0 ? pool : quizQuestions;

  currentQuestion = candidates[Math.floor(Math.random() * candidates.length)];
  currentQuestionStartTime = Date.now();

  recentQuestions.push(currentQuestion.id);
  if (recentQuestions.length > recentLimit) {
    recentQuestions.shift();
  }

  console.log(`New question: ${currentQuestion.question}`);
}

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

    if (!currentQuestion) {
      res.writeHead(503, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Questions not ready yet' }));
    }

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
        if (!userId || !questionId || userAnswer == null) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ error: 'Missing fields' }));
        }

        const question = quizQuestions.find(q => q.id === questionId);
        if (!question) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ error: 'Question not found' }));
        }

        const correct = question.correctAnswer.trim().toLowerCase() === userAnswer.trim().toLowerCase();

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ correct }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Server error' }));
      }
    });
  }

  else if (parsedUrl.pathname === '/ping') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'alive' }));
  }

  else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('PvP Quiz Server is running!');
  }
});

server.listen(PORT, () => {
  console.log(`Server live at http://localhost:${PORT}`);
});

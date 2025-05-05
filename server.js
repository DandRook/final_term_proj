// server.js
const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;
let quizQuestions = [];
let playerQueue = [];
let userCurrentQuestion = {}; // Tracks per-user question

// Load CSV
fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', (row) => quizQuestions.push(row))
  .on('end', () => {
    console.log(`Loaded ${quizQuestions.length} questions`);
  });

function matchPlayer(userId) {
  if (playerQueue.length > 0 && playerQueue[0] !== userId) {
    const opponentId = playerQueue.shift();
    return { status: 'matched', opponentId };
  } else {
    if (!playerQueue.includes(userId)) playerQueue.push(userId);
    return { status: 'waiting' };
  }
}

function getRandomQuestion() {
  const index = Math.floor(Math.random() * quizQuestions.length);
  return quizQuestions[index];
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
    if (matchStatus.status === 'waiting') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ status: 'waiting' }));
    }

    if (!userCurrentQuestion[userId]) {
      userCurrentQuestion[userId] = getRandomQuestion();
    }

    const currentQuestion = userCurrentQuestion[userId];
    console.log(`Sending question: ${currentQuestion.question} to user: ${userId}`);

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'matched',
      opponentId: matchStatus.opponentId,
      question: currentQuestion.question,
      questionId: currentQuestion.id,
      correctAnswer: currentQuestion.correctAnswer,
      options: [
        currentQuestion.option1,
        currentQuestion.option2,
        currentQuestion.option3,
        currentQuestion.option4
      ].filter(Boolean)
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
        delete userCurrentQuestion[userId];

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ correct }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
    });
  }

  else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('PvP Quiz Server active.');
  }
});

server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

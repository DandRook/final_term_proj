const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;
const rotationInterval = 60000;

let quizQuestions = [];
let currentQuestion = null;
let currentQuestionStartTime = null;

let playerQueue = [];

fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', row => quizQuestions.push(row))
  .on('end', () => {
    console.log(`Loaded ${quizQuestions.length} questions`);
    rotateQuestion();
    setInterval(rotateQuestion, rotationInterval);
  });

function rotateQuestion() {
  const index = Math.floor(Math.random() * quizQuestions.length);
  currentQuestion = quizQuestions[index];
  currentQuestionStartTime = Date.now();
  console.log(`Rotated question: ${currentQuestion.question}`);
}

function matchPlayer(userId) {
  if (playerQueue.includes(userId)) return { status: 'waiting' };

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
  const { pathname, query } = parsedUrl;

  if (pathname === '/next-question' && req.method === 'GET') {
    const userId = query.userId;
    const mode = query.mode || 'single';

    if (!userId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Missing userId' }));
    }

    if (mode === '1v1') {
      const matchStatus = matchPlayer(userId);

      if (matchStatus.status === 'waiting') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'waiting' }));
      }

      console.log(`Sending question: ${currentQuestion.question} to user: ${userId} (1v1)`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        status: 'matched',
        opponentId: matchStatus.opponentId,
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

    // SINGLE player mode
    const singleQ = quizQuestions[Math.floor(Math.random() * quizQuestions.length)];
    console.log(`Sending single question: ${singleQ.question} to user: ${userId}`);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      status: 'matched',
      question: singleQ.question,
      questionId: singleQ.id,
      options: [
        singleQ.option1,
        singleQ.option2,
        singleQ.option3,
        singleQ.option4
      ].filter(Boolean),
      startTime: Date.now(),
      deadline: Date.now() + rotationInterval
    }));
  }

  else if (pathname === '/submit-answer' && req.method === 'POST') {
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

  else if (pathname === '/ping') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'online' }));
  }

  else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('PvP Quiz Server active.');
  }
});

server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

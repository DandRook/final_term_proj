// server.js
const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;
const quizQuestions = [];
let playerQueue = [];
const matchMap = new Map(); // userId -> opponentId
const matchQuestions = new Map(); // matchKey -> question
const matchSubmissions = new Map(); // matchKey -> Set(userIds)

fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', (row) => quizQuestions.push(row))
  .on('end', () => {
    console.log(`Loaded ${quizQuestions.length} questions`);
  });

function getMatchKey(user1, user2) {
  return [user1, user2].sort().join('_');
}

function matchPlayer(userId) {
  if (matchMap.has(userId)) {
    const opponentId = matchMap.get(userId);
    return { status: 'matched', opponentId };
  }

  if (playerQueue.length > 0 && playerQueue[0] !== userId) {
    const opponentId = playerQueue.shift();
    matchMap.set(userId, opponentId);
    matchMap.set(opponentId, userId);
    return { status: 'matched', opponentId };
  }

  if (!playerQueue.includes(userId)) {
    playerQueue.push(userId);
  }

  return { status: 'waiting' };
}

function getRandomQuestion() {
  const index = Math.floor(Math.random() * quizQuestions.length);
  return quizQuestions[index];
}

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const userId = parsedUrl.query.userId;
  const mode = parsedUrl.query.mode || 'single';

  if (parsedUrl.pathname === '/next-question' && req.method === 'GET') {
    if (!userId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Missing userId' }));
    }

    if (mode === 'single') {
      const q = getRandomQuestion();
      console.log(`Sending single question: ${q.question} to user: ${userId}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        status: 'matched',
        question: q.question,
        questionId: q.id,
        options: [
          q.option1,
          q.option2,
          q.option3,
          q.option4
        ].filter(Boolean)
      }));
    }

    // 1v1 mode
    const matchStatus = matchPlayer(userId);

    if (matchStatus.status === 'waiting') {
      return res.end(JSON.stringify({ status: 'waiting' }));
    }

    const opponentId = matchStatus.opponentId;
    const matchKey = getMatchKey(userId, opponentId);

    if (!matchQuestions.has(matchKey)) {
      const q = getRandomQuestion();
      matchQuestions.set(matchKey, q);
      matchSubmissions.set(matchKey, new Set());
      console.log(`Assigned new question for match ${matchKey}: ${q.question}`);
    }

    const q = matchQuestions.get(matchKey);
    console.log(`Sending question to ${userId}: ${q.question}`);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      status: 'matched',
      opponentId,
      question: q.question,
      questionId: q.id,
      options: [q.option1, q.option2, q.option3, q.option4].filter(Boolean)
    }));
  }

  else if (parsedUrl.pathname === '/submit-answer' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const { userId, questionId, userAnswer, mode = 'single' } = JSON.parse(body);
        const question = quizQuestions.find(q => q.id === questionId);
        if (!question) throw new Error("Invalid question");

        const correct = question.correctAnswer.trim().toLowerCase() === userAnswer.trim().toLowerCase();

        if (mode === '1v1') {
          const opponentId = matchMap.get(userId);
          const matchKey = getMatchKey(userId, opponentId);
          const submitted = matchSubmissions.get(matchKey) || new Set();
          submitted.add(userId);

          if (submitted.size === 2) {
            matchSubmissions.set(matchKey, new Set()); // Reset
            matchQuestions.delete(matchKey); // Force next rotation
          } else {
            matchSubmissions.set(matchKey, submitted);
          }
        }

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

server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

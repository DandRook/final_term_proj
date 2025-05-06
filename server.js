const http = require('http');
const fs = require('fs');
const url = require('url');
const csv = require('csv-parser');

const PORT = process.env.PORT || 3000;

const quizQuestions = [];             // Loaded quiz questions from CSV
const playerQueue = [];               // Queue for waiting players in 1v1 mode
const matchMap = new Map();           // Maps userId => opponentId
const playerQuestions = new Map();    // Maps userId => assigned question (only for 1v1 mode)

// Load questions from CSV file into memory
fs.createReadStream('questions.csv')
  .pipe(csv())
  .on('data', (row) => quizQuestions.push(row))
  .on('end', () => {
    console.log(`Loaded ${quizQuestions.length} questions`);
  });

// Select a random question from the quiz
function getRandomQuestion() {
  const index = Math.floor(Math.random() * quizQuestions.length);
  return quizQuestions[index];
}

// Handles matchmaking logic for 1v1 mode
function matchPlayer(userId) {
  if (matchMap.has(userId)) {
    return { status: 'matched', opponentId: matchMap.get(userId) };
  }

  if (playerQueue.length > 0 && playerQueue[0] !== userId) {
    const opponentId = playerQueue.shift();
    matchMap.set(userId, opponentId);
    matchMap.set(opponentId, userId);
    return { status: 'matched', opponentId };
  }

  if (!playerQueue.includes(userId)) {
    playerQueue.push(userId); // Wait in queue
  }

  return { status: 'waiting' };
}

// Core HTTP server handling quiz logic
const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const userId = parsedUrl.query.userId;
  const mode = parsedUrl.query.mode || 'single';

  // === Request for a new question ===
  if (parsedUrl.pathname === '/next-question' && req.method === 'GET') {
    if (!userId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Missing userId' }));
    }

    // === Single-player mode ===
    if (mode === 'single') {
      const q = getRandomQuestion();
      console.log(`Sending single question: ${q.question} to user: ${userId}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        status: 'matched',
        question: q.question,
        questionId: q.id,
        options: [q.option1, q.option2, q.option3, q.option4].filter(Boolean)
      }));
    }

    // === 1v1 Mode ===
    if (mode === '1v1') {
      const matchStatus = matchPlayer(userId);

      if (matchStatus.status === 'waiting') {
        return res.end(JSON.stringify({ status: 'waiting' }));
      }

      // Assign a new question only if not already assigned
      if (!playerQuestions.has(userId)) {
        const q = getRandomQuestion();
        playerQuestions.set(userId, q);
        console.log(`Assigned question to ${userId}: ${q.question}`);
      }

      const q = playerQuestions.get(userId);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        status: 'matched',
        opponentId: matchStatus.opponentId,
        question: q.question,
        questionId: q.id,
        options: [q.option1, q.option2, q.option3, q.option4].filter(Boolean)
      }));
    }
  }

  // === Answer submission handler ===
  else if (parsedUrl.pathname === '/submit-answer' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const { userId, questionId, userAnswer, mode = 'single' } = JSON.parse(body);
        const question = quizQuestions.find(q => q.id === questionId);
        if (!question) throw new Error("Invalid question");

        // Check if answer is correct (case-insensitive)
        const correct = question.correctAnswer.trim().toLowerCase() === userAnswer.trim().toLowerCase();

        // Clean up the question state in 1v1 mode
        if (mode === '1v1') {
          const q = playerQuestions.get(userId);
          if (q && q.id === questionId) {
            playerQuestions.delete(userId);
            console.log(`Player ${userId} answered question ${questionId}`);
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

  // === Default route ===
  else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('PvP Quiz Server active.');
  }
});

// Start the server
server.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

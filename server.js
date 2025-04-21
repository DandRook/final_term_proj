// server.js
const http = require('http');
const url = require('url');

// Render requires dynamic port binding
const PORT = process.env.PORT || 3000;

const quizQuestions = [
  { id: 'q1', question: 'What is the capital of France?', correctAnswer: 'Paris' },
  { id: 'q2', question: 'What is 2 + 2?', correctAnswer: '4' },
  { id: 'q3', question: 'Who wrote "1984"?', correctAnswer: 'George Orwell' },
  { id: 'q4', question: 'What is the smallest planet in our solar system?', correctAnswer: 'Mercury' },
  { id: 'q5', question: 'What is the largest ocean on Earth?', correctAnswer: 'Pacific' }
];

function getRandomQuestion() {
  return quizQuestions[Math.floor(Math.random() * quizQuestions.length)];
}

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  
  if (parsedUrl.pathname === '/next-question') {
    const question = getRandomQuestion();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      id: question.id,
      question: question.question
      // optionally include correctAnswer here
    }));
  } else {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('Hello from Render-deployed server!');
  }
});

server.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}/`);
});

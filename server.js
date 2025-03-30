const express = require('express');
const path = require('path');
const app = express();
const port = 5000;

// Serve static files from the web directory
app.use(express.static(path.join(__dirname, 'src/main/resources/assets/aicompanion/web')));

// Mock login endpoint (for demo purposes only)
app.post('/api/auth/login', express.json(), (req, res) => {
  const { username, password } = req.body;
  
  // Check credentials (from AICompanionConfig.java)
  if (username === 'admin' && password === 'password') {
    res.json({
      token: 'mock-jwt-token',
      username: username
    });
  } else {
    res.status(401).json({ error: 'Invalid username or password' });
  }
});

// Catch-all route to serve the index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'src/main/resources/assets/aicompanion/web/index.html'));
});

app.listen(port, () => {
  console.log(`Web server running at http://localhost:${port}`);
});
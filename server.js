const express = require('express');
const path = require('path');
const app = express();
const port = 5000;

// Store users in memory (for demo purposes only)
// In a real application, these would be stored in a database
const users = {
  'admin': 'password'
};

// Serve static files from the web directory
app.use(express.static(path.join(__dirname, 'src/main/resources/assets/aicompanion/web')));

// Mock login endpoint (for demo purposes only)
app.post('/api/auth/login', express.json(), (req, res) => {
  const { username, password } = req.body;
  
  // Check credentials
  if (users[username] && users[username] === password) {
    res.json({
      token: 'mock-jwt-token-' + username, // Include username in token for demo
      username: username
    });
  } else {
    res.status(401).json({ 
      error: 'Invalid username or password',
      message: 'Invalid username or password'
    });
  }
});

// Mock registration endpoint
app.post('/api/auth/register', express.json(), (req, res) => {
  const { username, password } = req.body;
  
  // Validate inputs
  if (!username || !password) {
    return res.status(400).json({ 
      error: 'Missing required fields',
      message: 'Username and password are required'
    });
  }
  
  // Check if username already exists
  if (users[username]) {
    return res.status(409).json({ 
      error: 'Username already exists',
      message: 'This username is already taken'
    });
  }
  
  // Register new user
  users[username] = password;
  
  console.log(`New user registered: ${username}`);
  
  // Return success
  res.status(201).json({
    message: 'Account created successfully',
    username: username
  });
});

// Mock token validation endpoint
app.get('/api/auth/validate', (req, res) => {
  const authHeader = req.headers.authorization;
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Invalid token' });
  }
  
  // This is just a demo, so we're not actually validating the token
  res.json({ valid: true });
});

// Mock companions endpoint
app.get('/api/companions', (req, res) => {
  // Return empty array since this is just a demo
  res.json([]);
});

// Catch-all route to serve the index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'src/main/resources/assets/aicompanion/web/index.html'));
});

app.listen(port, () => {
  console.log(`Web server running at http://localhost:${port}`);
  console.log(`Default admin credentials: username=admin, password=password`);
});
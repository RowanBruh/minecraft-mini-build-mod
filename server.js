const express = require('express');
const path = require('path');
const http = require('http');
const { WebSocketServer, WebSocket } = require('ws');
const app = express();
const server = http.createServer(app);
const port = 5000;

// Create WebSocket server
const wss = new WebSocketServer({ server, path: '/ws' });

// Connected clients
const clients = new Set();

// Handle WebSocket connections
wss.on('connection', (ws) => {
  console.log('New WebSocket client connected');
  clients.add(ws);
  
  // Send welcome message
  ws.send(JSON.stringify({
    type: 'connection',
    status: 'connected',
    message: 'Connected to AI Companion WebSocket server'
  }));
  
  // Handle messages from clients
  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);
      console.log('Received message:', data);
      
      // Handle different message types
      switch (data.type) {
        case 'skin':
          handleSkinCommand(ws, data);
          break;
        default:
          // Mock command response
          handleCommand(ws, data);
      }
    } catch (error) {
      console.error('Error processing message:', error);
      ws.send(JSON.stringify({
        type: 'error',
        message: 'Invalid message format'
      }));
    }
  });
  
  // Handle disconnection
  ws.on('close', () => {
    console.log('WebSocket client disconnected');
    clients.delete(ws);
  });
  
  // Handle errors
  ws.on('error', (error) => {
    console.error('WebSocket error:', error);
    clients.delete(ws);
  });
});

// Handle skin command (mock implementation)
function handleSkinCommand(ws, data) {
  console.log('Handling skin command:', data);
  
  // Simulate server processing
  setTimeout(() => {
    // Send success response
    ws.send(JSON.stringify({
      type: 'command_result',
      commandId: data.commandId,
      success: true,
      command: 'skin',
      message: `Skin updated to ${data.skinType || 'custom'}`,
      data: {
        skinType: data.skinType || 'custom',
        skinPath: data.skinPath
      }
    }));
  }, 500);
}

// Handle general command (mock implementation)
function handleCommand(ws, data) {
  console.log('Handling command:', data);
  
  // Simulate server processing
  setTimeout(() => {
    // Send success response
    ws.send(JSON.stringify({
      type: 'command_result',
      commandId: data.commandId,
      success: true,
      command: data.command,
      message: `Command ${data.command} executed successfully`,
      data: {}
    }));
  }, 500);
}

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

// Mock companions endpoint with sample data
app.get('/api/companions', (req, res) => {
  // Return sample companions for demonstration
  res.json([
    {
      id: '12345678-1234-1234-1234-123456789abc',
      name: 'Guardian',
      status: 'active',
      skinType: 'default',
      lastPosition: { x: 100, y: 64, z: 200 },
      inventory: [],
      settings: {
        followDistance: 5,
        behavior: 'follow',
        attackHostiles: true,
        autoCollect: false
      }
    },
    {
      id: '87654321-4321-4321-4321-cba987654321',
      name: 'Wizard',
      status: 'active',
      skinType: 'wizard',
      lastPosition: { x: -150, y: 70, z: 50 },
      inventory: [],
      settings: {
        followDistance: 3,
        behavior: 'guard',
        attackHostiles: true,
        autoCollect: true
      }
    }
  ]);
});

// Mock companion details endpoint
app.get('/api/companions/:id', (req, res) => {
  const companionId = req.params.id;
  
  // Return mock details based on ID
  if (companionId === '12345678-1234-1234-1234-123456789abc') {
    res.json({
      id: '12345678-1234-1234-1234-123456789abc',
      name: 'Guardian',
      status: 'active',
      skinType: 'default',
      lastPosition: { x: 100, y: 64, z: 200 },
      inventory: [],
      settings: {
        followDistance: 5,
        behavior: 'follow',
        attackHostiles: true,
        autoCollect: false
      }
    });
  } else if (companionId === '87654321-4321-4321-4321-cba987654321') {
    res.json({
      id: '87654321-4321-4321-4321-cba987654321',
      name: 'Wizard',
      status: 'active',
      skinType: 'wizard',
      lastPosition: { x: -150, y: 70, z: 50 },
      inventory: [],
      settings: {
        followDistance: 3,
        behavior: 'guard',
        attackHostiles: true,
        autoCollect: true
      }
    });
  } else {
    res.status(404).json({ error: 'Companion not found' });
  }
});

// Mock companion inventory endpoint
app.get('/api/companions/:id/inventory', (req, res) => {
  // Return mock inventory data
  res.json([
    { id: 'item_1', name: 'Stone Sword', count: 1, slot: 0 },
    { id: 'item_2', name: 'Bread', count: 8, slot: 1 },
    { id: 'item_3', name: 'Iron Pickaxe', count: 1, slot: 2 }
  ]);
});

// Mock skin update endpoint
app.post('/api/companions/:id/skin', express.json(), (req, res) => {
  const companionId = req.params.id;
  const { skinType } = req.body;
  
  console.log(`Updating skin for companion ${companionId} to ${skinType}`);
  
  // Simulate success response
  res.json({
    success: true,
    message: `Skin updated to ${skinType}`,
    skinType: skinType
  });
});

// Catch-all route to serve the index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'src/main/resources/assets/aicompanion/web/index.html'));
});

// Use the HTTP server instead of the Express app directly
server.listen(port, '0.0.0.0', () => {
  console.log(`Web server running at http://localhost:${port}`);
  console.log(`WebSocket server running at ws://localhost:${port}/ws`);
  console.log(`Default admin credentials: username=admin, password=password`);
});

// Handle server errors
server.on('error', (error) => {
  console.error('Server error:', error);
});

// Handle process termination
process.on('SIGINT', () => {
  console.log('Server shutting down...');
  
  // Close all WebSocket connections
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.close(1000, 'Server shutting down');
    }
  });
  
  // Close the server
  server.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

// Keep the process alive
process.on('uncaughtException', (err) => {
  console.error('Uncaught exception:', err);
});

// Log startup complete
console.log('Server initialization complete');
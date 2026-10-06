const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Home page
app.get('/', (req, res) => {
  res.json({
    message: 'Welcome to Jenkins Demo App!',
    version: '1.0.0',
    endpoints: ['/', '/health', '/api/info']
  });
});

// API info endpoint
app.get('/api/info', (req, res) => {
  res.json({
    app: 'jenkins-demo-app',
    environment: process.env.NODE_ENV || 'development',
    uptime: process.uptime(),
    nodeVersion: process.version
  });
});

// Simple echo endpoint for testing
app.post('/api/echo', (req, res) => {
  res.json({ echo: req.body });
});

// Start server only when run directly (not imported for testing)
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
  });
}

module.exports = app;

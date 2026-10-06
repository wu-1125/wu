const app = require('../src/app');

// Simple mock for Express request/response
describe('Jenkins Demo App', () => {
  describe('GET /', () => {
    it('should return welcome message', (done) => {
      const req = {};
      const res = {
        json: (data) => {
          expect(data.message).toBe('Welcome to Jenkins Demo App!');
          expect(data.version).toBe('1.0.0');
          done();
        }
      };

      // Get the route handler from the express app
      const router = app._router;
      const layer = router.stack.find(l => l.route && l.route.path === '/');
      if (layer) {
        layer.route.stack[0].handle(req, res);
      } else {
        done.fail('Route / not found');
      }
    });
  });

  describe('GET /health', () => {
    it('should return ok status', (done) => {
      const req = {};
      const res = {
        json: (data) => {
          expect(data.status).toBe('ok');
          expect(data.timestamp).toBeDefined();
          done();
        }
      };

      const router = app._router;
      const layer = router.stack.find(l => l.route && l.route.path === '/health');
      if (layer) {
        layer.route.stack[0].handle(req, res);
      } else {
        done.fail('Route /health not found');
      }
    });
  });

  describe('GET /api/info', () => {
    it('should return app info', (done) => {
      const req = {};
      const res = {
        json: (data) => {
          expect(data.app).toBe('jenkins-demo-app');
          expect(data.nodeVersion).toBeDefined();
          expect(typeof data.uptime).toBe('number');
          done();
        }
      };

      const router = app._router;
      const layer = router.stack.find(l => l.route && l.route.path === '/api/info');
      if (layer) {
        layer.route.stack[0].handle(req, res);
      } else {
        done.fail('Route /api/info not found');
      }
    });
  });
});

// Auth / session middleware — runs before chat proxy routes.
// Attach validated session headers here before forwarding to Spring Boot.
function authMiddleware(req, res, next) {
  // TODO: validate JWT from Authorization header
  // const token = req.headers['authorization']?.split(' ')[1];
  // const session = verifyToken(token);
  // req.headers['x-session-id'] = session.id;
  next();
}

module.exports = authMiddleware;

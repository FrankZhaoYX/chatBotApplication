# ChatBot Application

A full-stack AI chatbot monorepo with token-streaming responses.

## Architecture

```
User → React UI (5173) → Node.js BFF (3001) → Java Spring Boot (8080) → Ollama (11434)
```

Responses stream back up the same chain via Server-Sent Events (SSE).  
The BFF owns auth and session proxying. Spring AI owns MCP tool orchestration with Ollama.

## Stack

| Layer    | Technology                                      | Port  |
|----------|-------------------------------------------------|-------|
| Frontend | React 18, Redux Toolkit, Webpack 5              | 5173  |
| BFF      | Node.js 20, Express, Axios                      | 3001  |
| Backend  | Java 21, Spring Boot 3.3, Spring AI 1.0.0-M6   | 8080  |
| AI       | Ollama (local or LAN, model: `gemma4`)          | 11434 |

## Monorepo Structure

```
/
├── frontend/                   # React + Redux Toolkit UI
│   ├── public/index.html
│   ├── src/
│   │   ├── index.jsx            # React root
│   │   ├── App.jsx
│   │   ├── store/
│   │   │   ├── index.js         # Redux store
│   │   │   └── chatSlice.js     # SSE streaming + message state
│   │   └── components/
│   │       ├── ChatWindow.jsx
│   │       └── MessageInput.jsx
│   ├── webpack.config.js
│   └── .babelrc
│
├── bff/                        # Node.js BFF — auth, session proxying
│   └── src/
│       ├── index.js             # Express entry point
│       ├── routes/chat.js       # SSE proxy → Spring Boot
│       └── middleware/auth.js   # JWT / session stub
│
├── backend/                    # Java Spring Boot — MCP server + AI orchestration
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/chatbot/
│       │   ├── ChatBotApplication.java
│       │   ├── config/
│       │   │   ├── WebConfig.java       # Reactive CORS (CorsWebFilter)
│       │   │   └── McpToolsConfig.java  # Registers tools with MCP server
│       │   ├── tools/
│       │   │   └── SampleTools.java     # MCP tools: echo, add, currentTime
│       │   ├── controller/ChatController.java
│       │   └── service/ChatService.java
│       └── resources/application.yml
│
├── docker/                     # Docker Compose + Dockerfiles
│   ├── docker-compose.yml
│   ├── frontend.Dockerfile
│   ├── bff.Dockerfile
│   ├── backend.Dockerfile
│   └── nginx.conf
│
└── README.md
```

## MCP Server

The backend exposes an MCP server over HTTP SSE transport (Spring AI WebFlux).

| Endpoint         | Method | Purpose                        |
|------------------|--------|--------------------------------|
| `/sse`           | GET    | SSE stream — subscribe here    |
| `/mcp/message`   | POST   | Send JSON-RPC 2.0 messages     |

### Registered Tools

| Tool          | Description                                      |
|---------------|--------------------------------------------------|
| `echo`        | Echoes text back — round-trip test               |
| `add`         | Returns the sum of two integers                  |
| `currentTime` | Returns ISO-8601 datetime for a given timezone   |

### Verify MCP (two terminals)

**Terminal 1** — subscribe to SSE stream:
```bash
curl -N -H "Accept: text/event-stream" http://localhost:8080/sse
```

**Terminal 2** — list tools (response appears in terminal 1):
```bash
curl -s -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

Call a tool:
```bash
curl -s -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"add","arguments":{"a":21,"b":21}}}'
```

## CORS Policy

| Boundary          | Allowed origin            | Configured in              |
|-------------------|---------------------------|----------------------------|
| BFF ← Frontend    | `http://localhost:5173`   | `bff/src/index.js` (`cors` middleware) |
| Spring ← BFF      | `http://localhost:3001`   | `config/WebConfig.java` (`CorsWebFilter`) |

In Docker, the Spring allowed origin is `http://bff:3001` (internal service name).

## Prerequisites

- Node.js 20+
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- [Ollama](https://ollama.com) with `gemma4` pulled

```bash
ollama pull gemma4
```

## Local Development

Each service runs independently. Start them in order:

**1. Ollama**
```bash
# Local only
ollama serve

# LAN access (allow connections from other machines on the same network)
OLLAMA_HOST=0.0.0.0 ollama serve
```

**2. Spring Boot backend**
```bash
cd backend

# Local Ollama
mvn spring-boot:run

# Ollama on another machine (e.g. desktop at 192.168.50.33)
SPRING_AI_OLLAMA_BASE_URL=http://192.168.50.33:11434 mvn spring-boot:run
```

**3. Node.js BFF**
```bash
cd bff
cp .env.example .env
npm install
npm run dev
```

**4. React frontend**
```bash
cd frontend
npm install
npm start          # webpack-dev-server on :5173
```

Open `http://localhost:5173`.

## Docker (local dev)

```bash
cd docker
docker compose up --build
```

| Service          | Local URL                          |
|------------------|------------------------------------|
| react-frontend   | http://localhost:5173              |
| node-bff         | http://localhost:3001/health       |
| spring-backend   | http://localhost:8080              |
| ollama           | http://localhost:11434             |

`react-frontend` and `node-bff` mount their `src/` directories as volumes, so code changes reload without rebuilding the image.  
`spring-backend` waits for Ollama's healthcheck before starting.

## Environment Variables

### BFF (`bff/.env`)
| Variable       | Default                   | Description                  |
|----------------|---------------------------|------------------------------|
| `PORT`         | `3001`                    | BFF listen port              |
| `CORS_ORIGIN`  | `http://localhost:5173`   | Allowed frontend origin      |
| `BACKEND_URL`  | `http://localhost:8080`   | Spring Boot base URL         |

### Backend (`application.yml` / env)
| Variable                    | Default                    | Description                        |
|-----------------------------|----------------------------|------------------------------------|
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434`   | Ollama base URL (local or LAN IP)  |
| `CORS_ALLOWED_ORIGIN`       | `http://localhost:3001`    | Allowed BFF origin                 |

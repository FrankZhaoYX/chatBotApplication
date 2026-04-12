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
| AI       | Ollama (local MCP server, model: `llama3.2`)    | 11434 |

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
├── backend/                    # Java Spring Boot — MCP tool orchestration
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/chatbot/
│       │   ├── ChatBotApplication.java
│       │   ├── config/WebConfig.java    # Global CORS
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

## CORS Policy

| Boundary          | Allowed origin            | Configured in              |
|-------------------|---------------------------|----------------------------|
| BFF ← Frontend    | `http://localhost:5173`   | `bff/src/index.js` (`cors` middleware) |
| Spring ← BFF      | `http://localhost:3001`   | `config/WebConfig.java` (`WebMvcConfigurer`) |

In Docker, the Spring allowed origin is `http://bff:3001` (internal service name).

## Prerequisites

- Node.js 20+
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- [Ollama](https://ollama.com) with `llama3.2` pulled

```bash
ollama pull llama3.2
```

## Local Development

Each service runs independently. Start them in order:

**1. Ollama**
```bash
ollama serve
```

**2. Spring Boot backend**
```bash
cd backend
mvn spring-boot:run
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

## Docker (all services)

```bash
cd docker
docker compose up --build
```

| Service  | Local URL                    |
|----------|------------------------------|
| Frontend | http://localhost:3000        |
| BFF      | http://localhost:3001/health |
| Backend  | http://localhost:8080        |
| Ollama   | http://localhost:11434       |

## Environment Variables

### BFF (`bff/.env`)
| Variable       | Default                   | Description                  |
|----------------|---------------------------|------------------------------|
| `PORT`         | `3001`                    | BFF listen port              |
| `CORS_ORIGIN`  | `http://localhost:5173`   | Allowed frontend origin      |
| `BACKEND_URL`  | `http://localhost:8080`   | Spring Boot base URL         |

### Backend (`application.yml` / env)
| Variable                  | Default                    | Description              |
|---------------------------|----------------------------|--------------------------|
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama base URL          |
| `CORS_ALLOWED_ORIGIN`     | `http://localhost:3001`    | Allowed BFF origin       |

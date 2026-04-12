# ChatBot Application

A full-stack AI chatbot monorepo.

## Architecture

```
User → React UI → Node.js BFF → Java Spring Boot → Ollama (MCP)
```

Responses stream back up the same chain. The BFF owns auth and session proxying. Spring AI owns the MCP tool orchestration with Ollama.

## Stack

| Layer    | Technology                        |
|----------|-----------------------------------|
| Frontend | React, Redux Toolkit, Webpack     |
| BFF      | Node.js, Express, Axios           |
| Backend  | Java, Spring Boot, Spring AI      |
| AI       | Ollama (local MCP server)         |
| Infra    | Docker                            |

## Monorepo Structure

```
/
├── frontend/   # React + Redux Toolkit UI
├── bff/        # Node.js BFF — auth, session proxying
├── backend/    # Java Spring Boot — MCP tool orchestration
├── docker/     # Docker Compose and service Dockerfiles
└── README.md
```

## Getting Started

> Setup instructions will be added as each service is scaffolded.

### Prerequisites

- Node.js 20+
- Java 21+
- Docker & Docker Compose
- [Ollama](https://ollama.com) running locally

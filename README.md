# Collaborative Interview Platform

A state-of-the-art, real-time collaborative technical interview platform built with **React**, **Spring Boot**, **Redis**, and **WebRTC**. It mimics features from Google Meet/Microsoft Teams, integrated with a shared, sandboxed code execution IDE (similar to CoderPad).

---

## Key Features

1. **Real-time Video & Audio Chat**: P2P communication powered by WebRTC.
2. **Screen Sharing**: Easily share your screen with other participants in the room.
3. **Shared Code Editor**: Real-time synchronized Monaco Editor supporting multiple programming languages (Python, Java, Go, C++, C).
4. **Sandboxed Code Execution**: Run code securely in isolated Docker containers with stdout, stderr, and execution time metrics.
5. **Interactive Chat**: Built-in instant messaging for interviewers and candidates.
6. **Robust Authentication**: JWT-based secure login, registration, and anonymous/guest access via secure invitation links.
7. **Email Invitations**: Automatic scheduling notifications sent to candidates and interviewers with dynamic meeting links.

---

## Tech Stack

* **Frontend**: React (Vite), SockJS, STOMP.js, Monaco Editor, Tailwind CSS / Vanilla CSS.
* **Backend**: Java 17, Spring Boot, Spring Security (JWT), Spring WebSocket.
* **Database**: PostgreSQL (hosted via Supabase), Flyway (Migrations).
* **Caching & Signaling**: Redis (for cross-node WebSocket messaging and coordination).
* **Execution Environment**: Docker (sandboxed execution of user-submitted code).

---

## Prerequisites

Before running the application, ensure you have the following installed:
* **Docker** & **Docker Compose**
* **Java 17 JDK** (if running backend locally)
* **Maven** (if running backend locally)
* **Node.js** (v18+) and **npm** (if running frontend locally)

---

## Getting Started

### 1. Pull Code Execution Images
To support sandboxed code execution, pull the required runtime Docker images by running:
```bash
bash pull-images.sh
```

### 2. Choose How to Run

#### Option A: Docker Compose (Easiest)
Build and run the entire stack (Redis, Backend, Frontend) with a single command:
```bash
docker-compose up -d --build
```
* **Frontend Application**: [http://localhost:5173](http://localhost:5173)
* **Backend API / Swagger**: [http://localhost:8080](http://localhost:8080)
* **To Stop**: `docker-compose down`

---

#### Option B: Local Development Mode (Best for Debugging)

##### Step 1: Start Redis
Start a local Redis container on port `6379`:
```bash
docker run -d --name interview_redis_local -p 6379:6379 redis:7-alpine
```

##### Step 2: Run the Backend
Navigate to the `backend/` directory and run:
```bash
mvn spring-boot:run
```

##### Step 3: Run the Frontend
Navigate to the `frontend/` directory and run:
```bash
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## Credentials for Mock Users

During local testing, you can log in with the following default seeded credentials:

| Role | Email | Password |
| :--- | :--- | :--- |
| **Admin** | `admin@interview.dev` | `Admin@123` |
| **Interviewer** | `interviewer@interview.dev` | `Interviewer@123` |
| **Candidate** | `candidate@interview.dev` | `Candidate@123` |

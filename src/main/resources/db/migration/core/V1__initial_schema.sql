-- V1: Initial schema for Interview & Coding Assessment Platform

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name   VARCHAR(255) NOT NULL,
    avatar_url  VARCHAR(512),
    role        VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE' CHECK (role IN ('ADMIN','INTERVIEWER','CANDIDATE')),
    provider    VARCHAR(20) NOT NULL DEFAULT 'LOCAL' CHECK (provider IN ('LOCAL','GOOGLE')),
    provider_id VARCHAR(255),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Interviews table
CREATE TABLE IF NOT EXISTS interviews (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    scheduled_at       TIMESTAMPTZ NOT NULL,
    duration_minutes   INTEGER NOT NULL DEFAULT 60,
    status             VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                           CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    candidate_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    interviewer_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    room_token         VARCHAR(64) NOT NULL UNIQUE,
    notes              TEXT,
    created_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interviews_candidate ON interviews(candidate_id);
CREATE INDEX idx_interviews_interviewer ON interviews(interviewer_id);
CREATE INDEX idx_interviews_status ON interviews(status);
CREATE INDEX idx_interviews_scheduled_at ON interviews(scheduled_at);
CREATE INDEX idx_interviews_room_token ON interviews(room_token);

-- Submissions table (code runs)
CREATE TABLE IF NOT EXISTS submissions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id      UUID REFERENCES interviews(id) ON DELETE CASCADE,
    submitted_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    language          VARCHAR(20) NOT NULL CHECK (language IN ('JAVA','PYTHON','GO','CPP','C')),
    code              TEXT NOT NULL,
    stdin             TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','RUNNING','COMPLETED','ERROR','TIMEOUT')),
    stdout            TEXT,
    stderr            TEXT,
    execution_time_ms INTEGER,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_submissions_interview ON submissions(interview_id);
CREATE INDEX idx_submissions_status ON submissions(status);

-- Feedback / Notes table
CREATE TABLE IF NOT EXISTS interview_notes (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id         UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    interviewer_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    technical_score      SMALLINT CHECK (technical_score BETWEEN 1 AND 10),
    communication_score  SMALLINT CHECK (communication_score BETWEEN 1 AND 10),
    problem_solving_score SMALLINT CHECK (problem_solving_score BETWEEN 1 AND 10),
    feedback_notes       TEXT,
    recommendation       VARCHAR(20) CHECK (recommendation IN ('STRONG_YES','YES','MAYBE','NO','STRONG_NO')),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notes_interview ON interview_notes(interview_id);

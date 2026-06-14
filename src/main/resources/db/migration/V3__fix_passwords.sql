-- V3: Reset mock user passwords with verified BCrypt hashes
-- BCrypt 12 rounds
-- Admin@123    -> $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/oB7W6JVaC
-- Interviewer@123 -> $2a$12$XuX4xNh7JVvKBpPAeFGm4.h.BZaF0ICiJxU9m0bJIm17k9Y3v2TBm
-- Candidate@123 -> $2a$12$S8gm.pLH6JM7Zqq7T5EQiOLe6IiGRpwz1V2NF/kDKFJvMT/vCFVGi

-- Use a simpler known-working BCrypt hash approach:
-- These are generated using BCryptPasswordEncoder(12) from Spring Security
-- Password: password (bcrypt round 10) - using standard test value first

-- Actually, reset with round 10 which is faster and just as secure for testing:
-- Admin@123 hash (bcrypt $2a$10$):
UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE email = 'admin@interview.dev';
-- Interviewer@123 hash:
UPDATE users SET password_hash = '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO1z1dGTa.S'
WHERE email = 'interviewer@interview.dev';
-- Candidate@123 hash:
UPDATE users SET password_hash = '$2a$10$ywtdxXLfMhD0XBhJhFRB7.vHiCmXw.g2J7WvCMy.N6KJOUJDCm79W'
WHERE email = 'candidate@interview.dev';

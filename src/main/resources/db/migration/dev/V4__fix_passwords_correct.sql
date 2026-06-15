-- V4: Reset mock user passwords with verified BCrypt hashes
-- BCrypt 12 rounds
-- Admin: Admin@123
-- Interviewer: Interviewer@123
-- Candidate: Candidate@123

UPDATE users SET password_hash = '$2a$12$1Zxc5L5f1/eGmLSNQriwrueJN0yfM65ajvcKPWNIlw6k4Pd694Gi2'
WHERE email = 'admin@interview.dev';

UPDATE users SET password_hash = '$2a$12$6rhF8U7EMQ5gIyHJAFZxmuiurt1mfKiIWqjjjD1EVPQtUYGVdp0Ci'
WHERE email = 'interviewer@interview.dev';

UPDATE users SET password_hash = '$2a$12$ismWCna8fjwM22twK7XuY..skUPa7LBFe9r9P.X5g5a5Fz6vDxefK'
WHERE email = 'candidate@interview.dev';

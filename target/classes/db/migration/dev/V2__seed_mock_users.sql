-- V2: Seed mock users for testing (all roles)
-- Passwords are BCrypt-encoded (raw: Admin@123, Interviewer@123, Candidate@123)

INSERT INTO users (id, email, password_hash, full_name, role, provider, is_active)
VALUES
  (
    '00000000-0000-0000-0000-000000000001',
    'admin@interview.dev',
    '$2a$12$gPqZvU3TM/dpwJQvfibvuuiHFQG.9PxaKoILbZhK8KLRm5/dQhFIa',
    'Platform Admin',
    'ADMIN',
    'LOCAL',
    TRUE
  ),
  (
    '00000000-0000-0000-0000-000000000002',
    'interviewer@interview.dev',
    '$2a$12$O3yvV/T1ZMlWVT8/v8V5iOCvTVnlLbdCMtBBP9SDYC4j9FqH9y.7K',
    'Alex Interviewer',
    'INTERVIEWER',
    'LOCAL',
    TRUE
  ),
  (
    '00000000-0000-0000-0000-000000000003',
    'candidate@interview.dev',
    '$2a$12$N93aFJ7K8T1fH2bPp.3Qh.f2PxVObLQRTEw0Q4WnFPGLQCXFxSHyS',
    'Sam Candidate',
    'CANDIDATE',
    'LOCAL',
    TRUE
  )
ON CONFLICT (email) DO NOTHING;

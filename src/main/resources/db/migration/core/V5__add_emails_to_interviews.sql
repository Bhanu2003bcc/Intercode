ALTER TABLE interviews ADD COLUMN candidate_email VARCHAR(255);
ALTER TABLE interviews ADD COLUMN interviewer_email VARCHAR(255);

UPDATE interviews i
SET candidate_email = u.email
FROM users u
WHERE i.candidate_id = u.id;

UPDATE interviews i
SET interviewer_email = u.email
FROM users u
WHERE i.interviewer_id = u.id;

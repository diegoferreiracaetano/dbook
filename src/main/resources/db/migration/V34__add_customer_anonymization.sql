ALTER TABLE app_user ADD COLUMN anonymized_at TIMESTAMP WITH TIME ZONE;

-- What is left of an e-mail after its owner was anonymized: only a SHA-256 hash, so the address cannot be read back
-- but can still be refused at registration (otherwise deleting a blocked account would clear its record).
CREATE TABLE anonymized_email (
    email_hash VARCHAR(64) PRIMARY KEY,
    anonymized_at TIMESTAMP WITH TIME ZONE NOT NULL
);

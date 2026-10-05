-- Every refresh token belongs to a family: the chain of tokens that came from one sign-in (each refresh spends a token
-- and issues the next one in the same family). A token that was already spent and shows up again is a stolen copy:
-- the whole family is revoked, so the thief and the real user both have to sign in again. Existing tokens are each
-- their own family.
ALTER TABLE refresh_token ADD COLUMN family_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);

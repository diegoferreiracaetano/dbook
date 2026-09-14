ALTER TABLE app_user ADD COLUMN name VARCHAR(255);

UPDATE app_user SET name = split_part(email, '@', 1) WHERE name IS NULL;

ALTER TABLE app_user ALTER COLUMN name SET NOT NULL;

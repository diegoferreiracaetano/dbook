-- the airline's logo, kept by the back office alongside the rest of the catalog; nullable so nothing existing breaks
ALTER TABLE airline ADD COLUMN logo_url VARCHAR(500);

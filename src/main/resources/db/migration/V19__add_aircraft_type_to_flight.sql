ALTER TABLE flight ADD COLUMN aircraft_type VARCHAR(50);

UPDATE flight SET aircraft_type = 'Airbus A320' WHERE aircraft_type IS NULL;

ALTER TABLE flight ALTER COLUMN aircraft_type SET NOT NULL;

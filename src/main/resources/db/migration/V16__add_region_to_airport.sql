ALTER TABLE airport ADD COLUMN region VARCHAR(50);
ALTER TABLE airport ADD COLUMN is_popular BOOLEAN;

UPDATE airport SET region = 'América do Sul' WHERE iata_code IN ('GRU', 'GIG', 'EZE');
UPDATE airport SET region = 'América do Norte' WHERE iata_code IN ('JFK', 'MIA');
UPDATE airport SET region = 'Europa' WHERE iata_code IN ('LHR', 'CDG', 'LIS');

UPDATE airport SET is_popular = TRUE WHERE iata_code IN ('GRU', 'GIG', 'JFK', 'LHR');
UPDATE airport SET is_popular = FALSE WHERE is_popular IS NULL;

ALTER TABLE airport ALTER COLUMN region SET NOT NULL;
ALTER TABLE airport ALTER COLUMN is_popular SET NOT NULL;

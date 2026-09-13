ALTER TABLE airport ADD COLUMN photo_url VARCHAR(500);

UPDATE airport SET photo_url = 'https://images.unsplash.com/photo-1645918899630-85e2f3132a84?w=600&h=400&fit=crop&auto=format' WHERE iata_code = 'GRU';
UPDATE airport SET photo_url = 'https://images.unsplash.com/photo-1518639192441-8fce0a366e2e?w=600&h=400&fit=crop&auto=format' WHERE iata_code = 'GIG';
UPDATE airport SET photo_url = 'https://images.unsplash.com/photo-1496588152823-86ff7695e68f?w=600&h=400&fit=crop&auto=format' WHERE iata_code = 'JFK';

ALTER TABLE airport ALTER COLUMN photo_url SET NOT NULL;
